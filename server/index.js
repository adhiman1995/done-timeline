import express from 'express';
import { randomUUID, timingSafeEqual, createHash } from 'node:crypto';
import { pool, migrate, toDto } from './db.js';

const PORT = process.env.PORT || 3001;
const API_TOKEN = process.env.API_TOKEN;
const ALLOWED_ORIGINS = (process.env.ALLOWED_ORIGINS || '*').split(',').map(s => s.trim()).filter(Boolean);

if (!API_TOKEN || API_TOKEN.length < 16) {
  console.error('API_TOKEN must be set and at least 16 characters. See server/.env.example.');
  process.exit(1);
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// --- validation ------------------------------------------------------------
class HttpError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

function parseDate(value, field) {
  const d = new Date(value);
  if (value == null || Number.isNaN(d.getTime())) throw new HttpError(400, `${field} must be a valid date`);
  return d;
}

/** Validate a task body. `partial` allows omitting fields (for PUT). */
function validateTask(body, { partial = false } = {}) {
  if (!body || typeof body !== 'object') throw new HttpError(400, 'body must be an object');
  const out = {};
  if (!partial || body.title !== undefined) {
    if (typeof body.title !== 'string' || !body.title.trim()) throw new HttpError(400, 'title is required');
    out.title = body.title.trim().slice(0, 200);
  }
  if (!partial || body.completedAt !== undefined) out.completedAt = parseDate(body.completedAt, 'completedAt');
  if (body.notes !== undefined) out.notes = String(body.notes ?? '').trim().slice(0, 2000);
  if (body.category !== undefined) out.category = String(body.category ?? '').trim().slice(0, 40);
  if (body.starred !== undefined) out.starred = body.starred === true;
  return out;
}

// --- app -------------------------------------------------------------------
const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '2mb' }));

// CORS: the web app is served from GitHub Pages, a different origin.
app.use((req, res, next) => {
  const origin = req.headers.origin;
  if (origin && (ALLOWED_ORIGINS.includes('*') || ALLOWED_ORIGINS.includes(origin))) {
    res.setHeader('Access-Control-Allow-Origin', origin);
    res.setHeader('Vary', 'Origin');
    res.setHeader('Access-Control-Allow-Headers', 'Authorization, Content-Type');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Max-Age', '600');
  }
  if (req.method === 'OPTIONS') return res.status(204).end();
  next();
});

app.get('/api/health', (_req, res) => res.json({ ok: true }));

// Bearer-token auth for everything else.
const tokenHash = createHash('sha256').update(API_TOKEN).digest();
app.use('/api', (req, res, next) => {
  const header = req.headers.authorization || '';
  const given = header.startsWith('Bearer ') ? header.slice(7) : '';
  const ok = timingSafeEqual(createHash('sha256').update(given).digest(), tokenHash);
  if (!ok) return res.status(401).json({ error: 'invalid or missing access token' });
  next();
});

const wrap = fn => (req, res, next) => fn(req, res).catch(next);

app.get('/api/auth/check', (_req, res) => res.json({ ok: true }));

// --- REST (used by the web app) ---------------------------------------------
app.get('/api/tasks', wrap(async (_req, res) => {
  const { rows } = await pool.query(
    'SELECT * FROM tasks WHERE NOT deleted ORDER BY completed_at DESC'
  );
  res.json(rows.map(toDto));
}));

app.post('/api/tasks', wrap(async (req, res) => {
  const t = validateTask(req.body);
  const id = req.body.id ?? randomUUID();
  if (!UUID_RE.test(id)) throw new HttpError(400, 'id must be a UUID');
  const { rows } = await pool.query(
    `INSERT INTO tasks (id, title, notes, category, starred, completed_at)
     VALUES ($1, $2, $3, $4, $5, $6)
     ON CONFLICT (id) DO NOTHING
     RETURNING *`,
    [id, t.title, t.notes ?? '', t.category ?? '', t.starred ?? false, t.completedAt]
  );
  if (!rows.length) throw new HttpError(409, 'a task with this id already exists');
  res.status(201).json(toDto(rows[0]));
}));

