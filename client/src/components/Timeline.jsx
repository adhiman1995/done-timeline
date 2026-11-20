import { useState } from 'react';
import { EditIcon, TrashIcon, StarIcon } from './icons.jsx';
import { categoryHue } from '../colors.js';

function dayLabel(date) {
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  if (date.toDateString() === today.toDateString()) return 'Today';
  if (date.toDateString() === yesterday.toDateString()) return 'Yesterday';
  return date.toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: date.getFullYear() === today.getFullYear() ? undefined : 'numeric',
  });
}

function groupByDay(tasks) {
  const groups = [];
  for (const task of tasks) {
    const date = new Date(task.completedAt);
    const key = date.toDateString();
    if (groups.at(-1)?.key !== key) groups.push({ key, date, items: [] });
    groups.at(-1).items.push(task);
  }
  return groups;
}

const timeOf = task =>
  new Date(task.completedAt).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });

function Chip({ category }) {
  if (!category) return null;
  return <span className="chip" style={{ '--hue': categoryHue(category) }}>{category}</span>;
}

function Actions({ task, onEdit, onDelete, onToggleStar }) {
  const [confirming, setConfirming] = useState(false);

  if (confirming) {
    return (
      <div className="actions confirming">
        <button className="btn-mini danger" onClick={() => onDelete(task)}>Delete</button>
        <button className="btn-mini" onClick={() => setConfirming(false)}>Cancel</button>
      </div>
    );
  }
  return (
    <div className="actions">
      <button
        className={`icon-btn star ${task.starred ? 'on' : ''}`}
        onClick={() => onToggleStar(task)}
        aria-label={task.starred ? 'Unstar task' : 'Star task'}
        aria-pressed={!!task.starred}
        title={task.starred ? 'Unstar' : 'Star'}
      >
        <StarIcon width={16} height={16} filled={task.starred} />
      </button>
      <button className="icon-btn hover-only" onClick={() => onEdit(task)} aria-label="Edit task" title="Edit">
        <EditIcon width={16} height={16} />
      </button>
      <button className="icon-btn hover-only" onClick={() => setConfirming(true)} aria-label="Delete task" title="Delete">
        <TrashIcon width={16} height={16} />
      </button>
    </div>
  );
}

function DayCount({ n }) {
  return <span className="day-count">{n} completed</span>;
}

/* ---------- Timeline view ---------- */
function TimelineView({ groups, handlers }) {
  return (
    <div className="tl">
      {groups.map(g => (
        <section className="tl-day" key={g.key}>
          <div className="tl-row tl-head">
            <span className="day-pill">{dayLabel(g.date)}</span>
            <span className="tl-node day-node" aria-hidden="true" />
            <DayCount n={g.items.length} />
          </div>
          <ol className="tl-entries">
            {g.items.map(t => (
              <li className="tl-row tl-entry" key={t.id} style={{ '--hue': categoryHue(t.category) }}>
                <time className="tl-time" dateTime={t.completedAt}>{timeOf(t)}</time>
                <span className="tl-node" aria-hidden="true" />
                <article className="card">
                  <div className="card-main">
                    <h3 className="card-title">{t.title}</h3>
                    {t.notes && <p className="card-notes">{t.notes}</p>}
                    {/* Time lives in the meta row on narrow screens, where the time column is hidden. */}
                    <div className={`card-meta ${t.category ? '' : 'mobile-only'}`}>
                      <time className="card-time" dateTime={t.completedAt}>{timeOf(t)}</time>
                      <Chip category={t.category} />
                    </div>
                  </div>
                  <Actions task={t} {...handlers} />
                </article>
              </li>
            ))}
          </ol>
        </section>
      ))}
    </div>
  );
}

/* ---------- List view ---------- */
function ListView({ groups, handlers }) {
  return (
    <div className="lv">
      {groups.map(g => (
        <section className="lv-day" key={g.key}>
          <div className="lv-head">
            <span className="day-pill">{dayLabel(g.date)}</span>
            <DayCount n={g.items.length} />
          </div>
          <ul className="lv-items">
            {g.items.map(t => (
              <li key={t.id} className="card lv-item" style={{ '--hue': categoryHue(t.category) }}>
                <div className="card-main">
                  <div className="lv-title-row">
                    <h3 className="card-title">{t.title}</h3>
                    <time className="lv-time" dateTime={t.completedAt}>{timeOf(t)}</time>
                  </div>
                  {t.notes && <p className="card-notes">{t.notes}</p>}
                  {t.category && <div className="card-meta"><Chip category={t.category} /></div>}
                </div>
                <Actions task={t} {...handlers} />
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

/* ---------- Compact view ---------- */
function CompactView({ groups, handlers }) {
  return (
    <div className="cv">
      {groups.map(g => (
        <section className="cv-day" key={g.key}>
          <div className="cv-head">
            <span className="cv-label">{dayLabel(g.date)}</span>
            <DayCount n={g.items.length} />
          </div>
          <ul className="cv-box">
            {g.items.map(t => (
              <li key={t.id} className="cv-row" style={{ '--hue': categoryHue(t.category) }}>
                <time className="cv-time" dateTime={t.completedAt}>{timeOf(t)}</time>
                <span className="cv-dot" aria-hidden="true" />
                <span className="cv-title" title={t.notes || t.title}>{t.title}</span>
                <Chip category={t.category} />
                <Actions task={t} {...handlers} />
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

export default function TaskViews({ view, tasks, onEdit, onDelete, onToggleStar }) {
  const groups = groupByDay(tasks);
  const handlers = { onEdit, onDelete, onToggleStar };
  if (view === 'list') return <ListView groups={groups} handlers={handlers} />;
  if (view === 'compact') return <CompactView groups={groups} handlers={handlers} />;
  return <TimelineView groups={groups} handlers={handlers} />;
}
