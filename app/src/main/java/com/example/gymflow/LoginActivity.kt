package com.example.gymflow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

// Entry point of the app. Handles Google Sign-In through Firebase Authentication.
// This is our 5th screen and the gatekeeper for the rest of the app.
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Launches the Google account picker and receives the result back
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Get the Google account that the user selected
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // Hand the Google ID token over to Firebase to complete authentication
            firebaseAuthWithGoogle(account.idToken)
        } catch (e: ApiException) {
            // User cancelled, or something went wrong with Google Sign-In
            Log.w(TAG, "Google sign in failed", e)
            Toast.makeText(this, "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In to request the ID token (needed for Firebase)
        // and the user's email. default_web_client_id is generated automatically
        // by the google-services plugin from google-services.json.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)
        btnGoogleSignIn.setOnClickListener {
            // Start the Google account picker
            signInLauncher.launch(googleSignInClient.signInIntent)
        }

        // --- BONUS: second authentication method (email + password) ---
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        findViewById<Button>(R.id.btnEmailSignIn).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (validateEmailInput(email, password)) {
                signInWithEmail(email, password)
            }
        }

        findViewById<Button>(R.id.btnEmailRegister).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (validateEmailInput(email, password)) {
                registerWithEmail(email, password)
            }
        }
    }

    // Basic input checks before contacting Firebase
    private fun validateEmailInput(email: String, password: String): Boolean {
        if (email.isEmpty() || !email.contains("@")) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            // Firebase requires passwords of at least 6 characters
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    // Signs in an existing email/password user
    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    goToHome()
                } else {
                    Log.w(TAG, "signInWithEmail failed", task.exception)
                    Toast.makeText(
                        this,
                        "Sign-in failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // Creates a brand new email/password account and signs the user in
    private fun registerWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created — welcome!", Toast.LENGTH_SHORT).show()
                    goToHome()
                } else {
                    Log.w(TAG, "registerWithEmail failed", task.exception)
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        // If the user is already signed in, skip the login screen entirely
        if (auth.currentUser != null) {
            goToHome()
        }
    }

    // Exchanges the Google ID token for a Firebase credential and signs the user in
    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken == null) {
            Toast.makeText(this, "No ID token returned from Google.", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val name = auth.currentUser?.displayName ?: "athlete"
                    Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                    goToHome()
                } else {
                    Log.w(TAG, "signInWithCredential failed", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Moves to the home screen and closes login so Back doesn't return here
    private fun goToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
