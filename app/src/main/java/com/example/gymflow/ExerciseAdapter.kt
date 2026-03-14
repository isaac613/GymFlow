package com.example.gymflow

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExerciseAdapter(
    private val exercises: MutableList<Exercise>,
    private val onExerciseUpdated: () -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    // Holds references to the views inside each RecyclerView item
    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContainer: LinearLayout = itemView.findViewById(R.id.cardContainer)
        val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
        val tvExerciseSets: TextView = itemView.findViewById(R.id.tvExerciseSets)
        val tvExerciseReps: TextView = itemView.findViewById(R.id.tvExerciseReps)
        val btnCompleteExercise: Button = itemView.findViewById(R.id.btnCompleteExercise)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        // Inflate the XML layout for each exercise item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)

        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]

        // Bind the exercise data into the card views
        holder.tvExerciseName.text = exercise.name
        holder.tvExerciseSets.text = "Sets: ${exercise.sets}"
        holder.tvExerciseReps.text = "Reps: ${exercise.reps}"

        // Change the UI depending on completion state
        if (exercise.isCompleted) {
            holder.cardContainer.setBackgroundColor(Color.parseColor("#1E4D2B"))
            holder.btnCompleteExercise.text = "Completed"
        } else {
            holder.cardContainer.setBackgroundColor(Color.parseColor("#1E1E1E"))
            holder.btnCompleteExercise.text = "Complete Exercise"
        }

        // Toggle completion state when the button is clicked
        holder.btnCompleteExercise.setOnClickListener {
            exercise.isCompleted = !exercise.isCompleted

            // Refresh only this item
            notifyItemChanged(position)

            // Let the activity know progress should be updated
            onExerciseUpdated()
        }
    }

    override fun getItemCount(): Int {
        return exercises.size
    }
}