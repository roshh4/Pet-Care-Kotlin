package com.example.petcarekotlin.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R

class ActivityFeedFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ActivityFeedAdapter
    private lateinit var activityList: List<ActivityItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity_feed, container, false)

        recyclerView = view.findViewById(R.id.activityRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        activityList = listOf(
            ActivityItem("👤 Mike fed Buddy", "Today, 8:30 AM"),
            ActivityItem("👤 Sarah added a vet visit record for Buddy", "Yesterday, 2:15 PM"),
            ActivityItem("👤 Emma changed litter for Whiskers", "Yesterday, 7:30 PM"),
            ActivityItem("👤 John groomed Luna", "2 days ago, 6:00 PM"),
            ActivityItem("👤 Mike gave medicine to Buddy", "2 days ago, 10:00 AM")
        )

        adapter = ActivityFeedAdapter(activityList)
        recyclerView.adapter = adapter

        return view
    }
} 