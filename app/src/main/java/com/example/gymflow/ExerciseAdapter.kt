package com.example.gymflow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class ExerciseAdapter(
    private val exercises: MutableList<Exercise>,
    private val onExerciseUpdated: (Exercise) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    // Holds references to the views inside each RecyclerView item
    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContainer: LinearLayout = itemView.findViewById(R.id.cardContainer)
        val imgExercise: ImageView = itemView.findViewById(R.id.imgExercise)
        val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
        val tvExerciseSubtitle: TextView = itemView.findViewById(R.id.tvExerciseSubtitle)
        val tvExerciseSets: TextView = itemView.findViewById(R.id.tvExerciseSets)
        val tvExerciseReps: TextView = itemView.findViewById(R.id.tvExerciseReps)
        val btnCompleteExercise: Button = itemView.findViewById(R.id.btnCompleteExercise)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)

        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]

        // Bind exercise content into the card
        holder.tvExerciseName.text = exercise.name
        holder.tvExerciseSets.text = "Sets: ${exercise.sets}"
        holder.tvExerciseReps.text = "Reps: ${exercise.reps}"

        // Load the exercise demo image from Firestore's URL with Glide.
        // Cards without an image simply don't show the image area.
        if (exercise.imageUrl.isNotEmpty()) {
            holder.imgExercise.visibility = View.VISIBLE
            val radius = holder.itemView.resources
                .getDimensionPixelSize(R.dimen.exercise_image_corner_radius)
            Glide.with(holder.imgExercise)
                .load(exercise.imageUrl)
                .transform(CenterCrop(), RoundedCorners(radius))
                .placeholder(R.drawable.exercise_card_default)
                .error(R.drawable.exercise_card_default)
                .into(holder.imgExercise)
        } else {
            holder.imgExercise.visibility = View.GONE
        }

        // Small helper subtitle for better UI polish
        holder.tvExerciseSubtitle.text = if (exercise.isCompleted) {
            "Exercise completed"
        } else {
            "Tap below when finished"
        }

        // Change appearance depending on completion state.
        // The button stays enabled when completed so users can undo a
        // mistaken tap — pressing it again un-completes the exercise.
        if (exercise.isCompleted) {

            holder.cardContainer.setBackgroundResource(R.drawable.exercise_card_completed)

            holder.btnCompleteExercise.text = "✓ Completed — tap to undo"
            holder.btnCompleteExercise.setBackgroundResource(R.drawable.exercise_button_done)

        } else {

            holder.cardContainer.setBackgroundResource(R.drawable.exercise_card_default)

            holder.btnCompleteExercise.text = "Complete Exercise"
            holder.btnCompleteExercise.setBackgroundResource(R.drawable.exercise_button_active)
        }

        holder.btnCompleteExercise.setOnClickListener {
            // Toggle completion state
            exercise.isCompleted = !exercise.isCompleted

            // Refresh only the changed row
            notifyItemChanged(position)

            // Send the changed exercise back to the activity
            onExerciseUpdated(exercise)
        }
    }

    override fun getItemCount(): Int {
        return exercises.size
    }
}