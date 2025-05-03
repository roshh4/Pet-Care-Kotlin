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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle

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
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_homepage_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Add lifecycle observer to refresh when fragment becomes visible
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
                val savedPetId = sharedPrefs.getString("CURRENT_PET_ID", null)
                
                // Only refresh if there's a new pet ID or the list is empty
                if (savedPetId != null && (petList.isEmpty() || getCurrentPetId() != savedPetId)) {
                    refreshData()
                }
            }
        }
        
        refreshData()
    }
    
    private fun showLoading(view: View) {
        view.findViewById<TextView>(R.id.petNameTextView).text = "Loading pets..."
        view.findViewById<TextView>(R.id.petDetailsTextView).text = "Please wait"
        view.findViewById<View>(R.id.lastFedLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.foodRemainingLayout)?.visibility = View.GONE
        view.findViewById<View>(R.id.vetAppointmentLayout)?.visibility = View.GONE
    }
    
    private fun getCurrentUser() {
        if (!isAdded) {
            updateUIForNoPets()
            return
        }

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

        isLoading = true
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    if (userDoc.exists()) {
                        val petsArray = userDoc.get("pets") as? List<*>
                        
                        if (petsArray.isNullOrEmpty()) {
                            Log.d(TAG, "User has no pets")
                            updateUIForNoPets()
                        } else {
                            val petIds = petsArray.filterIsInstance<String>()
                            
                            // Get the saved pet ID or use the first pet
                            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
                            val savedPetId = sharedPrefs.getString("CURRENT_PET_ID", null)
                            
                            // If we have a saved pet ID and it's in the list, use it
                            // Otherwise use the first pet
                            val selectedPetId = if (savedPetId != null && petIds.contains(savedPetId)) {
                                savedPetId
                            } else {
                                petIds.firstOrNull()?.also { firstPetId ->
                                    // Save the first pet ID as current
                                    sharedPrefs.edit().putString("CURRENT_PET_ID", firstPetId).apply()
                                }
                            }
                            
                            // Load all pets but set the current index based on the selected pet
                            petIds.forEach { petId ->
                                fetchPetDetails(petId)
                            }
                        }
                    } else {
                        Log.e(TAG, "User document not found")
                        updateUIForNoPets()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Log.e(TAG, "Error getting user document", e)
                    updateUIForNoPets()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }
    
    private fun fetchPetDetails(petId: String) {
        if (!isAdded) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val document = db.collection("pets").document(petId).get().await()
                
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    if (document.exists()) {
                        val petName = document.getString("name") ?: ""
                        val petBreed = document.getString("breed") ?: ""
                        val petAge = when (val ageValue = document.get("age")) {
                            is Long -> ageValue.toString()
                            is Int -> ageValue.toString()
                            is Double -> ageValue.toInt().toString()
                            is String -> ageValue
                            else -> "0"
                        }
                        
                        val foodRemaining = document.getLong("foodRemaining") ?: 0
                        val dailyConsumption = document.getLong("dailyConsumption") ?: 1
                        
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
                        
                        val daysLeft = if (dailyConsumption > 0) {
                            (foodRemaining / dailyConsumption).toString()
                        } else {
                            "N/A"
                        }
                        
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
                        
                        petList.add(pet)
                        fetchLastFeeding(petId, petList.size - 1)
                        
                        // Get the current pet ID from SharedPreferences
                        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
                        val currentPetId = sharedPrefs.getString("CURRENT_PET_ID", null)
                        
                        // If this is the current pet, update the UI
                        if (currentPetId == petId) {
                            currentIndex = petList.size - 1
                            view?.let { updateUI(it) }
                        }
                    } else {
                        Log.e(TAG, "Pet document not found")
                        if (petList.isEmpty()) {
                            updateUIForNoPets()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Log.e(TAG, "Error getting pet document", e)
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error loading pet data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    if (petList.isEmpty()) {
                        updateUIForNoPets()
                    }
                }
            }
        }
    }
    
    private fun fetchLastFeeding(petId: String, petIndex: Int) {
        if (!isAdded) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val petDoc = db.collection("pets").document(petId).get().await()
                val foodLogs = petDoc.get("foodLogs") as? List<String>
                
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    
                    if (foodLogs.isNullOrEmpty()) {
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
                        return@withContext
                    }
                    
                    val lastFoodLogId = foodLogs.last()
                    val doc = db.collection("foodLogs").document(lastFoodLogId).get().await()
                    
                    if (!doc.exists()) {
                        handleNoFeedingLog(petIndex)
                        return@withContext
                    }
                    
                    val timestamp = doc.getTimestamp("createdAt")
                    if (timestamp != null) {
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
                        
                        if (petIndex < petList.size) {
                            val updatedPet = petList[petIndex].copy(
                                lastFedTime = timeString
                            )
                            
                            try {
                                val userDoc = db.collection("foodLogs").document(lastFoodLogId).get().await()
                                val fullName = userDoc.getString("userFullName") ?: userDoc.getString("userName") ?: "Unknown"
                                
                                val finalPet = updatedPet.copy(
                                    fedBy = "by $fullName"
                                )
                                
                                withContext(Dispatchers.Main) {
                                    if (!isAdded) return@withContext
                                    petList[petIndex] = finalPet
                                    if (petIndex == currentIndex) {
                                        view?.let { updateUI(it) }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting user details", e)
                                val finalPet = updatedPet.copy(
                                    fedBy = "by Unknown"
                                )
                                withContext(Dispatchers.Main) {
                                    if (!isAdded) return@withContext
                                    petList[petIndex] = finalPet
                                    if (petIndex == currentIndex) {
                                        view?.let { updateUI(it) }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Log.e(TAG, "Error getting feeding log", e)
                    handleNoFeedingLog(petIndex)
                }
            }
        }
    }
    
    private fun handleNoFeedingLog(petIndex: Int) {
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
    }

    private fun updateUI(view: View) {
        if (petList.isEmpty()) {
            view.findViewById<TextView>(R.id.petNameTextView).text = "No pets added yet"
            view.findViewById<TextView>(R.id.petDetailsTextView).text = "Add a pet to get started"
            
            view.findViewById<View>(R.id.lastFedLayout)?.visibility = View.GONE
            view.findViewById<View>(R.id.foodRemainingLayout)?.visibility = View.GONE
            view.findViewById<View>(R.id.vetAppointmentLayout)?.visibility = View.GONE
            
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().remove("CURRENT_PET_ID").apply()
        } else {
            val pet = petList[currentIndex]
            
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("CURRENT_PET_ID", pet.id).apply()
            
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
        addPetDialog = Dialog(requireContext())
        addPetDialog?.setContentView(R.layout.dialog_add_new_pet)
        addPetDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        addPetDialog?.findViewById<Button>(R.id.cancelButton)?.setOnClickListener {
            addPetDialog?.dismiss()
        }
        
        addPetDialog?.findViewById<ImageView>(R.id.closeButton)?.setOnClickListener {
            addPetDialog?.dismiss()
        }
        
        addPetDialog?.findViewById<Button>(R.id.addPetButton)?.setOnClickListener {
            val petNameEditText = addPetDialog?.findViewById<EditText>(R.id.petNameEditText)
            val breedEditText = addPetDialog?.findViewById<EditText>(R.id.breedEditText)
            val ageEditText = addPetDialog?.findViewById<EditText>(R.id.ageEditText)
            val vetInfoEditText = addPetDialog?.findViewById<EditText>(R.id.vetInfoEditText)
            
            val petName = petNameEditText?.text.toString().trim()
            val breed = breedEditText?.text.toString().trim()
            val ageText = ageEditText?.text.toString().trim()
            
            if (petName.isEmpty() || breed.isEmpty() || ageText.isEmpty()) {
                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val age = ageText.toIntOrNull()
            if (age == null) {
                Toast.makeText(context, "Please enter a valid age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            addPetDialog?.findViewById<Button>(R.id.addPetButton)?.isEnabled = false
            
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
            val userId = sharedPrefs.getString("CURRENT_USER_ID", null) ?: "user1"
            
            val vetInfo = hashMapOf<String, Any>()
            val vetInfoText = vetInfoEditText?.text.toString().trim()
            if (vetInfoText.isNotEmpty()) {
                vetInfo["vetName"] = vetInfoText
                vetInfo["notes"] = "Regular checkup"
                
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.MONTH, 6)
                vetInfo["nextAppointment"] = Timestamp(calendar.time)
            }
            
            val newPet = hashMapOf(
                "name" to petName,
                "breed" to breed,
                "age" to age,
                "ownerId" to userId,
                "familyId" to "family1",
                "foodRemaining" to 1000,
                "dailyConsumption" to 200,
                "vetInfo" to vetInfo,
                "createdAt" to Timestamp.now(),
                "foodLogs" to listOf<String>()
            )
            
            db.collection("pets")
                .add(newPet)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Pet added with ID: ${documentReference.id}")
                    
                    val petId = documentReference.id
                    db.collection("users").document(userId)
                        .update("pets", com.google.firebase.firestore.FieldValue.arrayUnion(petId))
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pet added successfully", Toast.LENGTH_SHORT).show()
                            addPetDialog?.dismiss()
                            refreshData()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error adding pet to user", e)
                            Toast.makeText(context, "Error adding pet: ${e.message}", Toast.LENGTH_SHORT).show()
                            addPetDialog?.findViewById<Button>(R.id.addPetButton)?.isEnabled = true
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding pet", e)
                    Toast.makeText(context, "Error adding pet: ${e.message}", Toast.LENGTH_SHORT).show()
                    addPetDialog?.findViewById<Button>(R.id.addPetButton)?.isEnabled = true
                }
        }
        
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

    fun refreshData() {
        if (!isAdded) return
        
        petList.clear()
        view?.let { view ->
            showLoading(view)
            getCurrentUser()
        }
    }
} 