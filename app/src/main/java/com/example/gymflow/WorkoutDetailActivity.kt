package com.example.gymflow

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

// Shows the exercises of one category and lets the user mark them complete.
// Exercises are loaded from Firestore and each user's progress is saved
// under their own account (users/{uid}/progress/{category}).
class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvProgressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBackCategories: Button

    private lateinit var exerciseAdapter: ExerciseAdapter
    private val exercises = mutableListOf<Exercise>()

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

        // Connect views from XML
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvProgressText = findViewById(R.id.tvProgressText)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewExercises)
        btnBackCategories = findViewById(R.id.btnBackCategories)

        // Read selected category from the previous screen
        category = intent.getStringExtra("category") ?: "Workout"
        tvWorkoutTitle.text = category

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        exerciseAdapter = ExerciseAdapter(exercises) { changedExercise ->
            // Every time a user toggles an exercise, update the screen
            // and save the new state to Firestore
            updateProgress()
            saveExerciseState(changedExercise)
        }
        recyclerView.adapter = exerciseAdapter

        btnBackCategories.setOnClickListener {
            finish()
        }

        // Mark this category as started for the progress dashboard
        markCategoryStarted()

        // Load the exercises and the user's saved progress from Firestore
        loadExercisesFromFirestore()
    }

    // Loads this category's exercises, then merges in the user's completion state
    private fun loadExercisesFromFirestore() {
        tvProgressText.text = "Loading exercises..."

        db.collection("exercises")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { snapshot ->
                // Convert documents into Exercise objects, keeping the plan's order
                val loaded = snapshot.documents
                    .mapNotNull { it.toObject(Exercise::class.java) }
                    .sortedBy { it.order }

                // Now fetch which exercises this user already completed
                progressDoc().get()
                    .addOnSuccessListener { progress ->
                        val completedNames =
                            progress.get("completedExercises") as? List<*> ?: emptyList<Any>()

                        for (exercise in loaded) {
                            exercise.isCompleted = completedNames.contains(exercise.name)
                        }

                        exercises.clear()
                        exercises.addAll(loaded)
                        exerciseAdapter.notifyDataSetChanged()
                        updateProgress()
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to load exercises", e)
                Toast.makeText(this, "Could not load exercises: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    // Updates the on-screen progress text and bar
    private fun updateProgress() {
        val completedCount = exercises.count { it.isCompleted }
        val totalCount = exercises.size

        tvProgressText.text = "Completed: $completedCount / $totalCount exercises"
        progressBar.max = totalCount
        progressBar.progress = completedCount
    }

    // Saves the completion state of a single exercise to Firestore
    private fun saveExerciseState(exercise: Exercise) {
        if (exercise.isCompleted) {
            // Add the exercise name to this user's completed list for the category
            progressDoc().set(
                mapOf("completedExercises" to FieldValue.arrayUnion(exercise.name)),
                SetOptions.merge()
            )

            // Remember the most recent completion for the progress dashboard
            db.collection("users").document(uid).set(
                mapOf(
                    "lastCompletedExercise" to exercise.name,
                    "lastCompletedCategory" to category
                ),
                SetOptions.merge()
            )
        } else {
            // Exercise was un-completed — remove it from the list
            progressDoc().set(
                mapOf("completedExercises" to FieldValue.arrayRemove(exercise.name)),
                SetOptions.merge()
            )
        }
    }

    // Marks this category as visited/started for the current user
    private fun markCategoryStarted() {
        progressDoc().set(mapOf("started" to true), SetOptions.merge())
    }

    // The Firestore document that holds this user's progress for this category
    private fun progressDoc() =
        db.collection("users").document(uid)
            .collection("progress").document(category)

    companion object {
        private const val TAG = "WorkoutDetailActivity"
    }
}
