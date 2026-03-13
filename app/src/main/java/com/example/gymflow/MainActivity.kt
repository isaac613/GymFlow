package com.example.gymflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymflow.ui.theme.GymFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymFlowTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val currentScreen = remember { mutableStateOf("home") }
    val selectedCategory = remember { mutableStateOf("") }

    when (currentScreen.value) {
        "home" -> GymHomeScreen(
            modifier = modifier,
            onViewWorkoutsClick = { currentScreen.value = "categories" },
            onViewProgressClick = { currentScreen.value = "progress" }
        )

        "categories" -> WorkoutCategoriesScreen(
            onBackClick = { currentScreen.value = "home" },
            onCategoryClick = { category ->
                selectedCategory.value = category
                currentScreen.value = "workoutDetails"
            }
        )

        "workoutDetails" -> WorkoutDetailScreen(
            category = selectedCategory.value,
            onBackClick = { currentScreen.value = "categories" }
        )

        "progress" -> ProgressScreen(
            onBackClick = { currentScreen.value = "home" }
        )
    }
}

@Composable
fun GymHomeScreen(
    modifier: Modifier = Modifier,
    onViewWorkoutsClick: () -> Unit,
    onViewProgressClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "GymFlow",
            fontSize = 34.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your personal workout companion",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = onViewWorkoutsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Workouts")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onViewProgressClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Progress")
        }
    }
}

@Composable
fun ProgressScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Progress",
            fontSize = 30.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Completed workouts: 12",
            fontSize = 18.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Total training hours: 18",
            fontSize = 18.sp,
            color = Color.LightGray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GymHomeScreenPreview() {
    GymFlowTheme {
        GymHomeScreen(
            onViewWorkoutsClick = {},
            onViewProgressClick = {}
        )
    }
}