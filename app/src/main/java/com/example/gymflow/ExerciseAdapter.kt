package com.example.gymflow

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

    // ViewHolder stores references to the views inside each RecyclerView item
    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContainer: LinearLayout = itemView.findViewById(R.id.cardContainer)
        val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
        val tvExerciseSubtitle: TextView = itemView.findViewById(R.id.tvExerciseSubtitle)
        val tvExerciseSets: TextView = itemView.findViewById(R.id.tvExerciseSets)
        val tvExerciseReps: TextView = itemView.findViewById(R.id.tvExerciseReps)
        val btnCompleteExercise: Button = itemView.findViewById(R.id.btnCompleteExercise)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        // Inflate the XML layout used for a single exercise card
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)

        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]

        // Bind the basic exercise data
        holder.tvExerciseName.text = exercise.name
        holder.tvExerciseSets.text = "Sets: ${exercise.sets}"
        holder.tvExerciseReps.text = "Reps: ${exercise.reps}"

        // Small subtitle for extra visual polish
        holder.tvExerciseSubtitle.text = if (exercise.isCompleted) {
            "Exercise completed"
        } else {
            "Tap below when finished"
        }

        // Update the card appearance depending on completion state
        if (exercise.isCompleted) {
            holder.cardContainer.setBackgroundResource(R.drawable.exercise_card_completed)
            holder.btnCompleteExercise.text = "Completed"
        } else {
            holder.cardContainer.setBackgroundResource(R.drawable.exercise_card_default)
            holder.btnCompleteExercise.text = "Complete Exercise"
        }

        // Toggle completion state when the button is pressed
        holder.btnCompleteExercise.setOnClickListener {
            exercise.isCompleted = !exercise.isCompleted

            // Refresh the changed row only
            notifyItemChanged(position)

            // Notify the activity so the progress text and bar can update
            onExerciseUpdated()
        }
    }

    override fun getItemCount(): Int {
        return exercises.size
    }
}