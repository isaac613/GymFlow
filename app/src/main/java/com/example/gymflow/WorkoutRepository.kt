package com.example.gymflow

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.Executors

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
    // yet. Known seed exercises use the local map; anything else (e.g. custom
    // exercises added by users) is looked up in the online exercise database.
    // Safe to call on every app start: it only writes when something is missing.
    fun ensureExerciseImages() {
        db.collection("exercises").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    if (!doc.getString("imageUrl").isNullOrEmpty()) continue

                    val name = doc.getString("name") ?: continue
                    val knownUrl = exerciseImages[name]

                    if (knownUrl != null) {
                        doc.reference.update("imageUrl", knownUrl)
                    } else {
                        // Not a seed exercise — search the online database
                        findExerciseImage(name) { foundUrl ->
                            if (foundUrl != null) {
                                doc.reference.update("imageUrl", foundUrl)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e -> Log.w(TAG, "Could not check exercise images", e) }
    }

    // --- Online exercise image search -------------------------------------
    // free-exercise-db publishes an index of ~870 exercises as one JSON file.
    // We download it once on a background thread (the web API technique from
    // class: ExecutorService + Handler), cache it in memory, and match
    // exercise names against it to find a demo photo for custom exercises.

    private const val EXERCISE_DB_INDEX =
        "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // exercise name -> first image URL, filled on the first lookup
    private var onlineImageIndex: Map<String, String>? = null

    // Searches the online database for an image matching the exercise name.
    // Runs on a background thread; the callback is posted to the main thread.
    fun findExerciseImage(exerciseName: String, onResult: (String?) -> Unit) {
        executor.execute {
            try {
                val index = onlineImageIndex
                    ?: downloadImageIndex().also { onlineImageIndex = it }
                val url = matchImage(index, exerciseName)
                mainHandler.post { onResult(url) }
            } catch (e: Exception) {
                Log.w(TAG, "Online image lookup failed", e)
                mainHandler.post { onResult(null) }
            }
        }
    }

    // Downloads and parses the exercise index (runs on the background thread)
    private fun downloadImageIndex(): Map<String, String> {
        val reader = BufferedReader(InputStreamReader(URL(EXERCISE_DB_INDEX).openStream()))
        val json = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            json.append(line)
        }
        reader.close()

        val result = mutableMapOf<String, String>()
        val array = JSONArray(json.toString())
        for (i in 0 until array.length()) {
            val exercise = array.getJSONObject(i)
            val images = exercise.optJSONArray("images")
            if (images != null && images.length() > 0) {
                result[exercise.getString("name")] = "$IMAGE_BASE/${images.getString(0)}"
            }
        }
        Log.d(TAG, "Downloaded online exercise index (${result.size} entries)")
        return result
    }

    // Lowercases and strips everything except letters/digits, so that
    // "Push Ups" matches "Pushups" and "push-ups" alike
    private fun normalize(text: String) = text.lowercase().filter { it.isLetterOrDigit() }

    private val stopWords = setOf("the", "with", "and", "for", "your", "from")

    // Splits a name into meaningful lowercase words (3+ letters, no stop words)
    private fun words(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in stopWords }

    // True if two words are the same, or one is the simple plural of the other
    // ("fly"/"flyes", "curl"/"curls"), so word forms still match
    private fun wordsMatch(a: String, b: String): Boolean {
        if (a == b) return true
        return b == a + "s" || b == a + "es" || a == b + "s" || a == b + "es"
    }

    // Finds the best matching exercise image in three passes, most precise first:
    //   1. exact name match (ignoring spaces/case)
    //   2. one name fully contains the other ("Cable Fly" -> "Incline Cable Flye")
    //   3. shared words ("Pec Fly" -> "Dumbbell Flyes" via the word "fly")
    // Returns null when nothing meaningful matches, so the card shows the
    // dumbbell placeholder instead of an unrelated image.
    private fun matchImage(index: Map<String, String>, query: String): String? {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return null

        // 1. Exact match
        index.entries.firstOrNull { normalize(it.key) == normalizedQuery }
            ?.let { return it.value }

        // 2. One contains the other
        index.entries
            .filter {
                val name = normalize(it.key)
                name.contains(normalizedQuery) || normalizedQuery.contains(name)
            }
            .minByOrNull { it.key.length }
            ?.let { return it.value }

        // 3. Word overlap: score each candidate by how many query words it
        // shares, preferring more shared words then the shortest/closest name
        val queryWords = words(query)
        if (queryWords.isEmpty()) return null

        return index.entries
            .map { entry ->
                val candidateWords = words(entry.key)
                val score = queryWords.count { q -> candidateWords.any { wordsMatch(q, it) } }
                Triple(score, entry.key.length, entry.value)
            }
            .filter { it.first > 0 }
            .minWithOrNull(
                // highest score first, then shortest name
                compareByDescending<Triple<Int, Int, String>> { it.first }.thenBy { it.second }
            )
            ?.third
    }

    // True when this exercise is part of the built-in workout plan. Used to
    // decide whether it can be deleted — the defaults are protected, any
    // user-added exercise can be removed.
    fun isDefaultExercise(name: String, category: String): Boolean {
        return defaultPlan[category]?.any { it.name == name } == true
    }

    // Watches the daily target document with a real-time listener and hands
    // its text and goal to the caller on every change. Everything lives in
    // Firestore (config/dailyTarget), so editing it in the Firebase console
    // updates the home screen instantly — no app release needed. Returns the
    // listener registration so the caller can remove it when its screen closes.
    fun listenToDailyTarget(
        onLoaded: (title: String, subtitle: String, goal: Int) -> Unit
    ): ListenerRegistration {
        val defaultTitle = "🎯 TODAY'S TARGET"
        val defaultSubtitle = "Complete one set of exercises"
        val defaultGoal = 5L

        val targetDoc = db.collection("config").document("dailyTarget")
        return targetDoc.addSnapshotListener { doc, error ->
            if (error != null) {
                Log.w(TAG, "Daily target listener failed", error)
                return@addSnapshotListener
            }
            if (doc != null && doc.exists()) {
                onLoaded(
                    doc.getString("title") ?: defaultTitle,
                    doc.getString("subtitle") ?: defaultSubtitle,
                    (doc.getLong("goal") ?: defaultGoal).toInt()
                )
                // Older documents were created before the goal existed —
                // add it so it can be edited in the console
                if (doc.getLong("goal") == null) {
                    targetDoc.update("goal", defaultGoal)
                }
            } else if (doc != null) {
                // First run against this database — create the document with
                // defaults; the listener fires again once the write lands
                targetDoc.set(
                    mapOf(
                        "title" to defaultTitle,
                        "subtitle" to defaultSubtitle,
                        "goal" to defaultGoal
                    )
                )
                onLoaded(defaultTitle, defaultSubtitle, defaultGoal.toInt())
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
