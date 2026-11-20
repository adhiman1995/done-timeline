# Done Timeline

Log the tasks you've completed and see them on a timeline — on the web and on Android.

```
┌────────────────────┐        HTTPS + token        ┌──────────────────┐      ┌────────────┐
│ Web (React, static)│ ──── REST /api/tasks ─────▶ │  API (Node +     │ ───▶ │ PostgreSQL │
│  GitHub Pages      │                             │  Express)        │      │            │
└────────────────────┘                             │  e.g. Render     │      │ e.g. Neon  │
┌────────────────────┐                             │                  │      └────────────┘
│ Android (Kotlin,   │ ──── POST /api/sync ──────▶ │                  │
│ Compose, Room)     │   offline-first, 2-way      └──────────────────┘
└────────────────────┘
```

| Folder      | What                                                                 |
| ----------- | -------------------------------------------------------------------- |
| `client/`   | React + Vite web app, deployed to GitHub Pages                       |
| `server/`   | Node + Express API on PostgreSQL                                     |
| `android/`  | Native Android app (Kotlin, Jetpack Compose, Room, WorkManager)      |

---

## Local development

Requires Node 22+ and PostgreSQL binaries on your PATH (`initdb`, `pg_ctl`).

```bash
npm run install:all          # first time
cp server/.env.example server/.env   # then set API_TOKEN to a long random string
npm run dev                  # starts a private Postgres on :5433, the API on :3001, the web app on :5173
```

Open http://localhost:5173, leave **Server URL** empty and paste your `API_TOKEN`.

`npm run dev` runs its own Postgres instance stored in `.pgdata/` (port 5433), so it doesn't touch
any other database on your machine. Stop it with `npm run db:stop`.

### Android

Open the `android/` folder in Android Studio and run the `app` configuration, or:

```bash
cd android && ./gradlew installDebug
```

In the app: **Settings → Server URL** `http://10.0.2.2:3001` (the emulator's alias for your computer)
and the same access token. Debug builds allow plain HTTP for this; release builds require HTTPS.

---

## Deploying

### 1. Database — Neon (free)
Create a project at neon.tech and copy the connection string
(`postgres://…/neondb?sslmode=require`). Tables are created automatically on first start.

### 2. API — Render (free)
1. Push this repo to GitHub.
2. In Render: **New → Blueprint** → pick the repo. It reads `render.yaml`.
3. Set `DATABASE_URL` (from Neon) and `ALLOWED_ORIGINS` (`https://<your-username>.github.io`).
4. `API_TOKEN` is generated for you — copy it from the service's **Environment** tab.

Free Render services sleep after inactivity; the first request can take up to a minute.
The web app shows a "waking up" message and the Android app syncs in the background, so this is mostly invisible.

To move your existing local data: `DATABASE_URL=<neon url> npm run import-json --prefix server`
(imports `server/data/tasks.json`) — or point the Android app at the new server and it will upload its local tasks.

### 3. Web — GitHub Pages
1. Repo **Settings → Pages → Source: GitHub Actions**.
2. Repo **Settings → Secrets and variables → Actions → Variables**: add `API_URL` = your Render URL
   (e.g. `https://done-timeline-api.onrender.com`).
3. Push to `main`. `.github/workflows/deploy-web.yml` builds `client/` and publishes it.

The access token is **not** part of the build. You enter it once in the browser (plug icon in the header).

### 4. Android release
In Android Studio: **Build → Generate Signed App Bundle / APK**. Point the app at your Render URL in Settings.

---

## How sync works

- **Web** talks to the API directly (`GET/POST/PUT/DELETE /api/tasks`).
- **Android** is offline-first: every change is written to a local Room database and marked *dirty*.
  WorkManager pushes dirty rows to `POST /api/sync` as soon as there's a network, and pulls changes
  from other devices on app open, on pull-to-refresh and every ~15 minutes.
- Every write on the server gets a new value from a sequence (`rev`). A device sends its last-seen
  `rev` as a cursor and receives everything newer — including deletes, which are kept as tombstones.
- Conflicts resolve **last write wins** by `updatedAt`. Client timestamps are capped at the server's
  clock so a phone with a wrong clock can't override newer edits forever.

## API

All endpoints except `/api/health` need `Authorization: Bearer <API_TOKEN>`.

| Method | Path              | Body / notes                                                        |
| ------ | ----------------- | ------------------------------------------------------------------- |
| GET    | /api/health       | `{ ok: true }` — no auth                                            |
| GET    | /api/auth/check   | 200 if the token is valid                                           |
| GET    | /api/tasks        | All non-deleted tasks                                               |
| POST   | /api/tasks        | `{ title, completedAt, notes?, category?, starred?, id? }`          |
| PUT    | /api/tasks/:id    | Any of the fields above                                             |
| DELETE | /api/tasks/:id    | Soft delete                                                         |
| POST   | /api/sync         | `{ since, changes: Task[] }` → `{ cursor, changes: Task[] }`        |
