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
import android.content.Context

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
        
        // Set up switch pet button
        view.findViewById<Button>(R.id.switchPetButton).setOnClickListener {
            showPetSelectionDialog()
        }
        
        // Clear any existing data
        petList.clear()
        
        // Show loading state
        showLoading(view)
        
        // Get current user and fetch their pets
        getCurrentUser()
    }
    
    private fun showLoading(view: View) {
        view.findViewById<TextView>(R.id.petNameTextView).text = "Loading pets..."
        view.findViewById<TextView>(R.id.petDetailsTextView).text = "Please wait"
        view.findViewById<View>(R.id.lastFedLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.foodRemainingLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.vetAppointmentLayout)?.visibility = View.GONE
    }
    
    private fun getCurrentUser() {
        // Check if fragment is attached
        if (!isAdded) {
            updateUIForNoPets()
            return
        }

        // Get the current user from SharedPreferences (set during login)
        val userId: String?
        val username: String?
        
        try {
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
            userId = sharedPrefs.getString("CURRENT_USER_ID", null)
            username = sharedPrefs.getString("CURRENT_USERNAME", null)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Fragment not attached to activity", e)
            updateUIForNoPets()
            return
        }
        
        if (userId.isNullOrEmpty()) {
            Log.e(TAG, "No user ID found in SharedPreferences")
            updateUIForNoPets()
            return
        }
        
        // Fetch user document to get pets array
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                // Check if fragment is still attached
                if (!isAdded) {
                    return@addOnSuccessListener
                }

                if (document != null && document.exists()) {
                    // Get the pets array from user document
                    val petsArray = document.get("pets") as? List<*>
                    
                    if (petsArray.isNullOrEmpty()) {
                        // User has no pets
                        Log.d(TAG, "User has no pets")
                        updateUIForNoPets()
                    } else {
                        // Fetch details for all pets
                        petsArray.filterIsInstance<String>().forEach { petId ->
                            fetchPetDetails(petId)
                        }
                        
                        // Set current pet ID to SharedPreferences for use by other fragments
                        val firstPetId = petsArray.filterIsInstance<String>().firstOrNull()
                        if (firstPetId != null) {
                            try {
                                val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
                                sharedPrefs.edit().putString("CURRENT_PET_ID", firstPetId).apply()
                            } catch (e: IllegalStateException) {
                                Log.e(TAG, "Could not save current pet ID", e)
                            }
                        }
                    }
                } else {
                    // User document doesn't exist
                    Log.e(TAG, "User document not found")
                    updateUIForNoPets()
                }
            }
            .addOnFailureListener { exception ->
                // Check if fragment is still attached
                if (!isAdded) {
                    return@addOnFailureListener
                }

                Log.e(TAG, "Error getting user document", exception)
                context?.let { ctx ->
                    Toast.makeText(ctx, "Error loading user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
                updateUIForNoPets()
            }
    }
    
    private fun fetchPetDetails(petId: String) {
        // Check if fragment is attached
        if (!isAdded) {
            return
        }

        db.collection("pets")
            .document(petId)
            .get()
            .addOnSuccessListener { document ->
                // Check if fragment is still attached
                if (!isAdded) {
                    return@addOnSuccessListener
                }

                if (document != null && document.exists()) {
                    val petName = document.getString("name") ?: ""
                    val petBreed = document.getString("breed") ?: ""
                    
                    // Properly handle age field that could be a number
                    val petAge = when (val ageValue = document.get("age")) {
                        is Long -> ageValue.toString()
                        is Int -> ageValue.toString()
                        is Double -> ageValue.toInt().toString()
                        is String -> ageValue
                        else -> "0"
                    }
                    
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
                    
                    // Update UI with the first pet
                    if (petList.size == 1) {
                        currentIndex = 0
                        view?.let { updateUI(it) }
                    }
                } else {
                    // Pet document doesn't exist
                    Log.e(TAG, "Pet document not found")
                    if (petList.isEmpty()) {
                        updateUIForNoPets()
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Check if fragment is still attached
                if (!isAdded) {
                    return@addOnFailureListener
                }

                Log.e(TAG, "Error getting pet document", exception)
                context?.let { ctx ->
                    Toast.makeText(ctx, "Error loading pet data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
                if (petList.isEmpty()) {
                    updateUIForNoPets()
                }
            }
    }
    
    private fun fetchLastFeeding(petId: String, petIndex: Int) {
        // First get the pet document to access the foodLogs array
        db.collection("pets")
            .document(petId)
            .get()
            .addOnSuccessListener { petDoc ->
                val foodLogs = petDoc.get("foodLogs") as? List<String>
                
                if (foodLogs.isNullOrEmpty()) {
                    // No feeding logs found
                    if (petIndex < petList.size) {
                        val updatedPet = petList[petIndex].copy(
                            lastFedTime = "Not fed yet",
                            fedBy = ""
                        )
                        petList[petIndex] = updatedPet
                        
                        // If this is the current displayed pet, update UI
                        if (petIndex == currentIndex) {
                            view?.let { updateUI(it) }
                        }
                    }
                    return@addOnSuccessListener
                }
                
                // Get the last food log ID
                val lastFoodLogId = foodLogs.last()
                
                // Fetch the specific food log
                db.collection("foodLogs")
                    .document(lastFoodLogId)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            // Log doesn't exist anymore
                            handleNoFeedingLog(petIndex)
                            return@addOnSuccessListener
                        }
                        
                        val timestamp = doc.getTimestamp("createdAt")
                        val userId = doc.getString("userFullName") ?: ""
                        
                        if (timestamp != null) {
                            // Format the timestamp
                            val date = timestamp.toDate()
                            val currentTime = Date()
                            val diffInMillis = currentTime.time - date.time
                            val diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                            
                            val timeString = when {
                                diffInHours < 24 -> {
                                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    "Today, ${format.format(date)}"
                                }
                                diffInHours < 48 -> {
                                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    "Yesterday, ${format.format(date)}"
                                }
                                else -> {
                                    val format = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                    format.format(date)
                                }
                            }
                            
                            // Update the pet info
                            if (petIndex < petList.size) {
                                val updatedPet = petList[petIndex].copy(
                                    lastFedTime = timeString
                                )
                                
                                // Get the user who fed the pet
                                db.collection("foodLogs")
                                    .document(lastFoodLogId)
                                    .get()
                                    .addOnSuccessListener { userDoc ->
                                        val fullName = userDoc.getString("userFullName") ?: userDoc.getString("userName") ?: "Unknown"
                                        
                                        // Final update with username
                                        val finalPet = updatedPet.copy(
                                            fedBy = "by $fullName"
                                        )
                                        
                                        petList[petIndex] = finalPet
                                        
                                        // If this is the current displayed pet, update UI
                                        if (petIndex == currentIndex) {
                                            view?.let { updateUI(it) }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e(TAG, "Error getting user details", exception)
                                        // Still update the pet info even if we can't get the user details
                                        val finalPet = updatedPet.copy(
                                            fedBy = "by Unknown"
                                        )
                                        petList[petIndex] = finalPet
                                        if (petIndex == currentIndex) {
                                            view?.let { updateUI(it) }
                                        }
                                    }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error getting feeding log", exception)
                        handleNoFeedingLog(petIndex)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting pet document", exception)
                handleNoFeedingLog(petIndex)
            }
    }
    
    private fun handleNoFeedingLog(petIndex: Int) {
        // Helper function to handle the "Not fed yet" case
        if (petIndex < petList.size) {
            val updatedPet = petList[petIndex].copy(
                lastFedTime = "Not fed yet",
                fedBy = ""
            )
            petList[petIndex] = updatedPet
            if (petIndex == currentIndex) {
                view?.let { updateUI(it) }
            }
        }
    }
    
    private fun updateUIForNoPets() {
        val view = view ?: return
        view.findViewById<TextView>(R.id.petNameTextView).text = "No pets added yet"
        view.findViewById<TextView>(R.id.petDetailsTextView).text = "Add a pet to get started"
        view.findViewById<View>(R.id.lastFedLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.foodRemainingLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.vetAppointmentLayout)?.visibility = View.GONE
        view.findViewById<Button>(R.id.switchPetButton)?.visibility = View.GONE
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
            view.findViewById<Button>(R.id.switchPetButton)?.visibility = View.GONE
            
            // Clear current pet ID in SharedPreferences
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().remove("CURRENT_PET_ID").apply()
        } else {
            val pet = petList[currentIndex]
            
            // Show the switch button only if there are multiple pets
            view.findViewById<Button>(R.id.switchPetButton)?.visibility = 
                if (petList.size > 1) View.VISIBLE else View.GONE
            
            // Save current pet ID to SharedPreferences
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("CURRENT_PET_ID", pet.id).apply()
            
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
    
    private fun showPetSelectionDialog() {
        if (petList.isEmpty()) return
        
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_select_pet)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val listView = dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.petListView)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)
        
        // Set up RecyclerView with adapter for pet selection
        listView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        
        // Create a simple adapter for the pet list
        val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class PetViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
                val name: TextView = view.findViewById(R.id.petNameTextView)
                val details: TextView = view.findViewById(R.id.petDetailsTextView)
            }
            
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_simple_pet, parent, false)
                return PetViewHolder(view)
            }
            
            override fun getItemCount(): Int = petList.size
            
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val pet = petList[position]
                val viewHolder = holder as PetViewHolder
                
                viewHolder.name.text = pet.name
                viewHolder.details.text = "${pet.breed} • ${pet.age}"
                
                // Set click listener
                holder.itemView.setOnClickListener {
                    currentIndex = position
                    view?.let { updateUI(it) }
                    dialog.dismiss()
                }
            }
        }
        
        listView.adapter = adapter
        
        // Set close button click listener
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
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
            
            // Get the current user from SharedPreferences
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
            val userId = sharedPrefs.getString("CURRENT_USER_ID", null) ?: "user1"
            
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
                "ownerId" to userId,
                "familyId" to "family1", // In a real app, get the current user's family ID
                "foodRemaining" to 1000, // 1 kg default
                "dailyConsumption" to 200, // 200g per day default
                "vetInfo" to vetInfo,
                "createdAt" to Timestamp.now(),
                "foodLogs" to listOf<String>() // Initialize empty foodLogs array
            )
            
            // Add to Firestore
            db.collection("pets")
                .add(newPet)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Pet added with ID: ${documentReference.id}")
                    
                    // Add this pet to the user's pets array
                    val petId = documentReference.id
                    db.collection("users").document(userId)
                        .update("pets", com.google.firebase.firestore.FieldValue.arrayUnion(petId))
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pet added successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error adding pet to user", e)
                            Toast.makeText(context, "Pet created but not linked to user: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    
                    // Reload pets data
                    petList.clear()
                    getCurrentUser()
                    
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

    fun getCurrentPetId(): String? {
        return if (petList.isNotEmpty() && currentIndex < petList.size) {
            petList[currentIndex].id
        } else {
            null
        }
    }
} 