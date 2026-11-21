import { useEffect, useMemo, useRef, useState } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import * as firebase from './firebase.js';
import { DEMO as DEMO_PARAM, demoUser, demoBackend } from './demo.js';
import SignIn from './components/SignIn.jsx';
import AccountMenu from './components/AccountMenu.jsx';
import Rail from './components/Rail.jsx';
import Sidebar from './components/Sidebar.jsx';
import Toolbar from './components/Toolbar.jsx';
import Insights from './components/Insights.jsx';
import TaskViews from './components/Timeline.jsx';
import TaskModal from './components/TaskModal.jsx';
import GoalsCard, { GoalModal } from './components/Goals.jsx';
import { AiCard, AiModal } from './components/Ai.jsx';
import { STARRED } from './components/filters.js';
import { PlusIcon, SearchIcon, CloseIcon } from './components/icons.jsx';
import { RANGES, inRange, streak } from './dates.js';
import { useTheme } from './useTheme.js';

// `import.meta.env.DEV` is false in production builds, so the demo code is dropped there.
const DEMO = import.meta.env.DEV && DEMO_PARAM;
const backend = DEMO ? demoBackend : firebase;
const RANGE_TEXT = { all: 'all time', today: 'today', '7d': 'last 7 days', '30d': 'last 30 days' };
const isMac = /Mac|iPhone|iPad/.test(navigator.platform);

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
  const [user, setUser] = useState(DEMO ? demoUser : undefined); // undefined while Firebase restores the session
  const [notice, setNotice] = useState('');
  const [accountOpen, setAccountOpen] = useState(false);
  const [goals, setGoals] = useState([]);
  const [goalModal, setGoalModal] = useState(null); // null | { goal?: Goal }
  const [aiKind, setAiKind] = useState(null); // null | 'weekly' | 'standup'
  const searchRef = useRef(null);

  useEffect(() => (DEMO ? undefined : onAuthStateChanged(firebase.auth, u => setUser(u))), []);

  // Live subscription: changes from the Android app or another tab appear without reloading.
  useEffect(() => {
    setTasks([]);
    if (!user) return;
    setLoading(true);
    return backend.watchTasks(
      user.uid,
      list => { setTasks(list); setLoading(false); },
      e => {
        setLoading(false);
        if (e.code === 'permission-denied') {
          setNotice("Your tasks couldn't be loaded (permission denied). Please sign in again.");
          firebase.logOut();
        } else {
          setError(firebase.friendlyError(e));
        }
      },
    );
  }, [user]);

  useEffect(() => {
    setGoals([]);
    if (!user) return;
    return backend.watchGoals(user.uid, setGoals, e => setError(firebase.friendlyError(e)));
  }, [user]);

  // Ctrl/⌘ K focuses search.
  useEffect(() => {
    const onKey = e => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        searchRef.current?.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, []);

  const categories = useMemo(
    () => [...new Set(tasks.map(t => t.category).filter(Boolean))].sort(),
    [tasks]
  );

  // Category / starred filter + search first; range tabs show counts over this set.
  const base = useMemo(() => {
    const q = search.trim().toLowerCase();
    return tasks
      .filter(t => !filter || (filter === STARRED ? t.starred : t.category === filter))
      .filter(t => !q || `${t.title} ${t.notes} ${t.category}`.toLowerCase().includes(q))
      .sort((a, b) => b.completedAt.localeCompare(a.completedAt));
  }, [tasks, filter, search]);

  const rangeCounts = useMemo(
    () => Object.fromEntries(RANGES.map(([key]) => [key, base.filter(t => inRange(t, key)).length])),
    [base]
  );
  const visible = useMemo(() => base.filter(t => inRange(t, range)), [base, range]);
  const currentStreak = useMemo(() => streak(tasks), [tasks]);

  // Firestore applies writes locally at once (and syncs later when offline), so nothing is awaited here.
  const write = promise => { setError(''); promise.catch(e => setError(firebase.friendlyError(e))); };

  function handleSave(data) {
    write(modal?.task ? backend.updateTask(user.uid, modal.task.id, data) : backend.createTask(user.uid, data));
    setModal(null);
  }

  if (user === undefined) return null;

  if (!user) return <SignIn notice={notice} />;

  const handleDelete = task => write(backend.deleteTask(user.uid, task.id));
  const handleToggleStar = task => write(backend.updateTask(user.uid, task.id, { starred: !task.starred }));
  const openAccount = () => { setNotice(''); setAccountOpen(true); };

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

  const now = new Date();
  const subtitle =
    `${now.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })}, ` +
    `${now.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })} · ` +
    `${visible.length} task${visible.length === 1 ? '' : 's'}, ${RANGE_TEXT[range]}`;
  const filterLabel = filter === STARRED ? 'Starred' : filter;
  function saveGoal(goal) {
    write(goalModal?.goal ? backend.updateGoal(user.uid, goalModal.goal.id, goal) : backend.createGoal(user.uid, goal));
    setGoalModal(null);
  }

  const insights = (
    <>
    <AiCard onOpen={setAiKind} />
    <GoalsCard goals={goals} tasks={tasks} onAdd={() => setGoalModal({})} onEdit={goal => setGoalModal({ goal })} />
    <Insights
      tasks={tasks}
      onOpenTask={task => setModal({ task })}
      onFilter={c => { setFilter(c); setRange('all'); }}
      onExport={openAccount}
    />
    </>
  );

  return (
    <div className="app">
      <Rail user={user} theme={theme} onToggleTheme={toggleTheme} onAdd={() => setModal({})} onAi={() => setAiKind('weekly')} onAccount={openAccount} />
      <Sidebar
        tasks={tasks}
        categories={categories}
        filter={filter}
        onFilter={setFilter}
        streak={currentStreak}
        onAdd={() => setModal({})}
      />

      <main className="main">
        <header className="page-head">
          <div>
            <h1>Timeline</h1>
            <p className="page-sub">{subtitle}</p>
          </div>
          <label className="search">
            <SearchIcon width={17} height={17} />
            <input
              ref={searchRef}
              type="search"
              placeholder="Search tasks"
              value={search}
              onChange={e => setSearch(e.target.value)}
              aria-label="Search tasks"
            />
            <kbd>{isMac ? '⌘K' : 'Ctrl K'}</kbd>
          </label>
        </header>

        <div className="insights-inline">{insights}</div>

        <Toolbar
          range={range}
          onRange={setRange}
          rangeCounts={rangeCounts}
          view={view}
          onView={setView}
          categories={categories}
          filter={filter}
          onFilter={setFilter}
        />

        {error && <div className="alert" role="alert">{error}</div>}

        <section className="panel">
          <div className="panel-head">
            <div className="panel-title">
              <strong>{visible.length} task{visible.length === 1 ? '' : 's'}</strong>
              <span className="muted">Newest first</span>
            </div>
            <div className="panel-tags">
              {filterLabel && (
                <button className="tag" onClick={() => setFilter('')} title="Clear filter">
                  {filterLabel} <CloseIcon width={12} height={12} />
                </button>
              )}
              <span className="tag accent">{RANGES.find(([k]) => k === range)?.[1]}</span>
            </div>
          </div>

          {loading ? (
            <div className="skeleton-list">
              {[0, 1, 2].map(i => <div key={i} className="skeleton" />)}
            </div>
          ) : empty ? (
            <div className="empty">
              <div className="empty-icon"><PlusIcon /></div>
              <h3>{empty.title}</h3>
              <p>{empty.text}</p>
              {empty.action && <button className="btn soft" onClick={clearFilters}>{empty.action}</button>}
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
        </section>
      </main>

      <aside className="aside">{insights}</aside>

      <button className="fab" onClick={() => setModal({})} aria-label="Add completed task" title="Add completed task">
        <PlusIcon width={26} height={26} strokeWidth={2.4} />
      </button>

      {modal && (
        <TaskModal
          task={modal.task}
          categories={categories}
          onSave={handleSave}
          onDelete={task => { handleDelete(task); setModal(null); }}
          onClose={() => setModal(null)}
        />
      )}

      {goalModal && (
        <GoalModal
          goal={goalModal.goal}
          categories={categories}
          onSave={saveGoal}
          onDelete={goal => { write(backend.deleteGoal(user.uid, goal.id)); setGoalModal(null); }}
          onClose={() => setGoalModal(null)}
        />
      )}

      {aiKind && <AiModal kind={aiKind} tasks={tasks} onClose={() => setAiKind(null)} />}

      {accountOpen && <AccountMenu user={user} tasks={tasks} onClose={() => setAccountOpen(false)} />}
    </div>
  );
}
