package com.example.gymflow

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect this activity to its XML layout file
        setContentView(R.layout.activity_main)

        // Get references to the buttons from the XML layout
        val btnViewWorkouts = findViewById<Button>(R.id.btnViewWorkouts)
        val btnViewProgress = findViewById<Button>(R.id.btnViewProgress)

        // Open the workout categories screen
        btnViewWorkouts.setOnClickListener {
            val intent = Intent(this, WorkoutCategoriesActivity::class.java)
            startActivity(intent)
        }

        // Open the progress screen
        btnViewProgress.setOnClickListener {
            val intent = Intent(this, ProgressActivity::class.java)
            startActivity(intent)
        }
    }
}