package com.example.gymflow

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

// Progress dashboard. All numbers are computed live from Firestore using
// snapshot listeners — complete an exercise on another screen (or edit the
// database in the Firebase console) and this dashboard updates by itself.
class ProgressActivity : AppCompatActivity() {

    private lateinit var uid: String
    private val db get() = WorkoutRepository.db

    // Latest snapshots from the two listeners; dashboard refills when either fires
    private var totalPerCategory: Map<String, Int>? = null
    private var completedPerCategory: Map<String, Int>? = null
    private var categoriesStarted = 0

    // Drives the count-up animation of the hero percentage
    private var shownPercent = 0
    private var percentAnimator: ValueAnimator? = null

    private val listeners = mutableListOf<ListenerRegistration>()

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
        findViewById<Button>(R.id.btnBackHomeFromProgress).setOnClickListener {
            finish()
        }

        listenToPlan()
        listenToProgress()
        listenToHistory()
    }

    // Live listener on the workout plan — gives the totals per category
    private fun listenToPlan() {
        val registration = db.collection("exercises")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Plan listener failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val totals = mutableMapOf<String, Int>()
                    for (doc in snapshot.documents) {
                        val category = doc.getString("category") ?: continue
                        totals[category] = (totals[category] ?: 0) + 1
                    }
                    totalPerCategory = totals
                    fillDashboardIfReady()
                }
            }
        listeners.add(registration)
    }

    // Live listener on this user's progress documents (one per category)
    private fun listenToProgress() {
        val registration = db.collection("users").document(uid).collection("progress")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Progress listener failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val completed = mutableMapOf<String, Int>()
                    var started = 0

                    for (doc in snapshot.documents) {
                        val completedList =
                            doc.get("completedExercises") as? List<*> ?: emptyList<Any>()
                        completed[doc.id] = completedList.size
                        if (doc.getBoolean("started") == true) started++
                    }

                    completedPerCategory = completed
                    categoriesStarted = started
                    fillDashboardIfReady()
                }
            }
        listeners.add(registration)
    }

    // Live feed of the user's five most recent completions
    private fun listenToHistory() {
        val container = findViewById<LinearLayout>(R.id.historyContainer)
        val timeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.US)

        val registration = db.collection("users").document(uid).collection("history")
            .orderBy("completedAt", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "History listener failed", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                container.removeAllViews()

                if (snapshot.isEmpty) {
                    container.addView(buildHistoryRow(
                        "No activity yet — complete an exercise to see it here!", ""
                    ))
                    return@addSnapshotListener
                }

                for (doc in snapshot.documents) {
                    val exercise = doc.getString("exercise") ?: continue
                    val category = doc.getString("category") ?: ""
                    // serverTimestamp is briefly null while the write is pending
                    val time = doc.getTimestamp("completedAt")?.toDate()
                    val timeText = if (time != null) timeFormat.format(time) else "just now"

                    container.addView(buildHistoryRow("✅ $exercise — $category", timeText))
                }
            }
        listeners.add(registration)
    }

    // Builds one row of the recent-activity feed
    private fun buildHistoryRow(mainText: String, timeText: String): LinearLayout {
        val density = resources.displayMetrics.density
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = (10 * density).toInt()
        row.layoutParams = params

        val tvMain = TextView(this)
        tvMain.text = mainText
        tvMain.setTextColor(getColor(R.color.text_primary))
        tvMain.textSize = 15f
        row.addView(tvMain)

        if (timeText.isNotEmpty()) {
            val tvTime = TextView(this)
            tvTime.text = timeText
            tvTime.setTextColor(getColor(R.color.text_muted))
            tvTime.textSize = 12f
            row.addView(tvTime)
        }

        return row
    }

    // Refills the dashboard once both listeners have delivered data
    private fun fillDashboardIfReady() {
        val totals = totalPerCategory ?: return
        val completed = completedPerCategory ?: return

        findViewById<ProgressBar>(R.id.pbDashboardLoading).visibility = View.GONE

        val totalPossibleExercises = totals.values.sum()
        val totalCompleted = completed.values.sum()

        // A category counts as a finished workout when all its exercises are done
        val workoutsFinished = totals.count { (category, total) ->
            total > 0 && (completed[category] ?: 0) == total
        }

        val overallPercent = if (totalPossibleExercises == 0) {
            0
        } else {
            (totalCompleted * 100) / totalPossibleExercises
        }

        // Hero card: animate the donut ring filling and the percent counting up
        findViewById<TextView>(R.id.tvHeroSubtext).text =
            "$totalCompleted of $totalPossibleExercises exercises completed"

        val progressOverall = findViewById<ProgressBar>(R.id.progressOverall)
        progressOverall.max = 100
        progressOverall.setProgress(overallPercent, true)

        val tvPercent = findViewById<TextView>(R.id.tvHeroPercent)
        percentAnimator?.cancel()
        percentAnimator = ValueAnimator.ofInt(shownPercent, overallPercent).apply {
            duration = 800
            addUpdateListener { animator ->
                shownPercent = animator.animatedValue as Int
                tvPercent.text = "$shownPercent%"
            }
            start()
        }

        // Stat cards
        findViewById<TextView>(R.id.tvExercisesCompletedValue).text = totalCompleted.toString()
        findViewById<TextView>(R.id.tvCategoriesStartedValue).text = categoriesStarted.toString()
        findViewById<TextView>(R.id.tvWorkoutsFinishedValue).text = workoutsFinished.toString()

        // Category breakdown rows
        updateCategorySection("Upper Body", R.id.tvUpperBodyValue, R.id.pbUpperBody, totals, completed)
        updateCategorySection("Lower Body", R.id.tvLowerBodyValue, R.id.pbLowerBody, totals, completed)
        updateCategorySection("Push", R.id.tvPushValue, R.id.pbPush, totals, completed)
        updateCategorySection("Pull", R.id.tvPullValue, R.id.pbPull, totals, completed)
        updateCategorySection("Core", R.id.tvCoreValue, R.id.pbCore, totals, completed)
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
        // true = animate the fill instead of jumping
        progressBar.setProgress(completed, true)
    }

    // Slide back out to the right when leaving this screen
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the live listeners and animations when the screen closes
        listeners.forEach { it.remove() }
        percentAnimator?.cancel()
    }

    companion object {
        private const val TAG = "ProgressActivity"
    }
}
