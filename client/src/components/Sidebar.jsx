import { categoryHue } from '../colors.js';
import { LayersIcon, StarIcon, FlameIcon, PlusIcon } from './icons.jsx';
import { STARRED } from './filters.js';

/** Left navigation: all / starred, categories with counts, and the streak card. */
export default function Sidebar({ tasks, categories, filter, onFilter, streak, onAdd }) {
  const counts = {};
  for (const t of tasks) if (t.category) counts[t.category] = (counts[t.category] || 0) + 1;
  const starred = tasks.filter(t => t.starred).length;

  const item = (key, icon, label, count, style) => (
    <button
      key={key}
      className={`nav-item ${filter === key ? 'active' : ''}`}
      aria-pressed={filter === key}
      onClick={() => onFilter(key)}
      style={style}
    >
      <span className="nav-icon">{icon}</span>
      <span className="nav-label">{label}</span>
      <span className="nav-count">{count}</span>
    </button>
  );

  return (
    <aside className="sidebar">
      <div className="sidebar-head">
        <div className="sidebar-title">Ticked</div>
        <div className="sidebar-sub">Your completed work</div>
      </div>

      <div className="nav">
        {item('', <LayersIcon width={17} height={17} />, 'All tasks', tasks.length)}
        {item(STARRED, <StarIcon width={17} height={17} />, 'Starred', starred)}
      </div>

      {categories.length > 0 && (
        <>
          <div className="sidebar-section">Categories</div>
          <div className="nav">
            {categories.map(c =>
              item(c, <span className="nav-dot" />, c, counts[c] || 0, { '--hue': categoryHue(c) })
            )}
          </div>
        </>
      )}

      <div className="sidebar-card">
        <div className="sidebar-card-title">
          <FlameIcon width={16} height={16} className={streak ? 'flame' : ''} />
          {streak ? `${streak}-day streak` : 'Start a streak'}
        </div>
        <p>
          {streak
            ? `You've logged work ${streak} day${streak === 1 ? '' : 's'} in a row. Keep it going.`
            : 'Log something you finished today to start a new streak.'}
        </p>
        <button className="btn soft small" onClick={onAdd}>
          <PlusIcon width={15} height={15} /> Log a task
        </button>
      </div>
    </aside>
  );
}
