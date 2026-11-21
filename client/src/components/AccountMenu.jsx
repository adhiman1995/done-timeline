import { useRef, useState } from 'react';
import { importTasks, logOut, deleteAccount, friendlyError } from '../firebase.js';
import { CloseIcon } from './icons.jsx';

/** Account dialog: who is signed in, JSON export / import and sign out. */
export default function AccountMenu({ user, tasks, onClose }) {
  const fileRef = useRef(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null); // { ok, text }
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  async function removeAccount() {
    setDeleting(true);
    setMessage(null);
    try {
      const done = await deleteAccount();
      if (done) onClose();
      else setMessage({ ok: false, text: 'Account deletion was cancelled.' });
    } catch (err) {
      setMessage({ ok: false, text: friendlyError(err) });
    } finally {
      setDeleting(false);
      setConfirmDelete(false);
    }
  }

  function exportJson() {
    const blob = new Blob([JSON.stringify(tasks, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `ticked-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  async function importJson(e) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setBusy(true);
    setMessage(null);
    try {
      const data = JSON.parse(await file.text());
      const list = Array.isArray(data) ? data : data.tasks;
      if (!Array.isArray(list)) throw new Error('That file does not contain a list of tasks.');
      const n = await importTasks(user.uid, list);
      setMessage({ ok: true, text: `Imported ${n} task${n === 1 ? '' : 's'}.` });
    } catch (err) {
      setMessage({ ok: false, text: err instanceof SyntaxError ? 'That file is not valid JSON.' : friendlyError(err) });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="overlay" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-label="Account">
        <div className="modal-head">
          <h2>Account</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close"><CloseIcon /></button>
        </div>

        <div className="account-who">
          {user.photoURL
            ? <img src={user.photoURL} alt="" referrerPolicy="no-referrer" />
            : <span className="avatar-fallback">{(user.displayName || user.email || '?')[0].toUpperCase()}</span>}
          <div>
            <div className="account-name">{user.displayName || 'Signed in'}</div>
            <div className="account-email">{user.email}</div>
          </div>
        </div>

        <div className="account-actions">
          <button className="btn ghost" onClick={exportJson} disabled={!tasks.length}>Export tasks (JSON)</button>
          <button className="btn ghost" onClick={() => fileRef.current?.click()} disabled={busy}>
            {busy ? 'Importing…' : 'Import tasks (JSON)'}
          </button>
          <a className="btn ghost" href="./privacy.html" target="_blank" rel="noopener">Privacy Policy</a>
          <input ref={fileRef} type="file" accept="application/json,.json" hidden onChange={importJson} />
        </div>

        {message && <div className={message.ok ? 'notice' : 'alert'}>{message.text}</div>}

        {confirmDelete && (
          <div className="alert danger-zone">
            <strong>Delete your account?</strong> This permanently deletes your Ticked account and all your tasks,
            on every device. You'll be asked to confirm with Google. This can't be undone.
            <div className="danger-actions">
              <button className="btn-mini" onClick={() => setConfirmDelete(false)} disabled={deleting}>Cancel</button>
              <button className="btn-mini danger" onClick={removeAccount} disabled={deleting}>
                {deleting ? 'Deleting…' : 'Delete account'}
              </button>
            </div>
          </div>
        )}

        <div className="modal-foot">
          <button className="btn ghost danger-text" onClick={() => { onClose(); logOut(); }} style={{ marginRight: 'auto' }}>
            Sign out
          </button>
          {!confirmDelete && (
            <button className="btn ghost danger-text" onClick={() => setConfirmDelete(true)}>Delete account</button>
          )}
          <button className="btn primary" onClick={onClose}>Done</button>
        </div>
      </div>
    </div>
  );
}
