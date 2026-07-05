package com.example.gymflow

// Model class representing one exercise in a workout.
// Default values are required so Firestore can convert documents
// into Exercise objects automatically (toObject needs an empty constructor).
data class Exercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val category: String = "",
    val order: Int = 0,
    val imageUrl: String = "",
    var isCompleted: Boolean = false
)
