// Development-only demo data (open http://localhost:5173/?demo).
// Lets the UI be previewed without signing in; never used in production builds.

export const DEMO = import.meta.env.DEV && new URLSearchParams(location.search).has('demo');

export const demoUser = { uid: 'demo', displayName: 'Alok', email: 'demo@example.com', photoURL: null };

const SAMPLE = [
  [0, 9.5, 'Shipped the onboarding flow', 'Work', 'Feature flag on for 10% of new users.', true],
  [0, 8, 'Morning run · 5 km', 'Health', '28 minutes, easy pace.'],
  [0, 7.2, 'Reviewed pull requests', 'Work', ''],
  [1, 18, 'Finished chapter 4 of Designing Data-Intensive Apps', 'Learning', 'Notes on replication lag.'],
  [1, 13, 'Paid electricity bill', 'Personal', ''],
  [1, 10, 'Sprint planning', 'Work', 'Committed to 21 points.'],
  [2, 20, 'Called mom', 'Family', '', true],
  [2, 9, 'Fixed flaky login test', 'Work', 'Race condition in the session refresh.'],
  [3, 19, 'Yoga class', 'Health', ''],
  [3, 11, 'Wrote design doc for sync', 'Work', 'Last write wins, server-capped timestamps.', true],
  [4, 16, 'Kotlin coroutines course · module 3', 'Learning', ''],
  [5, 12, 'Grocery shopping', 'Personal', ''],
  [6, 9, 'Quarterly goals review', 'Work', ''],
  [8, 18, 'Swim · 1 km', 'Health', ''],
  [11, 15, 'Renewed passport', 'Personal', 'Appointment confirmed.'],
  [15, 10, 'Launched v2 landing page', 'Work', '', true],
];

// Built lazily so production builds (where DEMO is false) drop all of this.
const makeTasks = () => SAMPLE.map(([days, hour, title, category, notes, starred], i) => {
  const d = new Date();
  d.setDate(d.getDate() - days);
  d.setHours(Math.floor(hour), Math.round((hour % 1) * 60), 0, 0);
  if (days === 0 && d > new Date()) d.setTime(Date.now() - (i + 1) * 40 * 60000);
  return { id: `demo-${i}`, title, notes, category, starred: !!starred, completedAt: d.toISOString() };
});
let tasks = null;

const listeners = new Set();
const emit = () => listeners.forEach(fn => fn([...tasks]));

export const demoBackend = {
  watchTasks(_uid, onData) {
    tasks ??= makeTasks();
    listeners.add(onData);
    onData([...tasks]);
    return () => listeners.delete(onData);
  },
  async createTask(_uid, data) {
    tasks = [...tasks, { id: `demo-${Date.now()}`, starred: false, ...data }];
    emit();
  },
  async updateTask(_uid, id, data) {
    tasks = tasks.map(t => (t.id === id ? { ...t, ...data } : t));
    emit();
  },
  async deleteTask(_uid, id) {
    tasks = tasks.filter(t => t.id !== id);
    emit();
  },
};
