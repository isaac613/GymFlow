package com.example.gymflow

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ProgressActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect this activity to its XML layout
        setContentView(R.layout.activity_progress)

        // Back button returns to the previous screen
        val btnBackHome = findViewById<Button>(R.id.btnBackHomeFromProgress)

        btnBackHome.setOnClickListener {
            finish()
        }
    }
}
