package com.example.gymflow

import com.google.firebase.firestore.DocumentId

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
    var isCompleted: Boolean = false,
    // @DocumentId tells Firestore to fill this with the document's ID on read,
    // which we need in order to delete the right exercise. It is placed last
    // so the positional constructor used by the seed plan stays unchanged.
    @DocumentId val id: String = ""
)
