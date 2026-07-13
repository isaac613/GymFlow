package com.example.gymflow

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase

// Shows the exercises of one category and lets the user mark them complete.
// Uses real-time Firestore snapshot listeners, so any change to the plan or
// the user's progress (even edits made in the Firebase console) appears on
// screen immediately without reloading the page.
class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvProgressText: TextView
    private lateinit var tvRestTimer: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var pbLoadingExercises: ProgressBar
    private lateinit var recyclerView: RecyclerView

    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    // The list shown on screen, rebuilt from the two live snapshots below
    private val exercises = mutableListOf<Exercise>()
    private var planExercises = listOf<Exercise>()   // latest workout plan
    private var completedNames = listOf<String>()    // latest user progress
    private var planLoaded = false

    // Live listeners are kept so they can be removed when the screen closes
    private val listeners = mutableListOf<ListenerRegistration>()
    private var restTimer: CountDownTimer? = null

    private lateinit var category: String
    private lateinit var uid: String

    private val db get() = WorkoutRepository.db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)

        // A signed-in user is required — progress is stored per user
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finish()
            return
        }
        uid = user.uid

        firebaseAnalytics = Firebase.analytics

        // Connect views from XML
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvProgressText = findViewById(R.id.tvProgressText)
        tvRestTimer = findViewById(R.id.tvRestTimer)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        pbLoadingExercises = findViewById(R.id.pbLoadingExercises)
        recyclerView = findViewById(R.id.recyclerViewExercises)

        // Read selected category from the previous screen
        category = intent.getStringExtra("category") ?: "Workout"
        tvWorkoutTitle.text = category

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        exerciseAdapter = ExerciseAdapter(exercises) { changedExercise ->
            onExerciseToggled(changedExercise)
        }
        recyclerView.adapter = exerciseAdapter

        findViewById<Button>(R.id.btnBackCategories).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnAddExercise).setOnClickListener {
            showAddExerciseDialog()
        }

        findViewById<Button>(R.id.btnResetWorkout).setOnClickListener {
            confirmResetWorkout()
        }

        // Mark this category as started for the progress dashboard
        markCategoryStarted()

        // Start the two live listeners: the workout plan and the user's progress
        listenToExercises()
        listenToProgress()
    }

    // Live listener on this category's exercises. Fires immediately with the
    // current data and again on every change (e.g. an exercise added from the
    // app or edited in the Firebase console).
    private fun listenToExercises() {
        val registration = db.collection("exercises")
            .whereEqualTo("category", category)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Exercises listener failed", error)
                    Toast.makeText(this, "Could not load exercises.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    planExercises = snapshot.documents
                        .mapNotNull { it.toObject(Exercise::class.java) }
                        .sortedBy { it.order }
                    planLoaded = true
                    renderExercises()
                }
            }
        listeners.add(registration)
    }

    // Live listener on the user's progress document for this category
    private fun listenToProgress() {
        val registration = progressDoc().addSnapshotListener { doc, error ->
            if (error != null) {
                Log.w(TAG, "Progress listener failed", error)
                return@addSnapshotListener
            }
            completedNames =
                (doc?.get("completedExercises") as? List<*>)?.filterIsInstance<String>()
                    ?: emptyList()
            renderExercises()
        }
        listeners.add(registration)
    }

    // Merges the plan with the user's completion state and redraws the list
    private fun renderExercises() {
        if (!planLoaded) return

        val merged = planExercises.map { exercise ->
            exercise.copy(isCompleted = completedNames.contains(exercise.name))
        }

        exercises.clear()
        exercises.addAll(merged)
        exerciseAdapter.notifyDataSetChanged()
        updateProgress()

        // Loading finished — show either the list or the empty state
        pbLoadingExercises.visibility = View.GONE
        tvEmptyState.visibility = if (merged.isEmpty()) View.VISIBLE else View.GONE
    }

    // Updates the on-screen progress text and bar
    private fun updateProgress() {
        val completedCount = exercises.count { it.isCompleted }
        val totalCount = exercises.size

        tvProgressText.text = "Completed: $completedCount / $totalCount exercises"
        progressBar.max = if (totalCount > 0) totalCount else 1
        progressBar.progress = completedCount
    }

    // Called when the user taps an exercise's complete/undo button
    private fun onExerciseToggled(exercise: Exercise) {
        updateProgress()

        if (exercise.isCompleted) {
            // Save the completion to Firestore
            progressDoc().set(
                mapOf("completedExercises" to FieldValue.arrayUnion(exercise.name)),
                SetOptions.merge()
            )

            // Remember the most recent completion for the dashboard
            db.collection("users").document(uid).set(
                mapOf(
                    "lastCompletedExercise" to exercise.name,
                    "lastCompletedCategory" to category
                ),
                SetOptions.merge()
            )

            // Append to the user's workout history (feeds the activity
            // feed on the dashboard and the streak on the home screen)
            db.collection("users").document(uid).collection("history").add(
                mapOf(
                    "exercise" to exercise.name,
                    "category" to category,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            )

            // Analytics: one event per completed exercise
            firebaseAnalytics.logEvent("exercise_completed") {
                param("exercise_name", exercise.name)
                param("category", category)
            }

            // Start the rest countdown before the next exercise
            startRestTimer()

            // If that was the last one, the whole workout is complete
            if (exercises.isNotEmpty() && exercises.all { it.isCompleted }) {
                firebaseAnalytics.logEvent("workout_completed") {
                    param("category", category)
                }
                Toast.makeText(this, "Workout complete! 💪", Toast.LENGTH_LONG).show()
            }
        } else {
            // Exercise was un-completed — remove it from the saved list
            progressDoc().set(
                mapOf("completedExercises" to FieldValue.arrayRemove(exercise.name)),
                SetOptions.merge()
            )
        }
    }

    // Shows a 60 second rest countdown in the progress panel
    private fun startRestTimer() {
        restTimer?.cancel()
        tvRestTimer.visibility = View.VISIBLE

        restTimer = object : CountDownTimer(REST_SECONDS * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                tvRestTimer.text = "⏱ Rest: ${secondsLeft}s"
            }

            override fun onFinish() {
                tvRestTimer.text = "✅ Rest over — go again!"
                // Hide the message a moment later
                tvRestTimer.postDelayed({ tvRestTimer.visibility = View.GONE }, 3000L)
            }
        }.start()
    }

    // Dialog for adding a custom exercise to this category. The new exercise
    // is written to Firestore and appears instantly via the snapshot listener.
    private fun showAddExerciseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_exercise, null)
        val etName = dialogView.findViewById<EditText>(R.id.etExerciseName)
        val etSets = dialogView.findViewById<EditText>(R.id.etExerciseSets)
        val etReps = dialogView.findViewById<EditText>(R.id.etExerciseReps)

        AlertDialog.Builder(this)
            .setTitle("Add exercise to $category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val sets = etSets.text.toString().toIntOrNull()
                val reps = etReps.text.toString().toIntOrNull()

                if (name.isEmpty() || sets == null || reps == null) {
                    Toast.makeText(
                        this, "Please fill in a name, sets and reps.", Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                db.collection("exercises").add(
                    mapOf(
                        "name" to name,
                        "sets" to sets,
                        "reps" to reps,
                        "category" to category,
                        "order" to planExercises.size,
                        "imageUrl" to ""
                    )
                ).addOnSuccessListener {
                    Toast.makeText(this, "\"$name\" added!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Could not add: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Asks for confirmation, then clears this category's progress
    private fun confirmResetWorkout() {
        AlertDialog.Builder(this)
            .setTitle("Reset workout?")
            .setMessage("This clears your completed exercises for $category.")
            .setPositiveButton("Reset") { _, _ ->
                progressDoc().set(
                    mapOf("completedExercises" to emptyList<String>(), "started" to true)
                )
                Toast.makeText(this, "Workout reset.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Marks this category as visited/started for the current user
    private fun markCategoryStarted() {
        progressDoc().set(mapOf("started" to true), SetOptions.merge())
    }

    // The Firestore document that holds this user's progress for this category
    private fun progressDoc() =
        db.collection("users").document(uid)
            .collection("progress").document(category)

    override fun onDestroy() {
        super.onDestroy()
        // Stop the live listeners and the timer when the screen closes
        listeners.forEach { it.remove() }
        restTimer?.cancel()
    }

    companion object {
        private const val TAG = "WorkoutDetailActivity"
        private const val REST_SECONDS = 60
    }
}
