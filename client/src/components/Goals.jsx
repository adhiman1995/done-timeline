import { useEffect, useState } from 'react';
import { categoryHue } from '../colors.js';
import { PERIODS, goalName, goalProgress, periodNow, periodWord } from '../goals.js';
import { CheckIcon, CloseIcon, PlusIcon, TargetIcon } from './icons.jsx';

function Ring({ ratio, met, hue }) {
  const r = 17;
  const c = 2 * Math.PI * r;
  return (
    <svg className="ring" width="44" height="44" viewBox="0 0 44 44" style={{ '--hue': hue }} aria-hidden="true">
      <circle cx="22" cy="22" r={r} className="ring-track" />
      <circle
        cx="22" cy="22" r={r} className={`ring-fill ${met ? 'met' : ''}`}
        strokeDasharray={c} strokeDashoffset={c * (1 - ratio)} transform="rotate(-90 22 22)"
      />
      {met && <g transform="translate(14 14)" className="ring-check"><CheckIcon width={16} height={16} strokeWidth={3} /></g>}
    </svg>
  );
}

/** Goals summary card with progress rings. */
export default function GoalsCard({ goals, tasks, onAdd, onEdit }) {
  const met = goals.filter(g => goalProgress(g, tasks).met).length;
  return (
    <section className="icard">
      <div className="icard-head">
        <h2>Goals</h2>
        {goals.length > 0 && <span className="muted">{met} of {goals.length} on track</span>}
      </div>
      {goals.length === 0 ? (
        <div className="goals-empty">
          <TargetIcon width={22} height={22} />
          <p>Set a target, like <strong>Health · 4 per week</strong>, and watch it fill up as you log.</p>
        </div>
      ) : (
        <ul className="goal-list">
          {goals.map(g => {
            const p = goalProgress(g, tasks);
            return (
              <li key={g.id}>
                <button onClick={() => onEdit(g)} title="Edit goal">
                  <Ring ratio={p.ratio} met={p.met} hue={g.category ? categoryHue(g.category) : 217} />
                  <span className="goal-text">
                    <span className="goal-name">{goalName(g)} · {g.target} {periodWord(g.period)}</span>
                    <span className="goal-sub">{p.met ? 'Goal met' : `${p.done} of ${g.target}`} {periodNow(g.period)}</span>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
      <button className="btn soft small" onClick={onAdd}><PlusIcon width={15} height={15} /> Add goal</button>
    </section>
  );
}

/** Create / edit / delete a goal. */
export function GoalModal({ goal, categories, onSave, onDelete, onClose }) {
  const [form, setForm] = useState(() => ({
    category: goal?.category ?? '',
    target: goal?.target ?? 3,
    period: goal?.period ?? 'week',
  }));
  useEffect(() => {
    const onKey = e => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  const set = patch => setForm(f => ({ ...f, ...patch }));
  const options = ['', ...categories];

  return (
    <div className="overlay" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <form className="modal" role="dialog" aria-modal="true" aria-labelledby="goal-title"
        onSubmit={e => { e.preventDefault(); onSave(form); }}>
        <div className="modal-head">
          <h2 id="goal-title">{goal ? 'Edit goal' : 'New goal'}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close"><CloseIcon /></button>
        </div>

        <div className="field">
          <span>What counts</span>
          <div className="chip-picker">
            {options.map(c => (
              <button type="button" key={c || 'any'}
                className={`chip ${form.category.toLowerCase() === c.toLowerCase() ? 'active' : ''}`}
                style={{ '--hue': c ? categoryHue(c) : 217 }} onClick={() => set({ category: c })}>
                {c || 'Any task'}
              </button>
            ))}
          </div>
          <input value={form.category} onChange={e => set({ category: e.target.value })} maxLength={40}
            placeholder="Or type a category" aria-label="Category" />
        </div>

        <div className="field-row">
          <label className="field">
            <span>Target</span>
            <div className="stepper">
              <button type="button" onClick={() => set({ target: Math.max(1, form.target - 1) })} aria-label="Fewer">−</button>
              <input type="number" min={1} max={100} value={form.target}
                onChange={e => set({ target: Math.min(100, Math.max(1, Number(e.target.value) || 1)) })} />
              <button type="button" onClick={() => set({ target: Math.min(100, form.target + 1) })} aria-label="More">+</button>
            </div>
          </label>
          <div className="field">
            <span>Every</span>
            <div className="tabs period-tabs" role="tablist">
              {PERIODS.map(([key]) => (
                <button type="button" key={key} role="tab" aria-selected={form.period === key}
                  className={`tab ${form.period === key ? 'active' : ''}`} onClick={() => set({ period: key })}>
                  {key[0].toUpperCase() + key.slice(1)}
                </button>
              ))}
            </div>
          </div>
        </div>

        <p className="goal-preview">
          <TargetIcon width={16} height={16} /> {goalName(form)} · {form.target} {periodWord(form.period)}
        </p>

        <div className="modal-foot">
          {goal && <button type="button" className="btn ghost danger-text push" onClick={() => onDelete(goal)}>Delete</button>}
          <button type="button" className="btn ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn primary">{goal ? 'Save goal' : 'Add goal'}</button>
        </div>
      </form>
    </div>
  );
}
