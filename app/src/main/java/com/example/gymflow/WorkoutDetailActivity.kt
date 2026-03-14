package com.example.gymflow

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvProgressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBackCategories: Button

    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var exercises: MutableList<Exercise>

    private lateinit var category: String

    // SharedPreferences is used to store lightweight local app data
    private val prefs by lazy {
        getSharedPreferences("gymflow_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)

        // Connect views from XML
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvProgressText = findViewById(R.id.tvProgressText)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewExercises)
        btnBackCategories = findViewById(R.id.btnBackCategories)

        // Read selected category from the previous screen
        category = intent.getStringExtra("category") ?: "Workout"
        tvWorkoutTitle.text = category

        // Load exercises for this category
        exercises = getExercisesForCategory(category).toMutableList()

        // Restore saved completion state for each exercise
        restoreExerciseStates()

        // Mark this category as visited/started
        markCategoryStarted(category)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        exerciseAdapter = ExerciseAdapter(exercises) { changedExercise ->
            // Every time a user toggles an exercise, update screen progress
            updateProgress()

            // Save this exercise state locally
            saveExerciseState(category, changedExercise)

            // Recalculate dashboard data
            saveDashboardStats(changedExercise)
        }

        recyclerView.adapter = exerciseAdapter

        btnBackCategories.setOnClickListener {
            finish()
        }

        updateProgress()
    }

    // Updates the local screen progress text and bar
    private fun updateProgress() {
        val completedCount = exercises.count { it.isCompleted }
        val totalCount = exercises.size

        tvProgressText.text = "Completed: $completedCount / $totalCount exercises"
        progressBar.max = totalCount
        progressBar.progress = completedCount
    }

    // Saves the completion state of a single exercise
    private fun saveExerciseState(category: String, exercise: Exercise) {
        prefs.edit()
            .putBoolean("exercise_${category}_${exercise.name}", exercise.isCompleted)
            .apply()
    }

    // Restores saved completion state when reopening a category
    private fun restoreExerciseStates() {
        for (exercise in exercises) {
            exercise.isCompleted =
                prefs.getBoolean("exercise_${category}_${exercise.name}", false)
        }
    }

    // Saves global dashboard statistics used by ProgressActivity
    private fun saveDashboardStats(changedExercise: Exercise) {
        val editor = prefs.edit()

        val totalExercisesCompleted = getTotalCompletedExercises()
        editor.putInt("total_exercises_completed", totalExercisesCompleted)

        // Save the most recent completed exercise only when toggled on
        if (changedExercise.isCompleted) {
            editor.putString("last_completed_exercise", changedExercise.name)
            editor.putString("last_completed_category", category)
        }

        // Save per-category completed count
        val completedInCategory = exercises.count { it.isCompleted }
        editor.putInt("completed_$category", completedInCategory)
        editor.putInt("total_$category", exercises.size)

        // Count how many categories were started at least once
        editor.putInt("categories_started_count", getStartedCategoriesCount())

        // A category is considered fully finished if all its exercises are completed
        editor.putInt("workouts_finished_count", getFinishedWorkoutsCount())

        editor.apply()
    }

    // Marks category as started when user enters it
    private fun markCategoryStarted(category: String) {
        prefs.edit()
            .putBoolean("started_$category", true)
            .apply()

        prefs.edit()
            .putInt("categories_started_count", getStartedCategoriesCount())
            .apply()
    }

    // Counts all completed exercises across all categories
    private fun getTotalCompletedExercises(): Int {
        val categories = listOf("Upper Body", "Lower Body", "Push", "Pull", "Core")
        var totalCompleted = 0

        for (cat in categories) {
            val categoryExercises = getExercisesForCategory(cat)
            for (exercise in categoryExercises) {
                val isCompleted = prefs.getBoolean("exercise_${cat}_${exercise.name}", false)
                if (isCompleted) totalCompleted++
            }
        }

        return totalCompleted
    }

    // Counts how many categories were entered by the user
    private fun getStartedCategoriesCount(): Int {
        val categories = listOf("Upper Body", "Lower Body", "Push", "Pull", "Core")
        return categories.count { prefs.getBoolean("started_$it", false) }
    }

    // Counts how many full categories are completely finished
    private fun getFinishedWorkoutsCount(): Int {
        val categories = listOf("Upper Body", "Lower Body", "Push", "Pull", "Core")
        var finishedCount = 0

        for (cat in categories) {
            val total = getExercisesForCategory(cat).size
            val completed = getExercisesForCategory(cat).count {
                prefs.getBoolean("exercise_${cat}_${it.name}", false)
            }

            if (total > 0 && completed == total) {
                finishedCount++
            }
        }

        return finishedCount
    }

    // Returns the workout data for the selected category
    private fun getExercisesForCategory(category: String): List<Exercise> {
        return when (category) {
            "Upper Body" -> listOf(
                Exercise("Bench Press", 4, 10),
                Exercise("Pull Ups", 3, 8),
                Exercise("Shoulder Press", 3, 10),
                Exercise("Bicep Curls", 3, 12)
            )

            "Lower Body" -> listOf(
                Exercise("Squats", 4, 12),
                Exercise("Leg Press", 3, 10),
                Exercise("Romanian Deadlift", 3, 8),
                Exercise("Calf Raises", 4, 15)
            )

            "Push" -> listOf(
                Exercise("Bench Press", 4, 10),
                Exercise("Incline Dumbbell Press", 3, 10),
                Exercise("Shoulder Press", 3, 10),
                Exercise("Tricep Pushdown", 3, 12)
            )

            "Pull" -> listOf(
                Exercise("Deadlift", 3, 6),
                Exercise("Lat Pulldown", 3, 10),
                Exercise("Seated Row", 3, 12),
                Exercise("Hammer Curl", 3, 12)
            )

            "Core" -> listOf(
                Exercise("Plank", 3, 60),
                Exercise("Leg Raises", 3, 15),
                Exercise("Russian Twists", 3, 20),
                Exercise("Cable Crunch", 3, 15)
            )

            else -> emptyList()
        }
    }
}