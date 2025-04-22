package com.example.petcarekotlin.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R

class ActivityFeedAdapter(private val activityList: List<ActivityItem>) :
    RecyclerView.Adapter<ActivityFeedAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val activityText: TextView = itemView.findViewById(R.id.activityText)
        val activityTime: TextView = itemView.findViewById(R.id.activityTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_activity_item, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val item = activityList[position]
        holder.activityText.text = item.description
        holder.activityTime.text = item.time
    }

    override fun getItemCount(): Int = activityList.size
} 