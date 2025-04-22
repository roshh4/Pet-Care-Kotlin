package com.example.petcarekotlin.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.petcarekotlin.R
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Button
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PetInfo(
    val id: String = "",
    val name: String = "",
    val breed: String = "",
    val age: String = "",
    val lastFedTime: String = "Not fed yet",
    val fedBy: String = "",
    val foodRemaining: String = "0 kg",
    val estimatedDaysLeft: String = "0 days left",
    val vetDateTime: String = "No appointment scheduled",
    val vetDetails: String = ""
)

class HomepageLogsFragment : Fragment() {

    private var petList: MutableList<PetInfo> = mutableListOf()
    private var currentIndex = 0
    private var addPetDialog: Dialog? = null
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "HomepageLogsFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_homepage_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Clear any existing data
        petList.clear()
        
        // Show loading state
        showLoading(view)
        
        // Fetch pets from Firestore
        fetchPetsData()
    }
    
    private fun showLoading(view: View) {
        view.findViewById<TextView>(R.id.petNameTextView).text = "Loading pets..."
        view.findViewById<TextView>(R.id.petDetailsTextView).text = "Please wait"
        view.findViewById<View>(R.id.lastFedLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.foodRemainingLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.vetAppointmentLayout)?.visibility = View.GONE
    }
    
    private fun fetchPetsData() {
        // For now, assuming we're working with family1 from our schema
        // In a real app, you would get the current user's familyId from their profile
        val familyId = "family1"
        
        db.collection("pets")
            .whereEqualTo("familyId", familyId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No pets found
                    updateUIForNoPets()
                } else {
                    for (document in documents) {
                        val petId = document.id
                        val petName = document.getString("name") ?: ""
                        val petBreed = document.getString("breed") ?: ""
                        val petAge = document.getLong("age")?.toString() ?: "0"
                        val foodRemaining = document.getLong("foodRemaining") ?: 0
                        val dailyConsumption = document.getLong("dailyConsumption") ?: 1
                        
                        // Get vet info (stored as a map)
                        val vetInfo = document.get("vetInfo") as? Map<String, Any>
                        var vetDateTime = "No appointment scheduled"
                        var vetDetails = ""
                        
                        if (vetInfo != null) {
                            val nextAppointment = vetInfo["nextAppointment"] as? Timestamp
                            val vetName = vetInfo["vetName"] as? String ?: ""
                            val notes = vetInfo["notes"] as? String ?: ""
                            
                            if (nextAppointment != null) {
                                val dateFormat = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.getDefault())
                                vetDateTime = dateFormat.format(nextAppointment.toDate())
                            }
                            
                            vetDetails = "$vetName - $notes"
                        }
                        
                        // Calculate estimated days left
                        val daysLeft = if (dailyConsumption > 0) {
                            (foodRemaining / dailyConsumption).toString()
                        } else {
                            "N/A"
                        }
                        
                        // Create pet object
                        val pet = PetInfo(
                            id = petId,
                            name = petName,
                            breed = petBreed,
                            age = "$petAge years old",
                            foodRemaining = "${foodRemaining / 1000.0} kg",
                            estimatedDaysLeft = "$daysLeft days left",
                            vetDateTime = vetDateTime,
                            vetDetails = vetDetails
                        )
                        
                        // Add to our list
                        petList.add(pet)
                        
                        // Get the most recent feeding log
                        fetchLastFeeding(petId, petList.size - 1)
                    }
                    
                    // Update UI with the first pet
                    if (petList.isNotEmpty()) {
                        updateUI(requireView())
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting pets", exception)
                Toast.makeText(context, "Error loading pets: ${exception.message}", Toast.LENGTH_SHORT).show()
                updateUIForNoPets()
            }
    }
    
    private fun fetchLastFeeding(petId: String, petIndex: Int) {
        db.collection("feedingLogs")
            .whereEqualTo("petId", petId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val timestamp = doc.getTimestamp("timestamp")
                    val userId = doc.getString("userId") ?: ""
                    
                    if (timestamp != null) {
                        // Format the timestamp
                        val date = timestamp.toDate()
                        val currentTime = Date()
                        val diffInMillis = currentTime.time - date.time
                        val diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                        
                        val timeString = when {
                            diffInHours < 24 -> {
                                val format = SimpleDateFormat("h:mm a", Locale.getDefault())
                                "Today, ${format.format(date)}"
                            }
                            diffInHours < 48 -> {
                                val format = SimpleDateFormat("h:mm a", Locale.getDefault())
                                "Yesterday, ${format.format(date)}"
                            }
                            else -> {
                                val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                format.format(date)
                            }
                        }
                        
                        // Update the pet info
                        if (petIndex < petList.size) {
                            val updatedPet = petList[petIndex].copy(
                                lastFedTime = timeString
                            )
                            
                            // Get the user who fed the pet
                            db.collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val username = userDoc.getString("username") ?: "Unknown"
                                    
                                    // Final update with username
                                    val finalPet = updatedPet.copy(
                                        fedBy = "by $username"
                                    )
                                    
                                    petList[petIndex] = finalPet
                                    
                                    // If this is the current displayed pet, update UI
                                    if (petIndex == currentIndex) {
                                        updateUI(requireView())
                                    }
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting feeding logs", exception)
            }
    }
    
    private fun updateUIForNoPets() {
        val view = view ?: return
        view.findViewById<TextView>(R.id.petNameTextView).text = "No pets added yet"
        view.findViewById<TextView>(R.id.petDetailsTextView).text = "Add a pet to get started"
        view.findViewById<View>(R.id.lastFedLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.foodRemainingLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.vetAppointmentLayout)?.visibility = View.GONE
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
    
    fun showAddPetDialog() {
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
            val ageText = ageEditText?.text.toString().trim()
            
            // Validate required fields
            if (petName.isEmpty() || breed.isEmpty() || ageText.isEmpty()) {
                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Parse age as number
            val age = ageText.toIntOrNull()
            if (age == null) {
                Toast.makeText(context, "Please enter a valid age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Disable button to prevent multiple submissions
            addPetDialog?.findViewById<Button>(R.id.addPetButton)?.isEnabled = false
            
            // Gather vet information
            val vetInfo = hashMapOf<String, Any>()
            val vetInfoText = vetInfoEditText?.text.toString().trim()
            if (vetInfoText.isNotEmpty()) {
                vetInfo["vetName"] = vetInfoText
                vetInfo["notes"] = "Regular checkup"
                
                // Set a default next appointment in 6 months
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.MONTH, 6)
                vetInfo["nextAppointment"] = Timestamp(calendar.time)
            }
            
            // Create the new pet document
            val newPet = hashMapOf(
                "name" to petName,
                "breed" to breed,
                "age" to age,
                "ownerId" to "user1", // In a real app, get the current user's ID
                "familyId" to "family1", // In a real app, get the current user's family ID
                "foodRemaining" to 1000, // 1 kg default
                "dailyConsumption" to 200, // 200g per day default
                "vetInfo" to vetInfo,
                "createdAt" to Timestamp.now()
            )
            
            // Add to Firestore
            db.collection("pets")
                .add(newPet)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Pet added with ID: ${documentReference.id}")
                    Toast.makeText(context, "Pet added successfully", Toast.LENGTH_SHORT).show()
                    
                    // Reload pets data
                    petList.clear()
                    fetchPetsData()
                    
                    // Dismiss the dialog
                    addPetDialog?.dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding pet", e)
                    Toast.makeText(context, "Error adding pet: ${e.message}", Toast.LENGTH_SHORT).show()
                    
                    // Re-enable the button
                    addPetDialog?.findViewById<Button>(R.id.addPetButton)?.isEnabled = true
                }
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