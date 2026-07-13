package com.example.gymflow

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Central helper for Firestore access and one-time database seeding.
// All workout data (categories + exercises) lives in Firestore — nothing is static.
object WorkoutRepository {

    private const val TAG = "WorkoutRepository"

    val db: FirebaseFirestore
        get() = Firebase.firestore

    // The default workout plan. This is ONLY used to seed Firestore the very
    // first time the app runs against an empty database. After seeding,
    // Firestore is the single source of truth and this list is never read again —
    // the plan can be changed freely from the Firebase console.
    private val defaultPlan = linkedMapOf(
        "Upper Body" to listOf(
            Exercise("Bench Press", 4, 10),
            Exercise("Pull Ups", 3, 8),
            Exercise("Shoulder Press", 3, 10),
            Exercise("Bicep Curls", 3, 12)
        ),
        "Lower Body" to listOf(
            Exercise("Squats", 4, 12),
            Exercise("Leg Press", 3, 10),
            Exercise("Romanian Deadlift", 3, 8),
            Exercise("Calf Raises", 4, 15)
        ),
        "Push" to listOf(
            Exercise("Bench Press", 4, 10),
            Exercise("Incline Dumbbell Press", 3, 10),
            Exercise("Shoulder Press", 3, 10),
            Exercise("Tricep Pushdown", 3, 12)
        ),
        "Pull" to listOf(
            Exercise("Deadlift", 3, 6),
            Exercise("Lat Pulldown", 3, 10),
            Exercise("Seated Row", 3, 12),
            Exercise("Hammer Curl", 3, 12)
        ),
        "Core" to listOf(
            Exercise("Plank", 3, 60),
            Exercise("Leg Raises", 3, 15),
            Exercise("Russian Twists", 3, 20),
            Exercise("Cable Crunch", 3, 15)
        )
    )

    // Demo image for each exercise, served from the free-exercise-db project
    // (MIT licensed). Stored in Firestore per exercise, loaded with Glide.
    private const val IMAGE_BASE =
        "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"

    private val exerciseImages = mapOf(
        "Bench Press" to "$IMAGE_BASE/Barbell_Bench_Press_-_Medium_Grip/0.jpg",
        "Pull Ups" to "$IMAGE_BASE/Pullups/0.jpg",
        "Shoulder Press" to "$IMAGE_BASE/Barbell_Shoulder_Press/0.jpg",
        "Bicep Curls" to "$IMAGE_BASE/Barbell_Curl/0.jpg",
        "Squats" to "$IMAGE_BASE/Barbell_Squat/0.jpg",
        "Leg Press" to "$IMAGE_BASE/Leg_Press/0.jpg",
        "Romanian Deadlift" to "$IMAGE_BASE/Romanian_Deadlift/0.jpg",
        "Calf Raises" to "$IMAGE_BASE/Standing_Calf_Raises/0.jpg",
        "Incline Dumbbell Press" to "$IMAGE_BASE/Incline_Dumbbell_Press/0.jpg",
        "Tricep Pushdown" to "$IMAGE_BASE/Triceps_Pushdown/0.jpg",
        "Deadlift" to "$IMAGE_BASE/Barbell_Deadlift/0.jpg",
        "Lat Pulldown" to "$IMAGE_BASE/Wide-Grip_Lat_Pulldown/0.jpg",
        "Seated Row" to "$IMAGE_BASE/Seated_Cable_Rows/0.jpg",
        "Hammer Curl" to "$IMAGE_BASE/Hammer_Curls/0.jpg",
        "Plank" to "$IMAGE_BASE/Plank/0.jpg",
        "Leg Raises" to "$IMAGE_BASE/Flat_Bench_Lying_Leg_Raise/0.jpg",
        "Russian Twists" to "$IMAGE_BASE/Russian_Twist/0.jpg",
        "Cable Crunch" to "$IMAGE_BASE/Cable_Crunch/0.jpg"
    )

    // Adds the imageUrl field to any exercise document that doesn't have one
    // yet. Safe to call on every app start: it only writes when something is
    // missing, so already-migrated databases are untouched.
    fun ensureExerciseImages() {
        db.collection("exercises").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                var updates = 0

                for (doc in snapshot.documents) {
                    if (doc.getString("imageUrl").isNullOrEmpty()) {
                        val url = exerciseImages[doc.getString("name")]
                        if (url != null) {
                            batch.update(doc.reference, "imageUrl", url)
                            updates++
                        }
                    }
                }

                if (updates > 0) {
                    batch.commit()
                    Log.d(TAG, "Added images to $updates exercises")
                }
            }
            .addOnFailureListener { e -> Log.w(TAG, "Could not check exercise images", e) }
    }

    // Watches the daily target document with a real-time listener and hands
    // its text to the caller on every change. The text lives in Firestore
    // (config/dailyTarget), so editing it in the Firebase console updates the
    // home screen instantly — no app release needed. Returns the listener
    // registration so the caller can remove it when its screen closes.
    fun listenToDailyTarget(
        onLoaded: (title: String, subtitle: String) -> Unit
    ): ListenerRegistration {
        val defaultTitle = "🎯  Today's Target"
        val defaultSubtitle = "Complete one set of exercises"

        val targetDoc = db.collection("config").document("dailyTarget")
        return targetDoc.addSnapshotListener { doc, error ->
            if (error != null) {
                Log.w(TAG, "Daily target listener failed", error)
                return@addSnapshotListener
            }
            if (doc != null && doc.exists()) {
                onLoaded(
                    doc.getString("title") ?: defaultTitle,
                    doc.getString("subtitle") ?: defaultSubtitle
                )
            } else if (doc != null) {
                // First run against this database — create the document with
                // defaults; the listener fires again once the write lands
                targetDoc.set(mapOf("title" to defaultTitle, "subtitle" to defaultSubtitle))
                onLoaded(defaultTitle, defaultSubtitle)
            }
        }
    }

    // Uploads the default plan to Firestore, but only if the database is empty.
    // Uses a single batch write so seeding is all-or-nothing.
    fun seedDatabaseIfEmpty() {
        db.collection("categories").get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    // Database already has data — nothing to do
                    return@addOnSuccessListener
                }

                Log.d(TAG, "Empty database detected — seeding default workout plan")
                val batch = db.batch()

                defaultPlan.entries.forEachIndexed { categoryIndex, (categoryName, exercises) ->
                    // One document per category (used by the categories screen)
                    val categoryRef = db.collection("categories").document()
                    batch.set(
                        categoryRef, mapOf(
                            "name" to categoryName,
                            "order" to categoryIndex
                        )
                    )

                    // One document per exercise (used by the workout detail screen)
                    exercises.forEachIndexed { exerciseIndex, exercise ->
                        val exerciseRef = db.collection("exercises").document()
                        batch.set(
                            exerciseRef, mapOf(
                                "name" to exercise.name,
                                "sets" to exercise.sets,
                                "reps" to exercise.reps,
                                "category" to categoryName,
                                "order" to exerciseIndex,
                                "imageUrl" to (exerciseImages[exercise.name] ?: "")
                            )
                        )
                    }
                }

                batch.commit()
                    .addOnSuccessListener { Log.d(TAG, "Seeding complete") }
                    .addOnFailureListener { e -> Log.w(TAG, "Seeding failed", e) }
            }
            .addOnFailureListener { e -> Log.w(TAG, "Could not check database state", e) }
    }
}
