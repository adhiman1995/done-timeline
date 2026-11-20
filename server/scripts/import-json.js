// Imports tasks from the old JSON file store (server/data/tasks.json) into Postgres.
// Safe to run more than once: existing ids are skipped.
import { readFile } from 'node:fs/promises';
import { pool, migrate } from '../db.js';

const file = process.argv[2] || new URL('../data/tasks.json', import.meta.url);
const tasks = JSON.parse(await readFile(file, 'utf8'));

await migrate();
let added = 0;
for (const t of tasks) {
  const { rowCount } = await pool.query(
    `INSERT INTO tasks (id, title, notes, category, starred, completed_at, created_at, updated_at)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
     ON CONFLICT (id) DO NOTHING`,
    [t.id, t.title, t.notes ?? '', t.category ?? '', t.starred === true, t.completedAt,
     t.createdAt ?? t.completedAt, t.updatedAt ?? t.createdAt ?? t.completedAt]
  );
  added += rowCount;
}
console.log(`Imported ${added} of ${tasks.length} tasks.`);
await pool.end();
