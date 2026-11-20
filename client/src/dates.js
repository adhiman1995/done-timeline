// Shared date helpers for the timeline, toolbar and insights.

export const RANGES = [
  ['all', 'All'],
  ['today', 'Today'],
  ['7d', '7 days'],
  ['30d', '30 days'],
];
const RANGE_DAYS = { today: 1, '7d': 7, '30d': 30 };

export function startOfDay(d) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

export function daysAgo(n) {
  const d = startOfDay(new Date());
  d.setDate(d.getDate() - n);
  return d;
}

export function inRange(task, range) {
  const days = RANGE_DAYS[range];
  return !days || new Date(task.completedAt) >= daysAgo(days - 1);
}

/** Today / Yesterday / "Wed, Nov 18" (+ year when not this year). */
export function dayLabel(date) {
  const d = startOfDay(date).getTime();
  if (d === daysAgo(0).getTime()) return 'Today';
  if (d === daysAgo(1).getTime()) return 'Yesterday';
  return date.toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: date.getFullYear() === new Date().getFullYear() ? undefined : 'numeric',
  });
}

/** Tone for a day badge: today (amber), this week (blue) or older (grey). */
export function dayTone(date) {
  const d = startOfDay(date);
  if (d >= daysAgo(0)) return 'today';
  if (d >= daysAgo(6)) return 'week';
  return 'older';
}

export const timeOf = iso => new Date(iso).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });

export function relative(iso) {
  const mins = Math.round((Date.now() - new Date(iso)) / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins} min ago`;
  const hours = Math.round(mins / 60);
  if (hours < 24) return `${hours} h ago`;
  return dayLabel(new Date(iso));
}

/** Consecutive days with at least one task, ending today (or yesterday if nothing yet today). */
export function streak(tasks) {
  const days = new Set(tasks.map(t => startOfDay(t.completedAt).getTime()));
  const cursor = daysAgo(0);
  if (!days.has(cursor.getTime())) cursor.setDate(cursor.getDate() - 1);
  let n = 0;
  while (days.has(cursor.getTime())) {
    n++;
    cursor.setDate(cursor.getDate() - 1);
  }
  return n;
}
