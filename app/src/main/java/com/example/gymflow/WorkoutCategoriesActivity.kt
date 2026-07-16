package com.example.gymflow

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.firebase.firestore.ListenerRegistration

// Lists the workout categories as image cards. Everything is loaded from
// Firestore and built dynamically — each card uses one of its exercises'
// demo photos as the background, so new categories added in the Firebase
// console appear automatically.
class WorkoutCategoriesActivity : AppCompatActivity() {

    // Live listener on the categories collection, removed in onDestroy
    private var categoriesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect this activity to its XML layout
        setContentView(R.layout.activity_workout_categories)

        // Back button returns the user to the home screen
        findViewById<Button>(R.id.btnBackHome).setOnClickListener {
            finish()
        }

        loadCategoriesFromFirestore()
    }

    // Live listener on the categories collection: the screen rebuilds itself
    // whenever the plan changes. The exercises are re-read on each change for
    // the per-category counts and the card background images.
    private fun loadCategoriesFromFirestore() {
        val container = findViewById<LinearLayout>(R.id.categoriesContainer)
        val spinner = findViewById<ProgressBar>(R.id.pbLoadingCategories)
        val emptyState = findViewById<TextView>(R.id.tvNoCategories)
        val db = WorkoutRepository.db

        categoriesListener = db.collection("categories")
            .addSnapshotListener { categorySnapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Categories listener failed", error)
                    Toast.makeText(
                        this, "Could not load categories: ${error.message}", Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                if (categorySnapshot == null) return@addSnapshotListener

                // Keep the plan's intended order
                val categories = categorySnapshot.documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { it.getString("name") }

                db.collection("exercises").get()
                    .addOnSuccessListener { exerciseSnapshot ->
                        // Per category: how many exercises, and the first
                        // available demo image to use as the card background
                        val countPerCategory = mutableMapOf<String, Int>()
                        val imagePerCategory = mutableMapOf<String, String>()

                        for (doc in exerciseSnapshot.documents) {
                            val category = doc.getString("category") ?: continue
                            countPerCategory[category] = (countPerCategory[category] ?: 0) + 1

                            val image = doc.getString("imageUrl")
                            if (!image.isNullOrEmpty() && category !in imagePerCategory) {
                                imagePerCategory[category] = image
                            }
                        }

                        spinner.visibility = View.GONE
                        emptyState.visibility =
                            if (categories.isEmpty()) View.VISIBLE else View.GONE

                        container.removeAllViews()
                        for (categoryName in categories) {
                            container.addView(
                                buildCategoryCard(
                                    categoryName,
                                    countPerCategory[categoryName] ?: 0,
                                    imagePerCategory[categoryName]
                                )
                            )
                        }
                        // Staggered entrance animation for the cards
                        container.scheduleLayoutAnimation()
                    }
            }
    }

    // Builds one image card: exercise photo background, dark fade, then
    // emoji + category name + exercise count on top, chevron on the right.
    private fun buildCategoryCard(
        categoryName: String,
        exerciseCount: Int,
        imageUrl: String?
    ): FrameLayout {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val card = FrameLayout(this)
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(110)
        )
        cardParams.topMargin = dp(14)
        card.layoutParams = cardParams
        card.isClickable = true
        card.isFocusable = true

        // Ripple feedback on tap
        val rippleValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, rippleValue, true)
        card.foreground = getDrawable(rippleValue.resourceId)

        // Background: the category's first exercise photo (rounded), or the
        // plain dark card style when no image exists yet
        val image = ImageView(this)
        image.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .transform(CenterCrop(), RoundedCorners(dp(18)))
                .into(image)
        } else {
            image.setBackgroundResource(R.drawable.exercise_card_default)
        }
        card.addView(image)

        // Dark fade so the text on the left stays readable
        val scrim = View(this)
        scrim.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        scrim.setBackgroundResource(R.drawable.card_scrim)
        card.addView(scrim)

        // Category name + exercise count
        val textColumn = LinearLayout(this)
        textColumn.orientation = LinearLayout.VERTICAL
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        textParams.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        textParams.marginStart = dp(20)
        textColumn.layoutParams = textParams

        val tvName = TextView(this)
        tvName.text = "${emojiForCategory(categoryName)}  $categoryName"
        tvName.setTextColor(Color.WHITE)
        tvName.textSize = 20f
        tvName.typeface = Typeface.DEFAULT_BOLD

        val tvCount = TextView(this)
        tvCount.text = "$exerciseCount exercises"
        tvCount.setTextColor(Color.parseColor("#D1D5DB"))
        tvCount.textSize = 13f

        textColumn.addView(tvName)
        textColumn.addView(tvCount)
        card.addView(textColumn)

        // Chevron on the right to hint that the card navigates
        val chevron = TextView(this)
        chevron.text = "›"
        chevron.setTextColor(Color.WHITE)
        chevron.textSize = 26f
        val chevronParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        chevronParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        chevronParams.marginEnd = dp(20)
        chevron.layoutParams = chevronParams
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
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // Slide back out to the right when leaving this screen
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the live listener when the screen closes
        categoriesListener?.remove()
    }

    companion object {
        private const val TAG = "WorkoutCategoriesActivity"
    }
}
