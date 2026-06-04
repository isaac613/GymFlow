package com.example.gymflow

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Analytics
        firebaseAnalytics = Firebase.analytics

        // Connect this activity to its XML layout file
        setContentView(R.layout.activity_main)

        // --- BONUS: 3rd Party Library (Glide) ---
        // Let's use Glide to load a cool fitness image from the internet 
        // and crop it into a perfect circle, replacing the static dumbbell icon!
        val imgDumbbell = findViewById<ImageView>(R.id.imgDumbbell)
        Glide.with(this)
            .load("https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=300")
            .circleCrop()
            .into(imgDumbbell)

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

        // Setup Crash Button
        val btnCrash = findViewById<Button>(R.id.btnCrash)
        btnCrash.setOnClickListener {
            throw RuntimeException("Test Crash") // Force a crash
        }

        // Initialize Firestore
        val db = Firebase.firestore

        // Setup Firestore Save Data
        val btnSaveData = findViewById<Button>(R.id.btnSaveData)
        btnSaveData.setOnClickListener {
            val workout = hashMapOf(
                "name" to "Morning Run",
                "durationMinutes" to 30,
                "caloriesBurned" to 300
            )
            
            db.collection("workouts")
                .add(workout)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Saved! ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Setup Firestore Read Data
        val btnReadData = findViewById<Button>(R.id.btnReadData)
        btnReadData.setOnClickListener {
            db.collection("workouts")
                .get()
                .addOnSuccessListener { result ->
                    val count = result.size()
                    Toast.makeText(this, "Found $count workouts in database!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error reading: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}