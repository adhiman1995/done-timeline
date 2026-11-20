# Firebase setup

Everything here is on the free **Spark** plan. No credit card is needed.
Do not upgrade to Blaze, because nothing in this app requires it.

Console: https://console.firebase.google.com

---

## 1. Create the project

1. Click **Create a project** (or **Add project**).
2. Name it `Done Timeline`, then click **Continue**.
3. Turn **Google Analytics** off. The app doesn't need it.
4. Click **Create project**, wait for it to finish, then click **Continue**.

## 2. Turn on Google sign-in

1. In the left menu, open **Build → Authentication → Get started**.
2. On the **Sign-in method** tab, click **Google** and switch on **Enable**.
3. Choose your email as the **Project support email**, then click **Save**.
4. Open **Settings → Authorized domains → Add domain** and add `<your-github-username>.github.io`.
   `localhost` is already in the list, so local development works too.

## 3. Create the database

1. Open **Build → Firestore Database → Create database**.
2. Choose the **Standard** edition.
3. For **Location**, pick the region closest to you, for example `asia-south1 (Mumbai)`.
   **You can't change this later.**
4. Choose **Start in production mode**, then click **Create**.

## 4. Set the security rules

1. Open the **Rules** tab of Firestore.
2. Replace everything there with the rules below, putting your Gmail address in place of the placeholder.
3. Click **Publish**.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Each user can only reach their own tasks, and only the allowed account can sign in.
    match /users/{uid}/tasks/{taskId} {
      allow read, write: if request.auth != null
        && request.auth.uid == uid
        && request.auth.token.email_verified == true
        && request.auth.token.email == "YOUR_EMAIL@gmail.com";
    }
  }
}
```

The rules are what protect your data. The Firebase settings inside the website are public by design.

## 5. Register the website

1. Go to **Project Overview** (the home icon) and click the **Web** icon `</>`.
2. Use the nickname `Done Timeline Web`.
3. Leave **Firebase Hosting** unticked, because the site is hosted on GitHub Pages.
4. Click **Register app**.
5. Copy the `firebaseConfig` object it shows. It looks like this:

   ```js
   const firebaseConfig = {
     apiKey: "…",
     authDomain: "….firebaseapp.com",
     projectId: "…",
     storageBucket: "….firebasestorage.app",
     messagingSenderId: "…",
     appId: "…"
   };
   ```

   These values are safe to share and commit.
6. Click **Continue to console**. You can skip the SDK instructions; I'll do the code.

## 6. Register the Android app

1. Go to **Project Overview → Add app** and choose the **Android** icon.
2. Fill in the form:
   - **Package name:** `com.donetimeline.app`
   - **Nickname:** `Done Timeline Android`
   - **Debug signing certificate SHA-1** (the debug key on this Mac):
     ```
     A1:54:39:25:EE:87:BE:03:77:18:5A:3F:13:17:F5:6F:E7:20:5B:8C
     ```
3. Click **Register app**.
4. Download **`google-services.json`** and save it as `android/app/google-services.json`.
5. Skip the remaining SDK steps.

> **Release builds:** the SHA-1 above only works for debug builds. Later, when you create a release keystore, add that
> key's SHA-1 and SHA-256 under **Project settings → Your apps → Android → Add fingerprint**. If you publish through
> Google Play, also add the SHA-1 from **Play Console → App integrity**.
> After adding fingerprints, **download `google-services.json` again** and replace the old file.
>
> To print a keystore's fingerprints:
> `keytool -list -v -keystore <path-to-keystore> -alias <alias>`

## 7. Check before handing over

- [ ] Google sign-in is enabled, and your `github.io` domain is authorized.
- [ ] The Firestore database exists and the rules are published with your email.
- [ ] You have the web `firebaseConfig` object.
- [ ] `android/app/google-services.json` is in place.

Then send me the `firebaseConfig` and I'll switch the website and the Android app to Firebase, and import your
existing tasks.
