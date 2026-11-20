import { StarIcon } from './icons.jsx';
import { categoryHue } from '../colors.js';

export const STARRED = '\u0000starred';

const RANGES = [
  ['today', 'Today'],
  ['7d', '7 Days'],
  ['30d', '30 Days'],
  ['all', 'All'],
];

export default function FilterBar({ tasks, categories, filter, onFilter, range, onRange }) {
  const counts = {};
  for (const t of tasks) if (t.category) counts[t.category] = (counts[t.category] || 0) + 1;
  const starredCount = tasks.filter(t => t.starred).length;

  const pill = (key, label, count, extra = {}) => (
    <button
      key={key}
      className={`filter-pill ${filter === key ? 'active' : ''}`}
      aria-pressed={filter === key}
      onClick={() => onFilter(filter === key && key !== '' ? '' : key)}
      {...extra}
    >
      {label}
      <span className="count">{count}</span>
    </button>
  );

  return (
    <div className="filter-bar">
      <div className="filter-pills">
        {pill('', 'All', tasks.length)}
        {categories.map(c =>
          pill(c, <><span className="hue-dot" />{c}</>, counts[c] || 0, { style: { '--hue': categoryHue(c) } })
        )}
        {pill(STARRED, <><StarIcon width={13} height={13} className="star-glyph" />Starred</>, starredCount)}
      </div>

      <div className="segmented small" role="tablist" aria-label="Time range">
        {RANGES.map(([key, label]) => (
          <button
            key={key}
            role="tab"
            aria-selected={range === key}
            className={range === key ? 'active' : ''}
            onClick={() => onRange(key)}
          >
            {label}
          </button>
        ))}
      </div>
    </div>
  );
}
