package com.example.gymflow

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect the activity to its XML layout
        setContentView(R.layout.activity_workout_detail)

        // Find views from the layout
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvProgressText = findViewById(R.id.tvProgressText)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewExercises)
        btnBackCategories = findViewById(R.id.btnBackCategories)

        // Read the selected category from the Intent
        val category = intent.getStringExtra("category") ?: "Workout"

        // Show the category as the page title
        tvWorkoutTitle.text = category

        // Load exercises based on the selected category
        exercises = getExercisesForCategory(category).toMutableList()

        // Set up RecyclerView with a vertical list layout
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Create the adapter and pass a callback for button clicks
        exerciseAdapter = ExerciseAdapter(exercises) {
            updateProgress()
        }

        recyclerView.adapter = exerciseAdapter

        // Back button closes this activity and returns to categories
        btnBackCategories.setOnClickListener {
            finish()
        }

        // Update the progress area when the screen first loads
        updateProgress()
    }

    // Updates the progress text and progress bar
    private fun updateProgress() {
        val completedCount = exercises.count { it.isCompleted }
        val totalCount = exercises.size

        tvProgressText.text = "Completed: $completedCount / $totalCount exercises"
        progressBar.max = totalCount
        progressBar.progress = completedCount
    }

    // Returns exercises depending on the category the user selected
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