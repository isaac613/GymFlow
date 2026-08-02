# Setup

How to get GymFlow running on a new machine.

## Prerequisites

- Android Studio (any recent version)
- An Android device or emulator running API 24 or higher

## Steps

1. **Clone the repository** and open the project folder in Android Studio.

2. **Let Android Studio create `local.properties`.** This file points at your local Android SDK and is intentionally not committed (it is machine specific). Android Studio generates it automatically on first sync. If it does not, create it in the project root with a single line:

   ```
   sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```

3. **Register your debug SHA-1 in Firebase** (required for Google sign in). Every machine signs debug builds with its own key, so each teammate's fingerprint must be added:

   - Get your fingerprint:

     ```
     keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```

     (`keytool` ships with Android Studio under `jbr/bin` if it is not on your PATH.)

   - In the [Firebase console](https://console.firebase.google.com/project/gymflow-7df94), open Project settings, find the Android app, and add the SHA-1 under "SHA certificate fingerprints".
   - Download the refreshed `google-services.json` and replace `app/google-services.json` if prompted. Usually the existing file keeps working once the fingerprint is added.

   Without this step, email/password sign in still works but Google sign in fails with a generic error.

4. **Run the app.** On first launch against an empty database the app seeds the default workout plan into Firestore automatically. This only happens when the `categories` collection is empty, so it never duplicates data.

## Firestore rules

The security rules live in [`firestore.rules`](../firestore.rules) at the repository root. They are published through the Firebase console (Firestore Database, then Rules, then paste and Publish). If you add a new collection, remember that Firestore denies anything the rules do not explicitly allow, so the rules must be updated first.

## Test account

For quick testing without a Google account: `test@gymflow.com` / `test123` (email/password sign in).

## Known limitations

- Profile photos taken with the camera are stored on the device, not in the cloud, so they do not follow the user across phones. Firebase Storage would fix this but it is a paid service the course does not use.
- The exercises collection is shared: a custom exercise added by one user is visible to all users, the same model as the shared contacts app from class.
