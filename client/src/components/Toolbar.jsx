import { categoryHue } from '../colors.js';
import { RANGES } from '../dates.js';
import { ChevronDownIcon, FilterIcon, StarIcon } from './icons.jsx';
import { STARRED, VIEWS } from './filters.js';

/** Range tabs with counts, the view picker, and (on small screens) category chips. */
export default function Toolbar({ range, onRange, rangeCounts, view, onView, categories, filter, onFilter }) {
  const chip = (key, label, style) => (
    <button
      key={key}
      className={`fchip ${filter === key ? 'active' : ''}`}
      aria-pressed={filter === key}
      onClick={() => onFilter(filter === key ? '' : key)}
      style={style}
    >
      {label}
    </button>
  );

  return (
    <div className="toolbar">
      <div className="toolbar-row">
        <div className="tabs" role="tablist" aria-label="Time range">
          {RANGES.map(([key, label]) => (
            <button
              key={key}
              role="tab"
              aria-selected={range === key}
              className={`tab ${range === key ? 'active' : ''}`}
              onClick={() => onRange(key)}
            >
              {label}
              <span className="tab-count">{rangeCounts[key]}</span>
            </button>
          ))}
        </div>

        <label className="select-pill">
          <span>View</span>
          <select value={view} onChange={e => onView(e.target.value)} aria-label="View">
            {VIEWS.map(([key, label]) => <option key={key} value={key}>{label}</option>)}
          </select>
          <ChevronDownIcon width={15} height={15} />
        </label>
      </div>

      <div className="toolbar-row chips-row">
        <FilterIcon width={17} height={17} className="chips-icon" />
        {chip('', 'All')}
        {categories.map(c => chip(c, <><span className="dot" />{c}</>, { '--hue': categoryHue(c) }))}
        {chip(STARRED, <><StarIcon width={14} height={14} className="star-glyph" />Starred</>)}
      </div>
    </div>
  );
}
