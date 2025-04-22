package com.example.petcarekotlin.foodlogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FoodLogsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var feedNowButton: Button
    private var bottomSheetDialog: BottomSheetDialog? = null

    private val foodLogs = mutableListOf(
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
            showRecordFeedingBottomSheet()
        }

        return view
    }

    private fun showRecordFeedingBottomSheet() {
        // Create the bottom sheet dialog
        bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_record_feeding, null)
        bottomSheetDialog?.setContentView(bottomSheetView)

        // Get the current time and format it
        val timeEditText = bottomSheetView.findViewById<EditText>(R.id.timeEditText)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeEditText.setText(sdf.format(Date()))

        // Set up cancel button
        bottomSheetView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            bottomSheetDialog?.dismiss()
        }

        // Set up close button
        bottomSheetView.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
            bottomSheetDialog?.dismiss()
        }

        // Set up save button
        bottomSheetView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val time = timeEditText.text.toString()
            val amount = bottomSheetView.findViewById<EditText>(R.id.amountEditText).text.toString()

            if (amount.isEmpty()) {
                Toast.makeText(context, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add new log entry to the top of the list
            val currentTime = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date())
            foodLogs.add(0, FoodLog("Today, $time", "You", "${amount}g"))
            recyclerView.adapter?.notifyItemInserted(0)
            recyclerView.scrollToPosition(0)

            // Dismiss the bottom sheet
            bottomSheetDialog?.dismiss()

            // Show success message
            Toast.makeText(context, "Feeding recorded successfully", Toast.LENGTH_SHORT).show()
        }

        bottomSheetDialog?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }
}

data class FoodLog(
    val time: String,
    val author: String,
    val amount: String
) 