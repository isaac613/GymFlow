package com.example.gymflow

// Model class representing one exercise in a workout
data class Exercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    var isCompleted: Boolean = false
)
