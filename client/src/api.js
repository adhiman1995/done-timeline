// Connection settings are entered at runtime and kept in this browser only.
// The token is never baked into the static bundle that GitHub Pages serves.
const DEFAULT_URL = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');
const KEY = 'connection';

export class AuthError extends Error {}

export function getConnection() {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY) || 'null');
    if (saved?.token) return { url: saved.url ?? DEFAULT_URL, token: saved.token };
  } catch {}
  return { url: DEFAULT_URL, token: '' };
}

export function saveConnection(conn) {
  try { localStorage.setItem(KEY, JSON.stringify({ url: conn.url.replace(/\/+$/, ''), token: conn.token })); } catch {}
}

export function clearConnection() {
  try { localStorage.removeItem(KEY); } catch {}
}

export const defaultApiUrl = DEFAULT_URL;

async function request(path, options = {}, conn = getConnection()) {
  let res;
  try {
    res = await fetch(`${conn.url}/api${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${conn.token}`,
        ...options.headers,
      },
    });
  } catch {
    throw new Error('Could not reach the server. Check the URL and your connection.');
  }
  if (res.status === 401) throw new AuthError('The access token was rejected.');
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error || `Request failed (${res.status})`);
  }
  return res.status === 204 ? null : res.json();
}

export const api = {
  check: conn => request('/auth/check', {}, conn),
  list: () => request('/tasks'),
  create: data => request('/tasks', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  remove: id => request(`/tasks/${id}`, { method: 'DELETE' }),
};
