import { useMemo } from 'react';
import { FlameIcon } from './icons.jsx';

function startOfDay(d) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

// Consecutive days (ending today, or yesterday if nothing yet today) with at least one task.
function streak(tasks) {
  const days = new Set(tasks.map(t => startOfDay(t.completedAt).getTime()));
  const cursor = startOfDay(new Date());
  if (!days.has(cursor.getTime())) cursor.setDate(cursor.getDate() - 1);
  let n = 0;
  while (days.has(cursor.getTime())) {
    n++;
    cursor.setDate(cursor.getDate() - 1);
  }
  return n;
}

export default function Stats({ tasks }) {
  const s = useMemo(() => {
    const today = startOfDay(new Date());
    const weekAgo = new Date(today);
    weekAgo.setDate(today.getDate() - 6);
    return {
      today: tasks.filter(t => new Date(t.completedAt) >= today).length,
      week: tasks.filter(t => new Date(t.completedAt) >= weekAgo).length,
      total: tasks.length,
      streak: streak(tasks),
    };
  }, [tasks]);

  return (
    <section className="stats">
      <div className="stat">
        <span className="stat-label">Today</span>
        <span className="stat-value">{s.today}</span>
      </div>
      <div className="stat">
        <span className="stat-label">Last 7 days</span>
        <span className="stat-value">{s.week}</span>
      </div>
      <div className="stat">
        <span className="stat-label">All time</span>
        <span className="stat-value">{s.total}</span>
      </div>
      <div className="stat">
        <span className="stat-label">Streak</span>
        <span className="stat-value">
          {s.streak}
          <small>{s.streak === 1 ? 'day' : 'days'}</small>
          {s.streak > 0 && <FlameIcon className="flame" />}
        </span>
      </div>
    </section>
  );
}
