import { Logo, PlusIcon, SunIcon, MoonIcon, TimelineIcon } from './icons.jsx';

/** Slim icon rail on desktop; becomes the top bar on phones. */
export default function Rail({ user, theme, onToggleTheme, onAdd, onAccount }) {
  const nextTheme = theme === 'dark' ? 'light' : 'dark';
  return (
    <nav className="rail" aria-label="App">
      <div className="rail-brand">
        <Logo size={36} />
        <span className="rail-name">Ticked</span>
      </div>

      <div className="rail-group">
        <button className="rail-btn active" aria-label="Timeline" title="Timeline" aria-current="page">
          <TimelineIcon />
        </button>
        <button className="rail-btn" onClick={onAdd} aria-label="Add completed task" title="Add completed task">
          <PlusIcon />
        </button>
      </div>

      <div className="rail-group rail-bottom">
        <button
          className="rail-btn"
          onClick={onToggleTheme}
          aria-label={`Switch to ${nextTheme} theme`}
          title={`Switch to ${nextTheme} theme`}
        >
          {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
        </button>
        <button className="avatar" onClick={onAccount} aria-label="Account" title={user.email || 'Account'}>
          {user.photoURL
            ? <img src={user.photoURL} alt="" referrerPolicy="no-referrer" />
            : <span>{(user.displayName || user.email || '?')[0].toUpperCase()}</span>}
        </button>
      </div>
    </nav>
  );
}
