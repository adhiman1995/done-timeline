# Ticked

Everything you've finished, on one timeline — on the web and on Android.
There's no server to run: both apps sign in with Google and store tasks in **Firebase**.

```
┌──────────────────────┐                      ┌──────────────────────────┐
│ Web (React, static)  │ ── Firebase SDK ───▶ │ Firebase                 │
│ GitHub Pages         │    live updates      │  • Authentication (Google)│
└──────────────────────┘                      │  • Cloud Firestore        │
┌──────────────────────┐                      │    users/{uid}/tasks/{id} │
│ Android (Kotlin,     │ ── Firebase SDK ───▶ │                          │
│ Compose)             │  offline cache+sync  └──────────────────────────┘
└──────────────────────┘
```

| Path               | What                                                           |
| ------------------ | -------------------------------------------------------------- |
| `client/`          | React + Vite web app, published to GitHub Pages                |
| `android/`         | Native Android app (Kotlin, Jetpack Compose, Firestore)        |
| `client/public/icon.svg` | App icon (the Android launcher icon uses the same geometry) |
| `firestore.rules`  | Security rules — paste into the Firebase console               |
| `FIREBASE_SETUP.md`| One-time Firebase project setup                                |

Live site: https://adhiman1995.github.io/done-timeline/

---

## How it works

- **Sign-in:** Google, through Firebase Authentication. Credential Manager on Android, a popup on the web.
- **Storage:** each task is a Firestore document at `users/{uid}/tasks/{taskId}` with
  `title`, `notes`, `category`, `starred`, `completedAt`, `createdAt` and `updatedAt`.
- **Security:** [firestore.rules](firestore.rules) lets anyone sign in, but each user can only reach their own
  tasks, and writes are validated. The Firebase config in the web code is public by design.
- **Android offline:** Firestore keeps a copy on the phone. Changes save instantly, work offline and
  upload automatically when the network returns. Edits from the website appear live while the app is open.
- **Web:** listens for changes live and caches tasks in the browser, so repeat visits only download what changed.
- **Free:** everything fits in Firebase's free Spark plan.

## Local development

```bash
npm run install:all
npm run dev          # http://localhost:5173
```

Preview the UI without signing in: http://localhost:5173/?demo (sample data, development only).
On Android debug builds: `adb shell am start -n com.ticked.app/com.donetimeline.app.ui.MainActivity --ez demo true`.

`localhost` is an authorized domain in Firebase by default, so sign-in works locally.

### Android

Open `android/` in Android Studio and run the `app` configuration, or:

```bash
cd android && ./gradlew installDebug
```

Google sign-in needs the signing key's **SHA-1** registered in Firebase
(**Project settings → Your apps → Android → Add fingerprint**), then a fresh `google-services.json`
in `android/app/`. The debug key's fingerprint:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

The phone or emulator also needs a Google account (an emulator image with Google Play services).

## Deploying

- **Website:** every push to `main` that touches `client/` builds and publishes it via
  [.github/workflows/deploy-web.yml](.github/workflows/deploy-web.yml).
- **Security rules:** after editing `firestore.rules`, paste it into
  **Firebase console → Firestore Database → Rules → Publish**.
- **Android release (Google Play):** package `com.ticked.app`. Release builds are signed with the upload key in
  `android/release/` (git-ignored; keep a backup). Build the bundle with
  `cd android && ./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
  Store listing text, data-safety answers and graphics are in [android/store/](android/store/).
  Firebase needs the SHA-1 of the debug key, the upload key **and** Play's app-signing key
  (Play Console → Setup → App signing) for Google sign-in to work.

## Ticked AI (weekly review and standup)

AI features run through a small Cloudflare Worker in [ai-worker/](ai-worker/) so the **Groq API key never ships in
the apps**. The apps send the user's Firebase ID token plus the task lines; the Worker verifies the token, rate-limits
each user (10 requests per minute) and calls Groq (`GROQ_MODEL` in `wrangler.toml`, currently `openai/gpt-oss-120b`).

```bash
cd ai-worker
npm install
npx wrangler login                  # once, opens the browser
npx wrangler deploy                 # prints https://ticked-ai.<subdomain>.workers.dev
npx wrangler secret put GROQ_API_KEY
```

The Worker URL is set in `client/src/ai.js` (`AI_URL`) and `android/.../data/AiClient.kt` (`AI_URL`).

## Goals

Goals live at `users/{uid}/goals/{id}` as `{ category ('' = any task), target, period: day | week | month }`.
Progress is the number of tasks in the current day, week (from Monday) or month whose category matches.

## Privacy and account deletion

[client/public/privacy.html](client/public/privacy.html) is published at
https://adhiman1995.github.io/done-timeline/privacy.html. Users can delete their account and all tasks from the
Android Account screen or the website's account menu.

## Backup and import

On the website, open the account menu (your avatar, top right):
- **Export tasks (JSON)** downloads all your tasks.
- **Import tasks (JSON)** uploads tasks from such a file. Tasks with the same id are overwritten.
