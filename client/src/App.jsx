import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, AuthError, getConnection, clearConnection } from './api.js';
import ConnectForm from './components/ConnectForm.jsx';
import Header from './components/Header.jsx';
import Stats from './components/Stats.jsx';
import FilterBar, { STARRED } from './components/FilterBar.jsx';
import TaskViews from './components/Timeline.jsx';
import TaskModal from './components/TaskModal.jsx';
import { PlusIcon } from './components/icons.jsx';
import { useTheme } from './useTheme.js';

const RANGE_DAYS = { today: 1, '7d': 7, '30d': 30, all: Infinity };

function rangeStart(range) {
  const days = RANGE_DAYS[range];
  if (days === Infinity) return null;
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() - (days - 1));
  return d;
}

function usePersisted(key, initial) {
  const [value, setValue] = useState(() => {
    try { return localStorage.getItem(key) ?? initial; } catch { return initial; }
  });
  useEffect(() => { try { localStorage.setItem(key, value); } catch {} }, [key, value]);
  return [value, setValue];
}

export default function App() {
  const [theme, toggleTheme] = useTheme();
  const [view, setView] = usePersisted('view', 'timeline');
  const [range, setRange] = usePersisted('range', '7d');
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null); // null | { task?: Task }
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState(''); // '' | category name | STARRED
  const [connected, setConnected] = useState(() => !!getConnection().token);
  const [authReason, setAuthReason] = useState('');
  const [connectOpen, setConnectOpen] = useState(false);
  const [slow, setSlow] = useState(false);

  const handleError = useCallback(e => {
    if (e instanceof AuthError) {
      setAuthReason('Your access token was rejected. Please enter it again.');
      setConnected(false);
    } else {
      setError(e.message);
    }
  }, []);

  const load = useCallback(async ({ quiet = false } = {}) => {
    if (!quiet) setLoading(true);
    const slowTimer = setTimeout(() => setSlow(true), 3000);
    try {
      setTasks(await api.list());
      setError('');
    } catch (e) {
      handleError(e);
    } finally {
      clearTimeout(slowTimer);
      setSlow(false);
      setLoading(false);
    }
  }, [handleError]);

  useEffect(() => {
    if (!connected) return;
    load();
    // Pick up changes made on other devices (e.g. the Android app) when returning to the tab.
    const onVisible = () => document.visibilityState === 'visible' && load({ quiet: true });
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  }, [connected, load]);

  const categories = useMemo(
    () => [...new Set(tasks.map(t => t.category).filter(Boolean))].sort(),
    [tasks]
  );

  // Range + search are applied first; chip counts are computed from this set.
  const inScope = useMemo(() => {
    const start = rangeStart(range);
    const q = search.trim().toLowerCase();
    return tasks
      .filter(t => !start || new Date(t.completedAt) >= start)
      .filter(t => !q || `${t.title} ${t.notes} ${t.category}`.toLowerCase().includes(q))
      .sort((a, b) => b.completedAt.localeCompare(a.completedAt));
  }, [tasks, range, search]);

  const visible = useMemo(() => {
    if (!filter) return inScope;
    if (filter === STARRED) return inScope.filter(t => t.starred);
    return inScope.filter(t => t.category === filter);
  }, [inScope, filter]);

  async function run(fn) {
    setError('');
    try { await fn(); } catch (e) { handleError(e); }
  }

  async function handleSave(data) {
    try {
      if (modal?.task) {
        const updated = await api.update(modal.task.id, data);
        setTasks(ts => ts.map(t => (t.id === updated.id ? updated : t)));
      } else {
        const created = await api.create(data);
        setTasks(ts => [...ts, created]);
      }
      setModal(null);
    } catch (e) {
      if (e instanceof AuthError) { setModal(null); handleError(e); return; }
      return e.message;
    }
  }

  if (!connected) {
    return (
      <>
        <div className="bg-glow" aria-hidden="true" />
        <ConnectForm
          reason={authReason}
          onConnected={() => { setAuthReason(''); setConnected(true); }}
        />
      </>
    );
  }

  const handleDelete = task => run(async () => {
    await api.remove(task.id);
    setTasks(ts => ts.filter(t => t.id !== task.id));
  });

  const handleToggleStar = task => run(async () => {
    const updated = await api.update(task.id, { starred: !task.starred });
    setTasks(ts => ts.map(t => (t.id === updated.id ? updated : t)));
  });

  let empty = null;
  if (!loading && visible.length === 0) {
    if (!tasks.length) {
      empty = { title: 'Your timeline starts here', text: 'Log the first thing you finished and it will show up here.' };
    } else if (search || filter) {
      empty = { title: 'No matching tasks', text: 'Try a different search, category or time range.', action: 'Clear filters' };
    } else {
      empty = { title: 'Nothing in this period', text: 'No tasks were completed in the selected range.', action: 'Show all time' };
    }
  }

  function clearFilters() {
    setSearch('');
    setFilter('');
    if (!search && !filter) setRange('all');
  }

  return (
    <>
      <div className="bg-glow" aria-hidden="true" />
      <Header
        search={search}
        onSearch={setSearch}
        category={categories.includes(filter) ? filter : ''}
        onCategory={setFilter}
        categories={categories}
        view={view}
        onView={setView}
        theme={theme}
        onToggleTheme={toggleTheme}
        onOpenConnection={() => setConnectOpen(true)}
      />

      <main className="container">
        <Stats tasks={tasks} />

        {error && <div className="alert" role="alert">{error}</div>}

        <h2 className="section-title">Timeline</h2>

        <FilterBar
          tasks={inScope}
          categories={categories}
          filter={filter}
          onFilter={setFilter}
          range={range}
          onRange={setRange}
        />

        {loading ? (
          <div className="skeleton-list">
            {slow && <p className="muted-note">Waking up the server… free hosting can take up to a minute.</p>}
            {[0, 1, 2].map(i => <div key={i} className="skeleton" />)}
          </div>
        ) : empty ? (
          <div className="empty">
            <div className="empty-icon"><PlusIcon /></div>
            <h3>{empty.title}</h3>
            <p>{empty.text}</p>
            {empty.action && <button className="btn ghost empty-action" onClick={clearFilters}>{empty.action}</button>}
          </div>
        ) : (
          <TaskViews
            view={view}
            tasks={visible}
            onEdit={task => setModal({ task })}
            onDelete={handleDelete}
            onToggleStar={handleToggleStar}
          />
        )}
      </main>

      <div className="dock">
        <button className="fab" onClick={() => setModal({})}>
          <PlusIcon />
          Add completed task
        </button>
      </div>

      {modal && (
        <TaskModal
          task={modal.task}
          categories={categories}
          onSave={handleSave}
          onClose={() => setModal(null)}
        />
      )}

      {connectOpen && (
        <ConnectForm
          onClose={() => setConnectOpen(false)}
          onConnected={() => { setConnectOpen(false); load(); }}
          onDisconnect={() => {
            clearConnection();
            setConnectOpen(false);
            setTasks([]);
            setConnected(false);
          }}
        />
      )}
    </>
  );
}
