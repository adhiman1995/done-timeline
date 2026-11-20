import { useState } from 'react';
import { api, AuthError, getConnection, saveConnection, defaultApiUrl } from '../api.js';
import { CheckIcon, CloseIcon } from './icons.jsx';

/** Server URL + access token form. Used full-page on first run and as a modal from the header. */
export default function ConnectForm({ onConnected, onClose, onDisconnect, reason }) {
  const current = getConnection();
  const [url, setUrl] = useState(current.url || defaultApiUrl);
  const [token, setToken] = useState(current.token);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(reason || '');

  async function submit(e) {
    e.preventDefault();
    const conn = { url: url.trim().replace(/\/+$/, ''), token: token.trim() };
    setBusy(true);
    setError('');
    try {
      await api.check(conn);
      saveConnection(conn);
      onConnected();
    } catch (err) {
      setError(err instanceof AuthError ? 'That access token is not valid for this server.' : err.message);
    } finally {
      setBusy(false);
    }
  }

  const form = (
    <form className={onClose ? 'modal' : 'modal connect-card'} onSubmit={submit} autoComplete="off">
      <div className="modal-head">
        <div className="connect-title">
          {!onClose && <div className="logo"><CheckIcon width={20} height={20} strokeWidth={2.5} /></div>}
          <h2>{onClose ? 'Server connection' : 'Connect to your server'}</h2>
        </div>
        {onClose && (
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close"><CloseIcon /></button>
        )}
      </div>
      {!onClose && (
        <p className="connect-intro">
          Your tasks are stored on your own server. Enter its address and the access token
          you set as <code>API_TOKEN</code>. They're saved in this browser only.
        </p>
      )}

      <label className="field">
        <span>Server URL {defaultApiUrl === '' && <em>leave empty when running locally</em>}</span>
        <input
          type="url"
          value={url}
          onChange={e => setUrl(e.target.value)}
          placeholder="https://your-api.onrender.com"
        />
      </label>
      <label className="field">
        <span>Access token</span>
        <input
          type="password"
          value={token}
          onChange={e => setToken(e.target.value)}
          placeholder="Paste your API token"
          required
          autoFocus
        />
      </label>

      {error && <div className="alert">{error}</div>}

      <div className="modal-foot">
        {onDisconnect && (
          <button type="button" className="btn ghost danger-text" onClick={onDisconnect} style={{ marginRight: 'auto' }}>
            Disconnect
          </button>
        )}
        {onClose && <button type="button" className="btn ghost" onClick={onClose}>Cancel</button>}
        <button type="submit" className="btn primary" disabled={busy || !token.trim()}>
          {busy ? 'Connecting…' : 'Connect'}
        </button>
      </div>
    </form>
  );

  if (!onClose) return <div className="connect-page">{form}</div>;
  return (
    <div className="overlay" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      {form}
    </div>
  );
}
