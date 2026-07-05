package com.example.gymflow

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Lists the workout categories. The categories are loaded from Firestore
// and the cards are created dynamically — nothing here is static, so new
// categories can be added straight from the Firebase console.
class WorkoutCategoriesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect this activity to its XML layout
        setContentView(R.layout.activity_workout_categories)

        // Back button returns the user to the home screen
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)
        btnBackHome.setOnClickListener {
            finish()
        }

        loadCategoriesFromFirestore()
    }

    // Reads the categories and the exercise counts, then builds one card
    // per category. Two async reads, chained: categories first, then
    // exercises (to show "N exercises" on each card).
    private fun loadCategoriesFromFirestore() {
        val container = findViewById<LinearLayout>(R.id.categoriesContainer)
        val db = WorkoutRepository.db

        db.collection("categories").get()
            .addOnSuccessListener { categorySnapshot ->
                // Keep the plan's intended order
                val categories = categorySnapshot.documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { it.getString("name") }

                db.collection("exercises").get()
                    .addOnSuccessListener { exerciseSnapshot ->
                        // Count how many exercises each category has
                        val countPerCategory = mutableMapOf<String, Int>()
                        for (doc in exerciseSnapshot.documents) {
                            val category = doc.getString("category") ?: continue
                            countPerCategory[category] = (countPerCategory[category] ?: 0) + 1
                        }

                        container.removeAllViews()
                        for (categoryName in categories) {
                            val count = countPerCategory[categoryName] ?: 0
                            container.addView(buildCategoryCard(categoryName, count))
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to load categories", e)
                Toast.makeText(this, "Could not load categories: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    // Builds one category card: emoji badge, name + exercise count, chevron.
    // Built in code (not XML) because the rows come from Firestore.
    private fun buildCategoryCard(categoryName: String, exerciseCount: Int): LinearLayout {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        // The card row itself
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.topMargin = dp(12)
        card.layoutParams = cardParams
        card.setPadding(dp(16), dp(16), dp(16), dp(16))
        card.setBackgroundResource(R.drawable.exercise_card_default)
        card.isClickable = true

        // Round emoji badge on the left
        val emojiBadge = TextView(this)
        emojiBadge.text = emojiForCategory(categoryName)
        emojiBadge.textSize = 22f
        emojiBadge.gravity = Gravity.CENTER
        emojiBadge.setBackgroundResource(R.drawable.category_icon_bg)
        emojiBadge.layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))

        // Category name + exercise count in the middle
        val textColumn = LinearLayout(this)
        textColumn.orientation = LinearLayout.VERTICAL
        val columnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        columnParams.marginStart = dp(14)
        textColumn.layoutParams = columnParams

        val tvName = TextView(this)
        tvName.text = categoryName
        tvName.setTextColor(Color.WHITE)
        tvName.textSize = 17f
        tvName.typeface = Typeface.DEFAULT_BOLD

        val tvCount = TextView(this)
        tvCount.text = "$exerciseCount exercises"
        tvCount.setTextColor(getColor(R.color.text_secondary))
        tvCount.textSize = 13f

        textColumn.addView(tvName)
        textColumn.addView(tvCount)

        // Chevron on the right to hint that the card navigates
        val chevron = TextView(this)
        chevron.text = "›"
        chevron.setTextColor(getColor(R.color.text_muted))
        chevron.textSize = 26f

        card.addView(emojiBadge)
        card.addView(textColumn)
        card.addView(chevron)

        // Tapping the card opens the detail screen for this category
        card.setOnClickListener { openWorkoutDetail(categoryName) }

        return card
    }

    // Picks an emoji for known categories, with a safe default for new ones
    // added later through the Firebase console
    private fun emojiForCategory(categoryName: String): String {
        return when (categoryName) {
            "Upper Body" -> "💪"
            "Lower Body" -> "🦵"
            "Push" -> "🏋️"
            "Pull" -> "🧗"
            "Core" -> "🔥"
            else -> "🏃"
        }
    }

    // Helper function to avoid repeating the same Intent code
    private fun openWorkoutDetail(category: String) {
        val intent = Intent(this, WorkoutDetailActivity::class.java)
        intent.putExtra("category", category)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "WorkoutCategoriesActivity"
    }
}
