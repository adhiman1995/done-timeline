import { CheckIcon, SearchIcon, SunIcon, MoonIcon } from './icons.jsx';

const VIEWS = [
  ['timeline', 'Timeline'],
  ['list', 'List'],
  ['compact', 'Compact'],
];

export default function Header({
  search, onSearch, category, onCategory, categories, view, onView, theme, onToggleTheme, user, onOpenAccount,
}) {
  const today = new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });
  const nextTheme = theme === 'dark' ? 'light' : 'dark';

  return (
    <header className="topbar">
      <div className="container topbar-inner">
        <div className="brand">
          <div className="logo"><CheckIcon width={20} height={20} strokeWidth={2.5} /></div>
          <div>
            <div className="brand-name">Done Timeline</div>
            <div className="brand-sub">{today}</div>
          </div>
        </div>

        <div className="controls">
          <label className="search control">
            <SearchIcon width={16} height={16} />
            <input
              type="search"
              placeholder="Search tasks"
              value={search}
              onChange={e => onSearch(e.target.value)}
              aria-label="Search tasks"
            />
          </label>

          <select
            className="control select"
            value={category}
            onChange={e => onCategory(e.target.value)}
            aria-label="Filter by category"
          >
            <option value="">All categories</option>
            {categories.map(c => <option key={c}>{c}</option>)}
          </select>

          <div className="segmented control" role="tablist" aria-label="View">
            {VIEWS.map(([key, label]) => (
              <button
                key={key}
                role="tab"
                aria-selected={view === key}
                className={view === key ? 'active' : ''}
                onClick={() => onView(key)}
              >
                {label}
              </button>
            ))}
          </div>

          <button
            className="control icon-control"
            onClick={onToggleTheme}
            aria-label={`Switch to ${nextTheme} theme`}
            title={`Switch to ${nextTheme} theme`}
          >
            {theme === 'dark' ? <SunIcon width={16} height={16} /> : <MoonIcon width={16} height={16} />}
          </button>

          <button
            className="control icon-control avatar-btn"
            onClick={onOpenAccount}
            aria-label="Account"
            title={user?.email || 'Account'}
          >
            {user?.photoURL
              ? <img src={user.photoURL} alt="" referrerPolicy="no-referrer" />
              : <span>{(user?.displayName || user?.email || '?')[0].toUpperCase()}</span>}
          </button>
        </div>
      </div>
    </header>
  );
}
