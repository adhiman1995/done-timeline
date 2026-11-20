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
- **Security:** [firestore.rules](firestore.rules) only lets a signed-in user reach their own tasks, and only
  for the allowed email address. The Firebase config in the web code is public by design.
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
On Android debug builds: `adb shell am start -n com.donetimeline.app/.ui.MainActivity --ez demo true`.

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
- **Android release:** **Build → Generate Signed App Bundle / APK** in Android Studio. Add the release key's
  SHA-1 (and Google Play's app-signing SHA-1, if you publish there) to Firebase first.

## Backup and import

On the website, open the account menu (your avatar, top right):
- **Export tasks (JSON)** downloads all your tasks.
- **Import tasks (JSON)** uploads tasks from such a file. Tasks with the same id are overwritten.
