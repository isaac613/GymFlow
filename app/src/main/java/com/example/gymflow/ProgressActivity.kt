package com.example.gymflow

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

// Progress dashboard. All numbers are computed live from Firestore:
// - the workout plan (exercises collection) gives the totals
// - the signed-in user's progress documents give the completed counts
class ProgressActivity : AppCompatActivity() {

    private lateinit var uid: String
    private val db get() = WorkoutRepository.db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        // A signed-in user is required — progress belongs to the user
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finish()
            return
        }
        uid = user.uid

        // Top navigation
        val btnBackHome = findViewById<Button>(R.id.btnBackHomeFromProgress)
        btnBackHome.setOnClickListener {
            finish()
        }

        loadProgressFromFirestore()
    }

    // Loads the workout plan and the user's progress, then fills the dashboard.
    // Two async reads: first the full exercise plan, then this user's progress.
    private fun loadProgressFromFirestore() {
        db.collection("exercises").get()
            .addOnSuccessListener { exerciseSnapshot ->
                // How many exercises exist in each category (from the plan)
                val totalPerCategory = mutableMapOf<String, Int>()
                for (doc in exerciseSnapshot.documents) {
                    val category = doc.getString("category") ?: continue
                    totalPerCategory[category] = (totalPerCategory[category] ?: 0) + 1
                }

                // Now read this user's progress documents (one per category)
                db.collection("users").document(uid).collection("progress").get()
                    .addOnSuccessListener { progressSnapshot ->
                        val completedPerCategory = mutableMapOf<String, Int>()
                        var categoriesStarted = 0

                        for (doc in progressSnapshot.documents) {
                            val completedList =
                                doc.get("completedExercises") as? List<*> ?: emptyList<Any>()
                            completedPerCategory[doc.id] = completedList.size

                            if (doc.getBoolean("started") == true) {
                                categoriesStarted++
                            }
                        }

                        fillDashboard(totalPerCategory, completedPerCategory, categoriesStarted)
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to load progress", e)
            }

        // Recent activity comes from the user's own document
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val lastExercise =
                    userDoc.getString("lastCompletedExercise") ?: "No exercise completed yet"
                val lastCategory =
                    userDoc.getString("lastCompletedCategory") ?: "No category yet"

                findViewById<TextView>(R.id.tvLastCompletedExercise).text =
                    "Last completed: $lastExercise"
                findViewById<TextView>(R.id.tvMostActiveCategory).text =
                    "Most recent category: $lastCategory"
            }
    }

    // Fills every section of the dashboard from the computed numbers
    private fun fillDashboard(
        totalPerCategory: Map<String, Int>,
        completedPerCategory: Map<String, Int>,
        categoriesStarted: Int
    ) {
        val totalPossibleExercises = totalPerCategory.values.sum()
        val totalCompleted = completedPerCategory.values.sum()

        // A category counts as a finished workout when all its exercises are done
        val workoutsFinished = totalPerCategory.count { (category, total) ->
            total > 0 && (completedPerCategory[category] ?: 0) == total
        }

        val overallPercent = if (totalPossibleExercises == 0) {
            0
        } else {
            (totalCompleted * 100) / totalPossibleExercises
        }

        // Hero card
        findViewById<TextView>(R.id.tvHeroPercent).text = "$overallPercent%"
        findViewById<TextView>(R.id.tvHeroSubtext).text =
            "$totalCompleted of $totalPossibleExercises exercises completed"
        val progressOverall = findViewById<ProgressBar>(R.id.progressOverall)
        progressOverall.max = 100
        progressOverall.progress = overallPercent

        // Stat cards
        findViewById<TextView>(R.id.tvExercisesCompletedValue).text = totalCompleted.toString()
        findViewById<TextView>(R.id.tvCategoriesStartedValue).text = categoriesStarted.toString()
        findViewById<TextView>(R.id.tvWorkoutsFinishedValue).text = workoutsFinished.toString()

        // Category breakdown rows
        updateCategorySection(
            "Upper Body", R.id.tvUpperBodyValue, R.id.pbUpperBody,
            totalPerCategory, completedPerCategory
        )
        updateCategorySection(
            "Lower Body", R.id.tvLowerBodyValue, R.id.pbLowerBody,
            totalPerCategory, completedPerCategory
        )
        updateCategorySection(
            "Push", R.id.tvPushValue, R.id.pbPush,
            totalPerCategory, completedPerCategory
        )
        updateCategorySection(
            "Pull", R.id.tvPullValue, R.id.pbPull,
            totalPerCategory, completedPerCategory
        )
        updateCategorySection(
            "Core", R.id.tvCoreValue, R.id.pbCore,
            totalPerCategory, completedPerCategory
        )
    }

    // Updates one row in the category breakdown section
    private fun updateCategorySection(
        category: String,
        valueTextId: Int,
        progressBarId: Int,
        totalPerCategory: Map<String, Int>,
        completedPerCategory: Map<String, Int>
    ) {
        val total = totalPerCategory[category] ?: 0
        val completed = completedPerCategory[category] ?: 0

        findViewById<TextView>(valueTextId).text = "$completed / $total"

        val progressBar = findViewById<ProgressBar>(progressBarId)
        progressBar.max = if (total > 0) total else 1
        progressBar.progress = completed
    }

    companion object {
        private const val TAG = "ProgressActivity"
    }
}
