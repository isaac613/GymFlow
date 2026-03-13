package com.example.gymflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data class representing a single exercise
// Each exercise has a name, sets, reps, and a completion state
data class Exercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val completed: Boolean = false
)

@Composable
fun WorkoutDetailScreen(
    category: String,
    onBackClick: () -> Unit
) {

    // Create a stateful list of exercises for the selected category
    // remember(category) ensures a new list loads if category changes
    val exercises = remember(category) {
        mutableStateListOf<Exercise>().apply {
            addAll(getExercisesForCategory(category))
        }
    }

    // Calculate progress
    val completedCount = exercises.count { it.completed }
    val totalExercises = exercises.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(20.dp)
    ) {

        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = category,
            fontSize = 30.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Back navigation button
        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Categories")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Progress information
        Text(
            text = "Completed: $completedCount / $totalExercises exercises",
            fontSize = 18.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Visual progress bar
        LinearProgressIndicator(
            progress = if (totalExercises == 0) 0f else completedCount.toFloat() / totalExercises,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Scrollable list of exercises
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // We iterate using indexes so we can update list items
            items(exercises.indices.toList()) { index ->

                val exercise = exercises[index]

                ExerciseCard(
                    exercise = exercise,

                    // Toggle completion state when button is pressed
                    onCompleteClick = {

                        // Copy the object and update the list
                        // This triggers recomposition in Compose
                        exercises[index] = exercise.copy(
                            completed = !exercise.completed
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: Exercise,
    onCompleteClick: () -> Unit
) {

    // Change card color when exercise is completed
    val cardColor = if (exercise.completed) {
        Color(0xFF1E4D2B)
    } else {
        Color(0xFF1E1E1E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = exercise.name,
                fontSize = 22.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Sets: ${exercise.sets}",
                    fontSize = 16.sp,
                    color = Color.LightGray
                )

                Text(
                    text = "Reps: ${exercise.reps}",
                    fontSize = 16.sp,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCompleteClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (exercise.completed)
                        "Completed"
                    else
                        "Complete Exercise"
                )
            }
        }
    }
}

// Returns exercises depending on category
fun getExercisesForCategory(category: String): List<Exercise> {

    return when (category) {

        "Upper Body" -> listOf(
            Exercise("Bench Press", 4, 10),
            Exercise("Pull Ups", 3, 8),
            Exercise("Shoulder Press", 3, 10),
            Exercise("Bicep Curls", 3, 12)
        )

        "Lower Body" -> listOf(
            Exercise("Squats", 4, 12),
            Exercise("Leg Press", 3, 10),
            Exercise("Romanian Deadlift", 3, 8),
            Exercise("Calf Raises", 4, 15)
        )

        "Push" -> listOf(
            Exercise("Bench Press", 4, 10),
            Exercise("Incline Dumbbell Press", 3, 10),
            Exercise("Shoulder Press", 3, 10),
            Exercise("Tricep Pushdown", 3, 12)
        )

        "Pull" -> listOf(
            Exercise("Deadlift", 3, 6),
            Exercise("Lat Pulldown", 3, 10),
            Exercise("Seated Row", 3, 12),
            Exercise("Hammer Curl", 3, 12)
        )

        "Core" -> listOf(
            Exercise("Plank", 3, 60),
            Exercise("Leg Raises", 3, 15),
            Exercise("Russian Twists", 3, 20),
            Exercise("Cable Crunch", 3, 15)
        )

        else -> emptyList()
    }
}