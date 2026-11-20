import { useEffect, useState } from 'react';
import { CloseIcon } from './icons.jsx';
import { categoryHue } from '../colors.js';

function toLocalInput(date) {
  const p = n => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}T${p(date.getHours())}:${p(date.getMinutes())}`;
}

export default function TaskModal({ task, categories, onSave, onDelete, onClose }) {
  const [form, setForm] = useState(() => ({
    title: task?.title ?? '',
    notes: task?.notes ?? '',
    category: task?.category ?? '',
    when: toLocalInput(task ? new Date(task.completedAt) : new Date()),
  }));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    const onKey = e => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [onClose]);

  const set = field => e => setForm(f => ({ ...f, [field]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    if (!form.title.trim()) return;
    setSaving(true);
    setError('');
    const err = await onSave({
      title: form.title,
      notes: form.notes,
      category: form.category,
      completedAt: new Date(form.when).toISOString(),
    });
    if (err) {
      setError(err);
      setSaving(false);
    }
  }

  return (
    <div className="overlay" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <form className="modal" onSubmit={submit} role="dialog" aria-modal="true" aria-labelledby="modal-title" autoComplete="off">
        <div className="modal-head">
          <h2 id="modal-title">{task ? 'Edit task' : 'Log a completed task'}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">
            <CloseIcon />
          </button>
        </div>

        <label className="field">
          <span>What did you complete?</span>
          <input
            value={form.title}
            onChange={set('title')}
            placeholder="e.g. Shipped the onboarding flow"
            maxLength={200}
            required
            autoFocus
          />
        </label>

        <label className="field">
          <span>Notes <em>optional</em></span>
          <textarea value={form.notes} onChange={set('notes')} rows={3} placeholder="Any details worth remembering" />
        </label>

        <div className="field-row">
          <label className="field">
            <span>Completed at</span>
            <input type="datetime-local" value={form.when} onChange={set('when')} required />
          </label>
          <label className="field">
            <span>Category <em>optional</em></span>
            <input
              value={form.category}
              onChange={set('category')}
              placeholder="e.g. Work"
              maxLength={40}
              list="category-options"
            />
            <datalist id="category-options">
              {categories.map(c => <option key={c} value={c} />)}
            </datalist>
          </label>
        </div>

        {categories.length > 0 && (
          <div className="chip-picker">
            {categories.map(c => (
              <button
                type="button"
                key={c}
                className={`chip ${form.category === c ? 'active' : ''}`}
                style={{ '--hue': categoryHue(c) }}
                onClick={() => setForm(f => ({ ...f, category: f.category === c ? '' : c }))}
              >
                {c}
              </button>
            ))}
          </div>
        )}

        {error && <div className="alert">{error}</div>}

        <div className="modal-foot">
          {task && onDelete && (
            confirmDelete ? (
              <span className="push">
                <button type="button" className="btn ghost danger-text" onClick={() => onDelete(task)}>Delete task</button>
              </span>
            ) : (
              <button type="button" className="btn ghost danger-text push" onClick={() => setConfirmDelete(true)}>Delete</button>
            )
          )}
          <button type="button" className="btn ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn primary" disabled={saving || !form.title.trim()}>
            {saving ? 'Saving…' : task ? 'Save changes' : 'Add to timeline'}
          </button>
        </div>
      </form>
    </div>
  );
}
