package com.example.petcarekotlin.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R

class ActivityFeedFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_activity_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.activityRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Sample data - in a real app you would fetch this from a database
        val activityItems = listOf(
            ActivityItem("Mike fed Buddy", "Today, 8:30 AM"),
            ActivityItem("Sarah added a vet visit record for Buddy", "Yesterday, 2:15 PM"),
            ActivityItem("Emma changed litter for Whiskers", "Yesterday, 7:30 PM")
        )

        // Set up the adapter
        recyclerView.adapter = ActivityAdapter(activityItems)
    }

    // Inner adapter class to handle the activity items
    private inner class ActivityAdapter(private val items: List<ActivityItem>) : 
        RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.activityIcon)
            val text: TextView = view.findViewById(R.id.activityText)
            val time: TextView = view.findViewById(R.id.activityTime)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_activity_item, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.text.text = item.description
            holder.time.text = item.time
        }
        
        override fun getItemCount() = items.size
    }
} 