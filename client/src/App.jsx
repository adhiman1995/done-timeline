import { useEffect, useMemo, useState } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { auth, watchTasks, createTask, updateTask, deleteTask, logOut, friendlyError } from './firebase.js';
import SignIn from './components/SignIn.jsx';
import AccountMenu from './components/AccountMenu.jsx';
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
  const [user, setUser] = useState(undefined); // undefined while Firebase restores the session
  const [notice, setNotice] = useState('');
  const [accountOpen, setAccountOpen] = useState(false);

  useEffect(() => onAuthStateChanged(auth, u => setUser(u)), []);

  // Live subscription: changes from the Android app or another tab appear without reloading.
  useEffect(() => {
    setTasks([]);
    if (!user) return;
    setLoading(true);
    return watchTasks(
      user.uid,
      list => { setTasks(list); setLoading(false); },
      e => {
        setLoading(false);
        if (e.code === 'permission-denied') {
          setNotice(`${user.email} doesn't have access to this timeline. Sign in with the account it belongs to.`);
          logOut();
        } else {
          setError(friendlyError(e));
        }
      },
    );
  }, [user]);

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

  // Firestore applies writes locally at once (and syncs later when offline), so nothing is awaited here.
  const write = promise => { setError(''); promise.catch(e => setError(friendlyError(e))); };

  function handleSave(data) {
    write(modal?.task ? updateTask(user.uid, modal.task.id, data) : createTask(user.uid, data));
    setModal(null);
  }

  if (user === undefined) return <div className="bg-glow" aria-hidden="true" />;

  if (!user) {
    return (
      <>
        <div className="bg-glow" aria-hidden="true" />
        <SignIn notice={notice} />
      </>
    );
  }

  const handleDelete = task => write(deleteTask(user.uid, task.id));
  const handleToggleStar = task => write(updateTask(user.uid, task.id, { starred: !task.starred }));

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
        user={user}
        onOpenAccount={() => { setNotice(''); setAccountOpen(true); }}
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

      {accountOpen && <AccountMenu user={user} tasks={tasks} onClose={() => setAccountOpen(false)} />}
    </>
  );
}
