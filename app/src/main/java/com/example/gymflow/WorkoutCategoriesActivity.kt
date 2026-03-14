package com.example.gymflow

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WorkoutCategoriesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect this activity to its XML layout
        setContentView(R.layout.activity_workout_categories)

        // Back button returns the user to the home screen
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)

        // Category buttons
        val btnUpperBody = findViewById<Button>(R.id.btnUpperBody)
        val btnLowerBody = findViewById<Button>(R.id.btnLowerBody)
        val btnPush = findViewById<Button>(R.id.btnPush)
        val btnPull = findViewById<Button>(R.id.btnPull)
        val btnCore = findViewById<Button>(R.id.btnCore)

        btnBackHome.setOnClickListener {
            finish()
        }

        // Each button opens the detail screen and sends the chosen category
        btnUpperBody.setOnClickListener { openWorkoutDetail("Upper Body") }
        btnLowerBody.setOnClickListener { openWorkoutDetail("Lower Body") }
        btnPush.setOnClickListener { openWorkoutDetail("Push") }
        btnPull.setOnClickListener { openWorkoutDetail("Pull") }
        btnCore.setOnClickListener { openWorkoutDetail("Core") }
    }

    // Helper function to avoid repeating the same Intent code
    private fun openWorkoutDetail(category: String) {
        val intent = Intent(this, WorkoutDetailActivity::class.java)
        intent.putExtra("category", category)
        startActivity(intent)
    }
}