package com.example.petcarekotlin.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.petcarekotlin.R
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.petcarekotlin.home.PetInfo

data class PetInfo(
    val name: String,
    val breed: String,
    val age: String,
    val lastFedTime: String,
    val fedBy: String,
    val foodRemaining: String,
    val estimatedDaysLeft: String,
    val vetDateTime: String,
    val vetDetails: String
)

class HomepageLogsFragment : Fragment() {

    private lateinit var petList: MutableList<PetInfo>
    private var currentIndex = 0
    private var addPetDialog: Dialog? = null
    private lateinit var addPetButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_homepage_logs, container, false)
        
        // Add the Add Pet button
        addPetButton = Button(requireContext())
        addPetButton.text = "+ Add New Pet"
        addPetButton.setBackgroundColor(resources.getColor(android.R.color.holo_orange_light))
        addPetButton.setTextColor(Color.WHITE)
        
        // Add the button to the layout
        (view as ViewGroup).addView(addPetButton, 
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        
        // Set click listener
        addPetButton.setOnClickListener {
            showAddPetDialog()
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        petList = mutableListOf(
            PetInfo(
                name = "Buddy",
                breed = "Golden Retriever",
                age = "3 years old",
                lastFedTime = "Today, 8:30 AM",
                fedBy = "Mike",
                foodRemaining = "1.2 kg",
                estimatedDaysLeft = "3 days left",
                vetDateTime = "May 15, 2025 - 2:00 PM",
                vetDetails = "Dr. Martinez - Annual Checkup"
            )
        )

        updateUI(view)
    }

    private fun updateUI(view: View) {
        if (petList.isEmpty()) {
            // Show a message when there are no pets
            view.findViewById<TextView>(R.id.petNameTextView).text = "No pets added yet"
            view.findViewById<TextView>(R.id.petDetailsTextView).text = "Add a pet to get started"
            
            // Hide other details
            view.findViewById<View>(R.id.lastFedLayout)?.visibility = View.GONE
            view.findViewById<View>(R.id.foodRemainingLayout)?.visibility = View.GONE
            view.findViewById<View>(R.id.vetAppointmentLayout)?.visibility = View.GONE
        } else {
            val pet = petList[currentIndex]
            
            // Show all details
            view.findViewById<View>(R.id.lastFedLayout)?.visibility = View.VISIBLE
            view.findViewById<View>(R.id.foodRemainingLayout)?.visibility = View.VISIBLE
            view.findViewById<View>(R.id.vetAppointmentLayout)?.visibility = View.VISIBLE

            view.findViewById<TextView>(R.id.petNameTextView).text = "🐾 ${pet.name}"
            view.findViewById<TextView>(R.id.petDetailsTextView).text = "${pet.breed} • ${pet.age}"
            view.findViewById<TextView>(R.id.lastFedTextView).text = pet.lastFedTime
            view.findViewById<TextView>(R.id.fedByTextView).text = pet.fedBy
            view.findViewById<TextView>(R.id.foodRemainingTextView).text = pet.foodRemaining
            view.findViewById<TextView>(R.id.estimatedDaysLeftTextView).text = pet.estimatedDaysLeft
            view.findViewById<TextView>(R.id.vetDateTimeTextView).text = pet.vetDateTime
            view.findViewById<TextView>(R.id.vetDetailsTextView).text = pet.vetDetails
        }
    }
    
    private fun showAddPetDialog() {
        // Create the dialog
        addPetDialog = Dialog(requireContext())
        addPetDialog?.setContentView(R.layout.dialog_add_new_pet)
        addPetDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Set up cancel button
        addPetDialog?.findViewById<Button>(R.id.cancelButton)?.setOnClickListener {
            addPetDialog?.dismiss()
        }
        
        // Set up close button
        addPetDialog?.findViewById<ImageView>(R.id.closeButton)?.setOnClickListener {
            addPetDialog?.dismiss()
        }
        
        // Set up add pet button
        addPetDialog?.findViewById<Button>(R.id.addPetButton)?.setOnClickListener {
            val petNameEditText = addPetDialog?.findViewById<EditText>(R.id.petNameEditText)
            val breedEditText = addPetDialog?.findViewById<EditText>(R.id.breedEditText)
            val ageEditText = addPetDialog?.findViewById<EditText>(R.id.ageEditText)
            val vetInfoEditText = addPetDialog?.findViewById<EditText>(R.id.vetInfoEditText)
            
            val petName = petNameEditText?.text.toString().trim()
            val breed = breedEditText?.text.toString().trim()
            val age = ageEditText?.text.toString().trim()
            
            // Validate required fields
            if (petName.isEmpty() || breed.isEmpty() || age.isEmpty()) {
                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Create a new pet
            val newPet = PetInfo(
                name = petName,
                breed = breed,
                age = "$age years old",
                lastFedTime = "Not fed yet",
                fedBy = "",
                foodRemaining = "0 kg",
                estimatedDaysLeft = "0 days left",
                vetDateTime = "No appointment scheduled",
                vetDetails = vetInfoEditText?.text.toString().trim()
            )
            
            // Add the pet to the list
            petList.add(newPet)
            currentIndex = petList.size - 1
            
            // Update the UI
            updateUI(requireView())
            
            // Dismiss the dialog
            addPetDialog?.dismiss()
            
            // Show success message
            Toast.makeText(context, "Pet added successfully", Toast.LENGTH_SHORT).show()
        }
        
        // Show the dialog
        addPetDialog?.show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        addPetDialog?.dismiss()
        addPetDialog = null
    }
} 