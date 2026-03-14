package com.example.gymflow

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProgressActivity : AppCompatActivity() {

    private val prefs by lazy {
        getSharedPreferences("gymflow_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        // Top navigation
        val btnBackHome = findViewById<Button>(R.id.btnBackHomeFromProgress)
        btnBackHome.setOnClickListener {
            finish()
        }

        // Hero section
        val tvHeroPercent = findViewById<TextView>(R.id.tvHeroPercent)
        val tvHeroSubtext = findViewById<TextView>(R.id.tvHeroSubtext)
        val progressOverall = findViewById<ProgressBar>(R.id.progressOverall)

        // Stat cards
        val tvExercisesCompleted = findViewById<TextView>(R.id.tvExercisesCompletedValue)
        val tvCategoriesStarted = findViewById<TextView>(R.id.tvCategoriesStartedValue)
        val tvWorkoutsFinished = findViewById<TextView>(R.id.tvWorkoutsFinishedValue)

        // Category breakdown
        val tvUpperBodyValue = findViewById<TextView>(R.id.tvUpperBodyValue)
        val tvLowerBodyValue = findViewById<TextView>(R.id.tvLowerBodyValue)
        val tvPushValue = findViewById<TextView>(R.id.tvPushValue)
        val tvPullValue = findViewById<TextView>(R.id.tvPullValue)
        val tvCoreValue = findViewById<TextView>(R.id.tvCoreValue)

        val pbUpperBody = findViewById<ProgressBar>(R.id.pbUpperBody)
        val pbLowerBody = findViewById<ProgressBar>(R.id.pbLowerBody)
        val pbPush = findViewById<ProgressBar>(R.id.pbPush)
        val pbPull = findViewById<ProgressBar>(R.id.pbPull)
        val pbCore = findViewById<ProgressBar>(R.id.pbCore)

        // Recent activity
        val tvLastCompletedExercise = findViewById<TextView>(R.id.tvLastCompletedExercise)
        val tvMostActiveCategory = findViewById<TextView>(R.id.tvMostActiveCategory)

        // Total app-wide numbers
        val totalCompleted = prefs.getInt("total_exercises_completed", 0)
        val categoriesStarted = prefs.getInt("categories_started_count", 0)
        val workoutsFinished = prefs.getInt("workouts_finished_count", 0)

        // Total possible exercises across all categories = 20 in this app
        val totalPossibleExercises = 20
        val overallPercent = if (totalPossibleExercises == 0) {
            0
        } else {
            (totalCompleted * 100) / totalPossibleExercises
        }

        // Fill hero card
        tvHeroPercent.text = "$overallPercent%"
        tvHeroSubtext.text = "$totalCompleted of $totalPossibleExercises exercises completed"
        progressOverall.max = 100
        progressOverall.progress = overallPercent

        // Fill stat cards
        tvExercisesCompleted.text = totalCompleted.toString()
        tvCategoriesStarted.text = categoriesStarted.toString()
        tvWorkoutsFinished.text = workoutsFinished.toString()

        // Fill category breakdown
        updateCategorySection("Upper Body", tvUpperBodyValue, pbUpperBody)
        updateCategorySection("Lower Body", tvLowerBodyValue, pbLowerBody)
        updateCategorySection("Push", tvPushValue, pbPush)
        updateCategorySection("Pull", tvPullValue, pbPull)
        updateCategorySection("Core", tvCoreValue, pbCore)

        // Recent activity info
        val lastExercise = prefs.getString("last_completed_exercise", "No exercise completed yet")
        val lastCategory = prefs.getString("last_completed_category", "No category yet")

        tvLastCompletedExercise.text = "Last completed: $lastExercise"
        tvMostActiveCategory.text = "Most recent category: $lastCategory"
    }

    // Updates one row in the category breakdown section
    private fun updateCategorySection(
        category: String,
        valueText: TextView,
        progressBar: ProgressBar
    ) {
        val completed = prefs.getInt("completed_$category", 0)
        val total = prefs.getInt("total_$category", 4)

        valueText.text = "$completed / $total"

        progressBar.max = total
        progressBar.progress = completed
    }
}