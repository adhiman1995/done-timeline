// Goal helpers shared by the goals card and modal.
import { startOfDay } from './dates.js';

export const PERIODS = [
  ['day', 'per day', 'today'],
  ['week', 'per week', 'this week'],
  ['month', 'per month', 'this month'],
];

/** Start of the current day / week (Monday) / month. */
export function periodStart(period) {
  const d = startOfDay(new Date());
  if (period === 'week') d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  if (period === 'month') d.setDate(1);
  return d;
}

export function goalProgress(goal, tasks) {
  const start = periodStart(goal.period);
  const cat = goal.category.trim().toLowerCase();
  const done = tasks.filter(
    t => new Date(t.completedAt) >= start && (!cat || t.category.trim().toLowerCase() === cat)
  ).length;
  return { done, ratio: Math.min(1, done / goal.target), met: done >= goal.target };
}

export const goalName = g => g.category || 'Any task';
export const periodWord = p => PERIODS.find(([k]) => k === p)?.[1] ?? '';
export const periodNow = p => PERIODS.find(([k]) => k === p)?.[2] ?? '';
