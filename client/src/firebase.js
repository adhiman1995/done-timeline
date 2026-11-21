import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, signInWithPopup, signInWithRedirect, signOut, reauthenticateWithPopup, deleteUser } from 'firebase/auth';
import {
  initializeFirestore, persistentLocalCache, persistentMultipleTabManager,
  collection, doc, onSnapshot, setDoc, updateDoc, deleteDoc, writeBatch, serverTimestamp, Timestamp, getDocsFromServer,
} from 'firebase/firestore';

// These values identify the Firebase project; they are public by design.
// Access to data is enforced by the Firestore security rules (see firestore.rules).
const firebaseConfig = {
  apiKey: 'AIzaSyDFEpZchbJLyCmMxupAzOhMAUmEMcVzIVo',
  authDomain: 'timeline-project-9286d.firebaseapp.com',
  projectId: 'timeline-project-9286d',
  storageBucket: 'timeline-project-9286d.firebasestorage.app',
  messagingSenderId: '283206404389',
  appId: '1:283206404389:web:ec4b2b50328e823469b2cc',
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);

// Cache tasks in the browser so repeat visits only download what changed.
export const db = initializeFirestore(app, {
  localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }),
});

export async function signInWithGoogle() {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: 'select_account' });
  try {
    await signInWithPopup(auth, provider);
  } catch (e) {
    if (e.code === 'auth/popup-blocked') return signInWithRedirect(auth, provider);
    if (e.code === 'auth/popup-closed-by-user' || e.code === 'auth/cancelled-popup-request') return;
    throw e;
  }
}

export const logOut = () => signOut(auth);

/**
 * Permanently deletes the signed-in account and all its tasks.
 * Confirms with Google first (Firebase requires a recent sign-in to delete an account).
 * Returns false if the user closed the Google window.
 */
export async function deleteAccount() {
  const user = auth.currentUser;
  if (!user) return false;
  try {
    await reauthenticateWithPopup(user, new GoogleAuthProvider());
  } catch (e) {
    if (e.code === 'auth/popup-closed-by-user' || e.code === 'auth/cancelled-popup-request') return false;
    throw e;
  }
  const docs = [
    ...(await getDocsFromServer(tasksCol(user.uid))).docs,
    ...(await getDocsFromServer(goalsCol(user.uid))).docs,
  ];
  for (let i = 0; i < docs.length; i += 400) {
    const batch = writeBatch(db);
    docs.slice(i, i + 400).forEach(d => batch.delete(d.ref));
    await batch.commit();
  }
  await deleteUser(user);
  return true;
}

// ---- Tasks: users/{uid}/tasks/{taskId} ----

const tasksCol = uid => collection(db, 'users', uid, 'tasks');
const iso = v => (v instanceof Timestamp ? v.toDate().toISOString() : v || null);
const ts = isoString => Timestamp.fromDate(new Date(isoString));

function fromDoc(snap) {
  const d = snap.data({ serverTimestamps: 'estimate' });
  return {
    id: snap.id,
    title: d.title ?? '',
    notes: d.notes ?? '',
    category: d.category ?? '',
    starred: !!d.starred,
    completedAt: iso(d.completedAt) ?? new Date().toISOString(),
    createdAt: iso(d.createdAt),
    updatedAt: iso(d.updatedAt),
  };
}

/** Live list of the user's tasks. Calls onData with every change (local writes show instantly). */
export function watchTasks(uid, onData, onError) {
  return onSnapshot(tasksCol(uid), snap => onData(snap.docs.map(fromDoc)), onError);
}

const clean = data => ({
  title: String(data.title ?? '').trim().slice(0, 200),
  notes: String(data.notes ?? '').trim().slice(0, 2000),
  category: String(data.category ?? '').trim().slice(0, 40),
});

// Writes apply to the local cache immediately; the returned promise settles once the server confirms.
export function createTask(uid, data) {
  return setDoc(doc(tasksCol(uid)), {
    ...clean(data),
    starred: !!data.starred,
    completedAt: ts(data.completedAt),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
}

export function updateTask(uid, id, data) {
  const patch = { updatedAt: serverTimestamp() };
  if ('title' in data) Object.assign(patch, clean(data));
  if ('starred' in data) patch.starred = !!data.starred;
  if ('completedAt' in data) patch.completedAt = ts(data.completedAt);
  return updateDoc(doc(tasksCol(uid), id), patch);
}

export const deleteTask = (uid, id) => deleteDoc(doc(tasksCol(uid), id));

/** Imports tasks exported as JSON (same shape as exportTasks). Existing tasks with the same id are overwritten. */
export async function importTasks(uid, list) {
  const valid = list.filter(t => t && typeof t.title === 'string' && t.title.trim() && !isNaN(new Date(t.completedAt)));
  for (let i = 0; i < valid.length; i += 400) {
    const batch = writeBatch(db);
    for (const t of valid.slice(i, i + 400)) {
      const ref = t.id && /^[\w-]{1,128}$/.test(t.id) ? doc(tasksCol(uid), t.id) : doc(tasksCol(uid));
      batch.set(ref, {
        ...clean(t),
        starred: !!t.starred,
        completedAt: ts(t.completedAt),
        createdAt: t.createdAt ? ts(t.createdAt) : serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
    await batch.commit();
  }
  return valid.length;
}

// ---- Goals: users/{uid}/goals/{goalId} = { category ('' = any task), target, period, createdAt } ----

const goalsCol = uid => collection(db, 'users', uid, 'goals');

const cleanGoal = g => ({
  category: String(g.category ?? '').trim().slice(0, 40),
  target: Math.min(100, Math.max(1, Math.round(Number(g.target) || 1))),
  period: ['day', 'week', 'month'].includes(g.period) ? g.period : 'week',
});

export function watchGoals(uid, onData, onError) {
  return onSnapshot(goalsCol(uid), snap => onData(snap.docs.map(d => ({ id: d.id, ...cleanGoal(d.data()) }))), onError);
}
export const createGoal = (uid, goal) => setDoc(doc(goalsCol(uid)), { ...cleanGoal(goal), createdAt: serverTimestamp() });
export const updateGoal = (uid, id, goal) => updateDoc(doc(goalsCol(uid), id), cleanGoal(goal));
export const deleteGoal = (uid, id) => deleteDoc(doc(goalsCol(uid), id));

/** Firebase ID token for the AI proxy. */
export const idToken = () => auth.currentUser?.getIdToken();

export function friendlyError(e) {
  if (e?.code === 'permission-denied') return "That change wasn't allowed. Please check the task and try again.";
  if (e?.code === 'auth/user-mismatch') return 'Please confirm with the same Google account you signed in with.';
  if (e?.code === 'unavailable') return "You're offline. Changes will sync when you're back online.";
  return e?.message || 'Something went wrong.';
}
