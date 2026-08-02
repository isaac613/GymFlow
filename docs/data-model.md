# Firestore data model

Project: `gymflow-7df94`. All app data lives in Firestore; nothing is hard coded in the app beyond the one-time seed.

## Collections

### categories (shared)

One document per workout category.

| Field | Type | Notes |
|-------|------|-------|
| name  | string | e.g. "Upper Body" |
| order | number | display order |

### exercises (shared)

One document per exercise, across all categories.

| Field | Type | Notes |
|-------|------|-------|
| name | string | e.g. "Push-ups" |
| sets | number | |
| reps | string | e.g. "10-12" |
| category | string | matches a category name; screens query with `whereEqualTo` |
| order | number | display order within the category |
| imageUrl | string | demo photo URL from free-exercise-db; empty shows a placeholder |
| addedBy | string | uid of the user who added it; empty for the default plan |

### config (shared)

App configuration edited from the console.

- `config/dailyTarget`: `title` (string), `subtitle` (string), `goal` (number) - drives the home screen hero card and goal bar. Change it in the console and the phone updates live.

### users/{uid} (per user)

The user's own document.

| Field | Type |
|-------|------|
| lastCompletedExercise | string |
| lastCompletedCategory | string |
| customPhotoUri | string |

### users/{uid}/progress/{category}

One document per category the user has touched.

| Field | Type | Notes |
|-------|------|-------|
| completedExercises | array of strings | exercise names, updated with `arrayUnion` / `arrayRemove` |
| started | boolean | |

### users/{uid}/history/{autoId}

One document per completion, powering the streak, the daily goal count, and the dashboard feed.

| Field | Type | Notes |
|-------|------|-------|
| exercise | string | |
| category | string | |
| completedAt | timestamp | `FieldValue.serverTimestamp()` |

## Seeding

`WorkoutRepository.seedDatabaseIfEmpty()` runs on app start. If the `categories` collection is empty it uploads the default plan (5 categories, 20 exercises) in a single batch write; otherwise it does nothing. This means a fresh Firebase project bootstraps itself on first launch, and the console can then edit or extend everything without touching code.

## Security rules

The rules are in [`firestore.rules`](../firestore.rules) and published in the Firebase console. In short:

- Every request requires a signed in user (`request.auth != null`).
- The shared collections (`categories`, `exercises`, `config`) are readable and writable by any signed in user. Writes are open so the app can seed an empty database and so users can add custom exercises.
- Everything under `users/{uid}` is locked to that user: `request.auth.uid == userId`. One user can never read another user's progress or history.
- Anything not matched by a rule is denied. This bit us once: adding the `config` collection without updating the rules produced PERMISSION_DENIED until the rules grew with the app.
