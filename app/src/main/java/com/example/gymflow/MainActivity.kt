package com.example.gymflow

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Home screen. Requires a signed-in user, greets them by name, and lets them
// take a custom profile photo with the device camera (phone capability).
class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private lateinit var imgProfile: ImageView
    private lateinit var uid: String

    // Where the camera will write the photo it takes
    private var currentImageUri: Uri? = null

    // Live listeners on the daily target and workout history, removed in onDestroy
    private var dailyTargetListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    // Latest values feeding the hero card's daily-goal progress bar
    private var dailyGoal = 5
    private var todayCount = 0

    private val db get() = WorkoutRepository.db

    // Launches the device camera app. The camera writes the photo into
    // currentImageUri and reports back whether a picture was actually taken.
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = currentImageUri
        if (success && uri != null) {
            // Show the new photo immediately
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(imgProfile)

            // Remember the photo so it survives app restarts.
            // Note: the URI points to this device's storage, so the custom
            // photo is only visible on the device where it was taken.
            db.collection("users").document(uid)
                .set(mapOf("customPhotoUri" to uri.toString()), SetOptions.merge())

            Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show()
        }
    }

    // Asks the user for camera + media permissions and reacts to the answer
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.CAMERA] == true) {
            // Permission granted — open the camera right away
            captureImage()
        } else {
            Toast.makeText(
                this,
                "Camera permission is needed to take a profile photo.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Analytics
        firebaseAnalytics = Firebase.analytics

        // Require a signed-in user. If nobody is signed in, send them to Login.
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        uid = user.uid

        // Connect this activity to its XML layout file
        setContentView(R.layout.activity_main)

        // Greet the signed-in user by name. Google users have a display name;
        // email/password users are greeted by the name part of their email.
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        // takeIf(isNotBlank) guards against Firebase returning "" instead of
        // null for the display name of email/password users
        val greetingName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "Athlete"
        tvWelcome.text = greetingName.replaceFirstChar { it.uppercase() }

        // Show the profile photo: the custom camera photo if one was taken,
        // otherwise the user's Google account photo.
        imgProfile = findViewById(R.id.imgProfile)
        loadProfilePhoto(user.photoUrl)

        // Tapping the profile photo takes a new one with the camera
        imgProfile.setOnClickListener {
            if (hasCameraPermission()) {
                captureImage()
            } else {
                // Ask only when the user actually wants to use the camera
                cameraPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_MEDIA_LOCATION
                    )
                )
            }
        }

        // Navigation cards: workouts and progress
        findViewById<LinearLayout>(R.id.cardWorkouts).setOnClickListener {
            startActivity(Intent(this, WorkoutCategoriesActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.cardProgress).setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }

        // Sign the user out of both Firebase and Google, then return to Login
        val btnSignOut = findViewById<Button>(R.id.btnSignOut)
        btnSignOut.setOnClickListener {
            auth.signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        // --- BONUS: 3rd Party Library (Glide) ---
        // Load a motivation banner from the internet, cropped and rounded
        // with Glide transformations.
        val imgBanner = findViewById<ImageView>(R.id.imgBanner)
        Glide.with(this)
            .load("https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=900")
            .transform(CenterCrop(), RoundedCorners(resources.getDimensionPixelSize(R.dimen.banner_corner_radius)))
            .into(imgBanner)

        // The daily target (label text + goal) comes from Firestore via a
        // real-time listener: edit config/dailyTarget in the Firebase console
        // and the hero card updates on screen instantly.
        dailyTargetListener = WorkoutRepository.listenToDailyTarget { title, subtitle, goal ->
            findViewById<TextView>(R.id.tvTargetLabel).text = title
            findViewById<TextView>(R.id.tvTargetText).text = subtitle
            dailyGoal = goal
            renderDailyProgress()
        }

        // Live listener on the user's workout history: keeps the streak line
        // and the daily-goal progress bar up to date the moment an exercise
        // is completed anywhere in the app.
        listenToHistory()

        // Seed the workout plan into Firestore on first run (no-op afterwards).
        // All app data lives in Firestore — nothing is hardcoded at runtime.
        WorkoutRepository.seedDatabaseIfEmpty()

        // Backfill demo images for exercises created before images existed
        WorkoutRepository.ensureExerciseImages()
    }

    // Watches the workout history and recomputes the streak and today's
    // completed-exercise count on every change.
    private fun listenToHistory() {
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

        historyListener = db.collection("users").document(uid).collection("history")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                // Every calendar day on which something was completed
                val completionDays = snapshot.documents
                    .mapNotNull { it.getTimestamp("completedAt")?.toDate() }
                    .map { dayFormat.format(it) }

                val activeDays = completionDays.toSet()
                val today = dayFormat.format(Calendar.getInstance().time)

                // How many exercises were completed today (drives the hero bar)
                todayCount = completionDays.count { it == today }
                renderDailyProgress()

                // Walk backwards day by day counting the streak. If nothing
                // was done today yet, an ongoing streak may still end yesterday.
                val calendar = Calendar.getInstance()
                if (!activeDays.contains(dayFormat.format(calendar.time))) {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }

                var streak = 0
                while (activeDays.contains(dayFormat.format(calendar.time))) {
                    streak++
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }

                findViewById<TextView>(R.id.tvStreak).text = when {
                    streak >= 2 -> "🔥 $streak-day streak!"
                    streak == 1 -> "🔥 1-day streak — come back tomorrow!"
                    else -> "Complete an exercise to start a streak!"
                }
            }
    }

    // Fills the hero card's progress line and bar from the latest goal and
    // today's completion count
    private fun renderDailyProgress() {
        val capped = todayCount.coerceAtMost(dailyGoal)
        val text = if (todayCount >= dailyGoal && dailyGoal > 0) {
            "✅ Goal reached — $todayCount / $dailyGoal exercises today!"
        } else {
            "$todayCount / $dailyGoal exercises today"
        }
        findViewById<TextView>(R.id.tvTargetProgress).text = text

        val bar = findViewById<ProgressBar>(R.id.pbDailyGoal)
        bar.max = if (dailyGoal > 0) dailyGoal else 1
        bar.progress = capped
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the live listeners when the screen closes
        dailyTargetListener?.remove()
        historyListener?.remove()
    }

    // Shows the saved camera photo if one exists, otherwise the Google photo
    private fun loadProfilePhoto(googlePhotoUrl: Uri?) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val customPhoto = userDoc.getString("customPhotoUri")
                Glide.with(this)
                    .load(customPhoto?.let { Uri.parse(it) } ?: googlePhotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_dumbbell)
                    .error(R.drawable.ic_dumbbell)
                    .into(imgProfile)
            }
    }

    // True when the app already holds the camera permission
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Creates a destination file in the device media store and opens the camera
    private fun captureImage() {
        val imageUri = createImageUri()
        if (imageUri != null) {
            currentImageUri = imageUri
            takePictureLauncher.launch(imageUri)
        } else {
            Toast.makeText(this, "Could not create image file.", Toast.LENGTH_SHORT).show()
        }
    }

    // Creates an empty image entry in the media store and returns its URI.
    // The camera app will write the actual photo into this location.
    private fun createImageUri(): Uri? {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "GymFlow Profile Photo")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")

        return contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )
    }
}
