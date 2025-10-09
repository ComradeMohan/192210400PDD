package com.simats.univault

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FeedbackAdapter(private val feedbackList: List<Feedback>) :
    RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

    inner class FeedbackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFeedback: TextView = itemView.findViewById(R.id.tvFeedbackText)
        val tvUser: TextView = itemView.findViewById(R.id.tvUser)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feedback, parent, false)
        return FeedbackViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        val item = feedbackList[position]
        holder.tvUser.text = "User: ${item.user_id}"
        holder.tvFeedback.text = item.feedback
        holder.tvTime.text = item.created_at
    }

    override fun getItemCount(): Int = feedbackList.size
}
