// Ticked AI: builds the task lines and calls the AI proxy (ai-worker/), which holds the Groq key.
import { idToken } from './firebase.js';
import { DEMO, demoBackend } from './demo.js';
import { daysAgo, startOfDay } from './dates.js';

const AI_URL = import.meta.env.VITE_AI_URL || 'https://ticked-ai.adhiman1995.workers.dev';

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

/** Tasks since the start of the previous working day (Friday when it's Monday), plus anything done today. */
export function standupInput(tasks) {
  const today = startOfDay(new Date());
  const back = today.getDay() === 1 ? 3 : today.getDay() === 0 ? 2 : 1;
  const start = new Date(today);
  start.setDate(today.getDate() - back);
  const list = tasks.filter(t => new Date(t.completedAt) >= start);
  return { lines: toLines(list), period: back === 1 ? 'yesterday and today so far' : 'the last working day and today so far', count: list.length };
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
