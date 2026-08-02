# Architecture

All code lives in `app/src/main/java/com/example/gymflow/`. The app is five activities plus a shared repository object and one data class. Screens talk to Firestore through `WorkoutRepository` where the logic is shared, or directly where it is screen specific.

## Navigation flow

```
LoginActivity
    └─> MainActivity (home)
            ├─> WorkoutCategoriesActivity ──> WorkoutDetailActivity
            └─> ProgressActivity
```

Screens are connected with intents; the chosen category name is passed to the detail screen as an intent extra.

## Files

### Exercise.kt

The data model for one exercise: name, sets, reps, category, order, image URL, who added it, and completion state. Every field has a default value so Firestore can deserialise documents with `toObject()`. The `@DocumentId` annotation fills in the document id automatically, which the delete feature needs. The id parameter sits last so the seeding code can keep using positional arguments.

### WorkoutRepository.kt

A singleton object and the only place that knows the default workout plan.

- `seedDatabaseIfEmpty()` - uploads the default plan (5 categories, 20 exercises) in a batch write, but only when the `categories` collection is empty. This is what satisfies the "no static data" requirement without needing a separate admin tool.
- `listenToDailyTarget()` - live listener on `config/dailyTarget`; creates the document with defaults if it does not exist.
- `findExerciseImage()` - looks up a demo photo for a custom exercise. Runs the network request on a background thread with an `ExecutorService`, posts the result back with a `Handler` on the main looper, downloads the free-exercise-db index (873 exercises, cached after first fetch), and matches names in three passes: exact normalised match, substring match, then word overlap with plural awareness (so "Pec Fly" finds "Pec Flys").
- `isDefaultExercise()` - whether an exercise belongs to the built in plan; used to protect defaults from deletion.

### LoginActivity.kt

Entry point and gatekeeper. Google sign in uses `GoogleSignInOptions` with the web client id, gets an id token, and exchanges it for a Firebase credential. Email/password supports both sign in and sign up with validation and error messages. Both paths log an Analytics event (`login` with a method parameter, or `sign_up`).

### MainActivity.kt

Home screen. Redirects to login if no user is signed in. Shows a personalised greeting (falls back to the email prefix when the display name is blank, which is how Firebase reports email users), the day streak, and a daily goal bar driven by two live listeners: the target from `config/dailyTarget` and today's completions from the user's history. The profile photo flow requests the camera permission at point of use, inserts a MediaStore entry, launches the camera through `ActivityResultContracts.TakePicture`, and persists the resulting URI.

### WorkoutCategoriesActivity.kt

Builds a card per category from a live listener, using the first exercise photo in each category as the card background, with a count of exercises. Cards are built in code as `FrameLayout`s with a scrim overlay so the text stays readable.

### WorkoutDetailActivity.kt

The busiest screen. Two live listeners: the exercises for the category (a `whereEqualTo` query) and the user's progress document. Completing an exercise updates the progress array (`arrayUnion`), appends a history document with a server timestamp, logs an Analytics event, and starts a 60 second rest timer (`CountDownTimer`). Tapping again undoes it (`arrayRemove`). Users can add a custom exercise through a dialog (the app then fetches a matching photo in the background) and delete any non-default exercise after a confirmation dialog.

### ExerciseAdapter.kt

The RecyclerView adapter for the exercise list. Binds each card, loads the demo photo with Glide (with a dumbbell placeholder when there is no image), shows the delete icon only when the activity says the exercise is deletable, and reports taps back to the activity through callbacks.

### ProgressActivity.kt

The dashboard. Three live listeners (all exercises for the totals, the user's progress documents, and the five most recent history entries) feed an overall progress ring, a per category breakdown, and a recent activity feed. Percentages animate with a `ValueAnimator` when they change.

## Patterns worth knowing

- **Reads are asynchronous.** Firestore results arrive in callbacks, so every screen draws first and fills in when data lands; lists are refreshed from inside the callback.
- **Live listeners over one-time reads** on the main screens. This is why editing data in the Firebase console updates the running app instantly.
- **Listeners are cleaned up** - `ListenerRegistration`s are removed in `onDestroy` to avoid leaks and stray callbacks.
- **Per-user data is namespaced** under `users/{uid}` and locked down by the security rules (see [data-model.md](data-model.md)).
