// Ticked AI: builds the task lines and calls the AI proxy (ai-worker/), which holds the Groq key.
import { idToken } from './firebase.js';
import { DEMO, demoBackend } from './demo.js';
import { daysAgo, startOfDay } from './dates.js';

const AI_URL = import.meta.env.VITE_AI_URL || 'https://ticked-ai.ticked-ai.workers.dev';

const fmt = iso =>
  new Date(iso).toLocaleString('en-GB', { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });

/** "Mon 17 Nov, 09:30 · Title [Work] ★ — notes" (oldest first). */
function toLines(tasks) {
  return [...tasks]
    .sort((a, b) => a.completedAt.localeCompare(b.completedAt))
    .map(t =>
      `${fmt(t.completedAt)} · ${t.title}${t.category ? ` [${t.category}]` : ''}${t.starred ? ' ★' : ''}` +
      (t.notes ? ` — ${t.notes.replace(/\s+/g, ' ').slice(0, 160)}` : '')
    );
}

/** Tasks from the last 7 days (including today). */
export function weeklyInput(tasks) {
  const start = daysAgo(6);
  const list = tasks.filter(t => new Date(t.completedAt) >= start);
  return { lines: toLines(list), period: `the last 7 days (${fmt(start.toISOString()).split(',')[0]} to today)`, count: list.length };
}

const isMondayish = () => [0, 1].includes(new Date().getDay());

/** Standup time windows. Standups only use tasks tagged "Work". */
export const STANDUP_PERIODS = [
  { key: 'yesterday', label: () => (isMondayish() ? 'Since Friday' : 'Since yesterday'),
    back: () => ({ 0: 2, 1: 3 })[new Date().getDay()] ?? 1,
    heading: () => (isMondayish() ? 'Since Friday' : 'Yesterday'),
    text: () => (isMondayish() ? 'since Friday' : 'yesterday and today') },
  { key: 'today', label: () => 'Today', back: () => 0, heading: () => 'Done today', text: () => 'today' },
  { key: '3d', label: () => 'Last 3 days', back: () => 2, heading: () => 'Last 3 days', text: () => 'in the last 3 days' },
  { key: 'week', label: () => 'This week', back: () => (new Date().getDay() + 6) % 7, heading: () => 'This week', text: () => 'this week' },
];

export const isWork = t => t.category.trim().toLowerCase() === 'work';

export function standupInput(tasks, periodKey = 'yesterday') {
  const p = STANDUP_PERIODS.find(x => x.key === periodKey) ?? STANDUP_PERIODS[0];
  const start = startOfDay(new Date());
  start.setDate(start.getDate() - p.back());
  const list = tasks.filter(t => new Date(t.completedAt) >= start && isWork(t));
  return {
    lines: toLines(list),
    period: p.text(),
    heading: p.heading(),
    count: list.length,
    tasks: [...list].sort((a, b) => b.completedAt.localeCompare(a.completedAt)),
  };
}

export async function askAi(kind, payload) {
  if (import.meta.env.DEV && DEMO) return demoBackend.ai(kind);
  const token = await idToken();
  if (!token) throw new Error('Please sign in again.');
  let res;
  try {
    res = await fetch(`${AI_URL}/v1/ai`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ kind, ...payload }),
    });
  } catch {
    throw new Error("Couldn't reach Ticked AI. Check your connection and try again.");
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || `Ticked AI failed (${res.status}).`);
  return data.text;
}
