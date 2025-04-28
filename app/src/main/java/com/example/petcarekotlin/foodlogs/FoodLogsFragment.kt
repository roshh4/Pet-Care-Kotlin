package com.example.petcarekotlin.foodlogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FoodLogsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var feedNowButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyStateView: TextView
    private var bottomSheetDialog: BottomSheetDialog? = null
    
    private lateinit var viewModel: FoodLogViewModel
    private val foodLogAdapter = FoodLogAdapter(mutableListOf())
    
    private var petId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get pet ID from arguments
        arguments?.let {
            petId = it.getString(ARG_PET_ID)
        }
        
        // If no pet ID in arguments, check shared preferences
        if (petId == null) {
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
            petId = sharedPrefs.getString("CURRENT_PET_ID", null)
        }
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(FoodLogViewModel::class.java)
        
        // Initialize user information
        viewModel.initializeUserInfo(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_logs, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewLogs)
        feedNowButton = view.findViewById(R.id.feedNowButton)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        emptyStateView = view.findViewById(R.id.emptyStateTextView)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = foodLogAdapter

        // Set up Feed Now button
        feedNowButton.setOnClickListener {
            showRecordFeedingBottomSheet()
        }

        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Observe ViewModel data
        viewModel.foodLogs.observe(viewLifecycleOwner) { foodLogModels ->
            if (foodLogModels.isEmpty()) {
                showEmptyState()
            } else {
                showLogs()
                
                // Convert models to UI models and update adapter
                val uiLogs = foodLogModels.map { model -> 
                    viewModel.convertToUiModel(model)
                }
                
                // Update the adapter with new data
                foodLogAdapter.updateLogs(uiLogs)
            }
        }
        
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
        
        // Load food logs if we have a pet ID
        petId?.let { id ->
            viewModel.loadFoodLogs(id)
        } ?: run {
            // No pet ID provided, show error state
            showEmptyState()
            emptyStateView.text = "No pet selected. Please select a pet from the home screen."
            Toast.makeText(context, "No pet selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRecordFeedingBottomSheet() {
        // If no pet ID is available, show an error
        if (petId == null) {
            Toast.makeText(context, "No pet selected. Please select a pet first.", Toast.LENGTH_SHORT).show()
            return
        }
        
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
            val amount = bottomSheetView.findViewById<EditText>(R.id.amountEditText).text.toString()

            if (amount.isEmpty()) {
                Toast.makeText(context, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save to Firestore via ViewModel
            petId?.let { id ->
                viewModel.addFoodLog(id, "${amount}g")
                
                // Dismiss the bottom sheet
                bottomSheetDialog?.dismiss()
                
                // Show temporary success message while data is being fetched
                Toast.makeText(context, "Saving feeding record...", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog?.show()
    }
    
    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
    }
    
    private fun showLogs() {
        recyclerView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }
    
    companion object {
        private const val ARG_PET_ID = "pet_id"
        
        @JvmStatic
        fun newInstance(petId: String) =
            FoodLogsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PET_ID, petId)
                }
            }
    }
} 