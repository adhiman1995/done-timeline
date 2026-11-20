import pg from 'pg';

if (!process.env.DATABASE_URL) {
  console.error('DATABASE_URL is not set. See server/.env.example.');
  process.exit(1);
}

export const pool = new pg.Pool({ connectionString: process.env.DATABASE_URL, max: 5 });

// Every write takes a fresh value from task_rev_seq. Clients pull "everything with
// rev > my cursor", which gives a simple, reliable change feed for offline sync.
// Deletes are soft (deleted = true) so they propagate to other devices.
export async function migrate() {
  await pool.query(`
    CREATE SEQUENCE IF NOT EXISTS task_rev_seq;
    CREATE TABLE IF NOT EXISTS tasks (
      id           uuid PRIMARY KEY,
      title        text        NOT NULL,
      notes        text        NOT NULL DEFAULT '',
      category     text        NOT NULL DEFAULT '',
      starred      boolean     NOT NULL DEFAULT false,
      completed_at timestamptz NOT NULL,
      created_at   timestamptz NOT NULL DEFAULT now(),
      updated_at   timestamptz NOT NULL DEFAULT now(),
      deleted      boolean     NOT NULL DEFAULT false,
      rev          bigint      NOT NULL DEFAULT nextval('task_rev_seq')
    );
    CREATE INDEX IF NOT EXISTS tasks_rev_idx ON tasks (rev);
  `);
}

export function toDto(row) {
  return {
    id: row.id,
    title: row.title,
    notes: row.notes,
    category: row.category,
    starred: row.starred,
    completedAt: row.completed_at.toISOString(),
    createdAt: row.created_at.toISOString(),
    updatedAt: row.updated_at.toISOString(),
    deleted: row.deleted,
  };
}
