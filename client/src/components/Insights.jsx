import { useMemo } from 'react';
import { categoryHue } from '../colors.js';
import { daysAgo, dayLabel, relative, startOfDay, streak } from '../dates.js';
import { ArrowUpRightIcon, CheckIcon, DownloadIcon, FlameIcon } from './icons.jsx';

export function CategoryIcon({ category, size = 38 }) {
  return (
    <span className="cat-icon" style={{ '--hue': categoryHue(category), width: size, height: size }}>
      {category ? category.trim()[0].toUpperCase() : <CheckIcon width={size * 0.45} height={size * 0.45} strokeWidth={2.6} />}
    </span>
  );
}

/** Right-hand summary cards: this week, top categories and all-time numbers. */
export default function Insights({ tasks, onOpenTask, onFilter, onExport }) {
  const data = useMemo(() => {
    const byDay = new Map();
    for (const t of tasks) {
      const key = startOfDay(t.completedAt).getTime();
      byDay.set(key, (byDay.get(key) || 0) + 1);
    }
    const week = Array.from({ length: 7 }, (_, i) => {
      const date = daysAgo(6 - i);
      return { date, count: byDay.get(date.getTime()) || 0, today: i === 6 };
    });

    const cats = new Map();
    for (const t of tasks) {
      if (!t.category) continue;
      const c = cats.get(t.category) || { name: t.category, count: 0, last: t.completedAt };
      c.count++;
      if (t.completedAt > c.last) c.last = t.completedAt;
      cats.set(t.category, c);
    }

    const latest = tasks.reduce((a, t) => (!a || t.completedAt > a.completedAt ? t : a), null);

    return {
      week,
      weekTotal: week.reduce((n, d) => n + d.count, 0),
      activeDays: week.filter(d => d.count).length,
      categories: [...cats.values()].sort((a, b) => b.count - a.count).slice(0, 4),
      latest,
      total: tasks.length,
      streak: streak(tasks),
      bestDay: Math.max(0, ...byDay.values()),
      starred: tasks.filter(t => t.starred).length,
    };
  }, [tasks]);

  return (
    <>
      <section className="icard">
        <div className="icard-head">
          <h2>This week</h2>
          <span className="muted">{data.weekTotal} task{data.weekTotal === 1 ? '' : 's'}</span>
        </div>
        <p className="icard-sub">{data.activeDays} of 7 days active</p>
        <div className="week">
          {data.week.map(d => (
            <div className="week-day" key={d.date.getTime()}>
              <span
                className={`week-dot ${d.count ? 'on' : ''} ${d.today ? 'today' : ''}`}
                title={`${dayLabel(d.date)}: ${d.count} task${d.count === 1 ? '' : 's'}`}
              >
                {d.count || d.date.getDate()}
              </span>
              <span className="week-label">{d.date.toLocaleDateString(undefined, { weekday: 'narrow' })}</span>
            </div>
          ))}
        </div>
        {data.latest && (
          <button className="latest" onClick={() => onOpenTask(data.latest)}>
            <CategoryIcon category={data.latest.category} size={34} />
            <span className="latest-text">
              <span className="latest-title">Latest: {data.latest.title}</span>
              <span className="latest-sub">
                {relative(data.latest.completedAt)}{data.latest.category ? ` · ${data.latest.category}` : ''}
              </span>
            </span>
            <span className="round-btn"><ArrowUpRightIcon width={15} height={15} /></span>
          </button>
        )}
      </section>

      {data.categories.length > 0 && (
        <section className="icard">
          <div className="icard-head"><h2>Top categories</h2></div>
          <ul className="cat-list">
            {data.categories.map(c => (
              <li key={c.name}>
                <button onClick={() => onFilter(c.name)}>
                  <CategoryIcon category={c.name} size={34} />
                  <span className="cat-text">
                    <span className="cat-name">{c.name}</span>
                    <span className="cat-sub">Latest {relative(c.last)}</span>
                  </span>
                  <span className="count-chip">{c.count} {c.count === 1 ? 'task' : 'tasks'}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="icard">
        <div className="icard-head"><h2>All time</h2></div>
        <div className="big-stat">
          <span className="big-num">{data.total}</span>
          <span className="muted">tasks completed</span>
        </div>
        <dl className="stat-rows">
          <div>
            <dt>Current streak</dt>
            <dd>{data.streak} {data.streak === 1 ? 'day' : 'days'}{data.streak > 0 && <FlameIcon width={15} height={15} className="flame" />}</dd>
          </div>
          <div><dt>Best day</dt><dd>{data.bestDay}</dd></div>
          <div><dt>Starred</dt><dd>{data.starred}</dd></div>
        </dl>
        <button className="btn soft small" onClick={onExport}>
          <DownloadIcon width={15} height={15} /> Back up your tasks
        </button>
      </section>
    </>
  );
}
