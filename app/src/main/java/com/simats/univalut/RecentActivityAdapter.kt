package com.simats.univalut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentActivityAdapter(private val activityList: List<String>) :
    RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvActivity: TextView = itemView.findViewById(R.id.tvActivityText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_activity, parent, false)
        return ActivityViewHolder(view)
    }


    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.tvActivity.text = activityList[position]
    }

    override fun getItemCount(): Int = activityList.size
}
