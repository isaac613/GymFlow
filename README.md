# GymFlow

A workout tracking app for Android, built as the final project for Advanced Topics in App Innovation.

**Team:** Isaac Selig (345709729), Eli Englard (342724911)

## What it does

Sign in, pick a training category (Upper Body, Lower Body, Push, Pull, Core), work through its exercises with a demo photo for each one, and mark them complete as you go. The home screen greets you by name and shows a daily goal bar and a day streak. A progress dashboard summarises everything: overall completion, a per-category breakdown, and a feed of your recent activity. Every user has their own account, so progress and history are personal even on a shared device.

## Screens

1. **Login** - Google sign in, plus email/password sign up and sign in
2. **Home** - greeting, daily goal bar, day streak, camera profile photo, navigation
3. **Workout Categories** - image cards for each category, built live from the database
4. **Workout Detail** - the exercise list for a category, with completion, a rest timer, and add/delete
5. **Progress Dashboard** - overall progress ring, per-category breakdown, recent activity feed

## Tech

- Kotlin, XML layouts, AppCompat (Android Studio, minSdk 24, targetSdk 36)
- **Firebase Authentication** - Google sign in and email/password
- **Cloud Firestore** - all app data lives here, nothing is hard coded. The app seeds an empty database once on first run; after that the Firebase console is the source of truth, and live snapshot listeners push console edits to the phone instantly
- **Firebase Analytics + Crashlytics** - with custom events (login method, sign up, exercise and workout completions)
- **Camera** - runtime permission, MediaStore, and the ActivityResult API for the profile photo
- **Glide** - loads all images; exercise photos come from the [free-exercise-db](https://github.com/yuhonas/free-exercise-db) project
- When a user adds a custom exercise, the app searches that database on a background thread and attaches a matching demo photo automatically

## Running it

See [docs/setup.md](docs/setup.md). Short version: clone, open in Android Studio, and make sure your machine's debug SHA-1 fingerprint is registered in the Firebase console or Google sign in will fail.

## More documentation

- [docs/setup.md](docs/setup.md) - getting the project running on a new machine
- [docs/architecture.md](docs/architecture.md) - what each file does and how the screens fit together
- [docs/data-model.md](docs/data-model.md) - the Firestore collections and security rules