app.put('/api/tasks/:id', wrap(async (req, res) => {
  if (!UUID_RE.test(req.params.id)) throw new HttpError(404, 'not found');
  const t = validateTask(req.body, { partial: true });
  const cols = { title: t.title, notes: t.notes, category: t.category, starred: t.starred, completed_at: t.completedAt };
  const sets = [];
  const values = [];
  for (const [col, val] of Object.entries(cols)) {
    if (val !== undefined) { values.push(val); sets.push(`${col} = $${values.length}`); }
  }
  values.push(req.params.id);
  const { rows } = await pool.query(
    `UPDATE tasks SET ${[...sets, "updated_at = now()", "rev = nextval('task_rev_seq')"].join(', ')}
     WHERE id = $${values.length} AND NOT deleted
     RETURNING *`,
    values
  );
  if (!rows.length) throw new HttpError(404, 'not found');
  res.json(toDto(rows[0]));
}));

app.delete('/api/tasks/:id', wrap(async (req, res) => {
  if (!UUID_RE.test(req.params.id)) throw new HttpError(404, 'not found');
  const { rowCount } = await pool.query(
    `UPDATE tasks SET deleted = true, updated_at = now(), rev = nextval('task_rev_seq')
     WHERE id = $1 AND NOT deleted`,
    [req.params.id]
  );
  if (!rowCount) throw new HttpError(404, 'not found');
  res.status(204).end();
}));

// --- Sync (used by the Android app) -----------------------------------------
// Request:  { since: <cursor>, changes: [TaskDto with updatedAt + deleted] }
// Response: { cursor: <new cursor>, changes: [every task with rev > since] }
// Conflicts resolve last-write-wins on updatedAt. Client timestamps are capped
// at the server's clock so a phone with a fast clock can't win forever.
app.post('/api/sync', wrap(async (req, res) => {
  const since = Number(req.body?.since ?? 0);
  const changes = req.body?.changes ?? [];
  if (!Number.isSafeInteger(since) || since < 0) throw new HttpError(400, 'since must be a non-negative integer');
  if (!Array.isArray(changes) || changes.length > 5000) throw new HttpError(400, 'changes must be an array (max 5000)');

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    for (const c of changes) {
      if (!c || !UUID_RE.test(c.id)) throw new HttpError(400, 'each change needs a UUID id');
      const deleted = c.deleted === true;
      const t = deleted
        ? { title: String(c.title ?? '').slice(0, 200), completedAt: parseDate(c.completedAt ?? 0, 'completedAt') }
        : validateTask(c);
      const updatedAt = parseDate(c.updatedAt, 'updatedAt');
      const createdAt = c.createdAt ? parseDate(c.createdAt, 'createdAt') : updatedAt;
      await client.query(
        `INSERT INTO tasks (id, title, notes, category, starred, completed_at, created_at, updated_at, deleted)
         VALUES ($1, $2, $3, $4, $5, $6, LEAST($7::timestamptz, now()), LEAST($8::timestamptz, now()), $9)
         ON CONFLICT (id) DO UPDATE SET
           title = EXCLUDED.title, notes = EXCLUDED.notes, category = EXCLUDED.category,
           starred = EXCLUDED.starred, completed_at = EXCLUDED.completed_at,
           updated_at = EXCLUDED.updated_at, deleted = EXCLUDED.deleted,
           rev = nextval('task_rev_seq')
         WHERE tasks.updated_at < EXCLUDED.updated_at`,
        [c.id, t.title, t.notes ?? '', t.category ?? '', t.starred ?? false, t.completedAt, createdAt, updatedAt, deleted]
      );
    }
    const { rows } = await client.query('SELECT * FROM tasks WHERE rev > $1 ORDER BY rev', [since]);
    await client.query('COMMIT');
    const cursor = rows.length ? Number(rows.at(-1).rev) : since;
    res.json({ cursor, changes: rows.map(toDto) });
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    throw e;
  } finally {
    client.release();
  }
}));

app.use('/api', (_req, res) => res.status(404).json({ error: 'not found' }));

app.use((err, _req, res, _next) => {
  if (err instanceof HttpError) return res.status(err.status).json({ error: err.message });
  if (err.type === 'entity.parse.failed') return res.status(400).json({ error: 'invalid JSON' });
  console.error(err);
  res.status(500).json({ error: 'internal server error' });
});

await migrate();
app.listen(PORT, () => console.log(`API listening on http://localhost:${PORT}`));
