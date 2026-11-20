import { useState } from 'react';
import { EditIcon, TrashIcon, StarIcon } from './icons.jsx';
import { CategoryIcon } from './Insights.jsx';
import { categoryHue } from '../colors.js';
import { dayLabel, dayTone, timeOf } from '../dates.js';

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

export function DayBadge({ date }) {
  return <span className={`badge tone-${dayTone(date)}`}><i />{dayLabel(date)}</span>;
}

function Chip({ category }) {
  if (!category) return null;
  return <span className="chip" style={{ '--hue': categoryHue(category) }}>{category}</span>;
}

function Actions({ task, onEdit, onDelete, onToggleStar }) {
  const [confirming, setConfirming] = useState(false);

  if (confirming) {
    return (
      <div className="actions confirming" onClick={e => e.stopPropagation()}>
        <button className="btn-mini danger" onClick={() => onDelete(task)}>Delete</button>
        <button className="btn-mini" onClick={() => setConfirming(false)}>Cancel</button>
      </div>
    );
  }
  return (
    <div className="actions" onClick={e => e.stopPropagation()}>
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

const count = n => `${n} completed`;

/* ---------- Timeline view: time · rail · card, grouped by day ---------- */
function TimelineView({ groups, handlers }) {
  return (
    <div className="tl">
      {groups.map(g => (
        <section className="tl-day" key={g.key}>
          <div className="tl-head">
            <DayBadge date={g.date} />
            <span className="day-count">{count(g.items.length)}</span>
          </div>
          <ol className="tl-entries">
            {g.items.map(t => (
              <li className="tl-entry" key={t.id} style={{ '--hue': categoryHue(t.category) }}>
                <time className="tl-time" dateTime={t.completedAt}>{timeOf(t.completedAt)}</time>
                <span className="tl-node" aria-hidden="true" />
                <article className="row-card" onClick={() => handlers.onEdit(t)}>
                  <CategoryIcon category={t.category} />
                  <div className="row-main">
                    <h3 className="row-title">{t.title}</h3>
                    {t.notes && <p className="row-notes">{t.notes}</p>}
                    <div className={`row-meta ${t.category ? '' : 'narrow-only'}`}>
                      <time className="narrow-only" dateTime={t.completedAt}>{timeOf(t.completedAt)}</time>
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

/* ---------- List view: one row per task with its day badge, like a queue ---------- */
function ListView({ tasks, handlers }) {
  return (
    <ul className="lv">
      {tasks.map(t => {
        const date = new Date(t.completedAt);
        return (
          <li key={t.id} className="row-card lv-row" style={{ '--hue': categoryHue(t.category) }} onClick={() => handlers.onEdit(t)}>
            <div className="lv-badge"><DayBadge date={date} /></div>
            <CategoryIcon category={t.category} />
            <div className="row-main">
              <h3 className="row-title">{t.title}</h3>
              {t.notes && <p className="row-notes">{t.notes}</p>}
            </div>
            <div className="lv-when">
              <time dateTime={t.completedAt}>{timeOf(t.completedAt)}</time>
              <span>{t.category || 'No category'}</span>
            </div>
            <Actions task={t} {...handlers} />
          </li>
        );
      })}
    </ul>
  );
}

/* ---------- Compact view: dense rows inside one card per day ---------- */
function CompactView({ groups, handlers }) {
  return (
    <div className="cv">
      {groups.map(g => (
        <section className="cv-day" key={g.key}>
          <div className="tl-head">
            <DayBadge date={g.date} />
            <span className="day-count">{count(g.items.length)}</span>
          </div>
          <ul className="cv-box">
            {g.items.map(t => (
              <li key={t.id} className="cv-row" style={{ '--hue': categoryHue(t.category) }} onClick={() => handlers.onEdit(t)}>
                <time className="cv-time" dateTime={t.completedAt}>{timeOf(t.completedAt)}</time>
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
  const handlers = { onEdit, onDelete, onToggleStar };
  if (view === 'list') return <ListView tasks={tasks} handlers={handlers} />;
  const groups = groupByDay(tasks);
  if (view === 'compact') return <CompactView groups={groups} handlers={handlers} />;
  return <TimelineView groups={groups} handlers={handlers} />;
}
