package com.example.petcarekotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FoodLogsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var feedNowButton: Button

    private val foodLogs = listOf(
        FoodLog("Today, 8:30 AM", "Mike", "200g"),
        FoodLog("Yesterday, 6:45 PM", "Sarah", "200g"),
        FoodLog("Yesterday, 8:15 AM", "Mike", "200g"),
        FoodLog("Apr 14, 7:00 PM", "Emma", "200g"),
        FoodLog("Apr 14, 8:30 AM", "Sarah", "200g")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_logs, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewLogs)
        feedNowButton = view.findViewById(R.id.feedNowButton)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = FoodLogAdapter(foodLogs)

        feedNowButton.setOnClickListener {
            // Later: open a dialog or add new entry
        }

        return view
    }
}


data class FoodLog(
    val time: String,
    val author: String,
    val amount: String
)
