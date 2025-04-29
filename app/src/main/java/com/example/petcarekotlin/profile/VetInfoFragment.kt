package com.example.petcarekotlin.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.petcarekotlin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.material.textfield.TextInputEditText

/**
 * Fragment for managing veterinarian information for a pet.
 * 
 * Firestore Structure:
 * pets/{petId} -> {
 *   vetInfo: {  // Note: capital "I" in vetInfo
 *     vetName: String,
 *     vetContact: String,
 *     nextAppointment: String,
 *     notes: String
 *   }
 * }
 */
class VetInfoFragment : Fragment() {
    
    private lateinit var vetNameEditText: TextInputEditText
    private lateinit var vetContactEditText: TextInputEditText
    private lateinit var saveButton: Button
    
    private val db = Firebase.firestore
    private var userId: String? = null
    private var petId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vet_info, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        vetNameEditText = view.findViewById(R.id.vetNameEditText)
        vetContactEditText = view.findViewById(R.id.vetContactEditText)
        saveButton = view.findViewById(R.id.saveButton)
        
        // Set initial values
        vetNameEditText.setText("Dr. Martinez")
        vetContactEditText.setText("(555) 123-4567")
        
        // Get current user ID from SharedPreferences
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
        userId = sharedPrefs.getString("CURRENT_USER_ID", "user1") // Default for testing
        
        // Show debug information
        val username = sharedPrefs.getString("CURRENT_USERNAME", "unknown")
        Toast.makeText(context, "Logged in as: $username, User ID: $userId", Toast.LENGTH_SHORT).show()
        
        // Get pet ID from arguments or try to fetch the default pet
        arguments?.let {
            petId = it.getString(ARG_PET_ID)
            if (petId != null) {
                Toast.makeText(context, "Loading vet info for pet: $petId", Toast.LENGTH_SHORT).show()
                loadVetData(petId!!)
            } else {
                // Try to fetch the default pet and its vet info
                Toast.makeText(context, "Fetching default pet for user: $userId", Toast.LENGTH_SHORT).show()
                fetchDefaultPet()
            }
        } ?: fetchDefaultPet() // If no arguments, try to fetch default pet
        
        // Set up save button click listener
        saveButton.setOnClickListener {
            saveVetData()
        }
    }
    
    private fun fetchDefaultPet() {
        // Show loading state
        setLoadingState(true)
        
        // Get the user document
        db.collection("users").document(userId ?: "user1")
            .get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    // Get the pets array from the user document
                    val petsArray = userDocument.get("pets") as? List<*>
                    
                    Toast.makeText(context, "User document found. Pets array: ${petsArray?.size ?: 0} items", Toast.LENGTH_SHORT).show()
                    
                    if (!petsArray.isNullOrEmpty()) {
                        // Get the first pet ID from the array
                        val firstPetId = petsArray[0] as? String
                        
                        if (firstPetId != null) {
                            Toast.makeText(context, "Found pet ID: $firstPetId", Toast.LENGTH_SHORT).show()
                            petId = firstPetId
                            
                            // Now load the vet data using this ID
                            loadVetData(firstPetId)
                        } else {
                            // Invalid pet ID
                            Toast.makeText(context, "Invalid pet ID found in array", Toast.LENGTH_SHORT).show()
                            setLoadingState(false)
                        }
                    } else {
                        // No pets found in the array
                        Toast.makeText(context, "No pets found for this user", Toast.LENGTH_SHORT).show()
                        setLoadingState(false)
                    }
                } else {
                    // User document doesn't exist
                    Toast.makeText(context, "User not found: $userId", Toast.LENGTH_SHORT).show()
                    setLoadingState(false)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
    }
    
    private fun loadVetData(petId: String) {
        // Show loading state
        setLoadingState(true)
        
        // Log for debugging
        Toast.makeText(context, "Loading vet data for pet ID: $petId", Toast.LENGTH_SHORT).show()
        
        // Get the pet document which contains the vetInfo map
        db.collection("pets").document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        // Get the vetInfo map from the document
                        val vetInfoMap = document.get("vetInfo") as? Map<*, *>
                        
                        if (vetInfoMap != null) {
                            // Extract vetName and vetContact from the map
                            val vetName = vetInfoMap["vetName"] as? String ?: ""
                            val vetContact = vetInfoMap["vetContact"] as? String ?: ""
                            
                            // Update UI with the extracted data
                            updateUIWithVetInfo(vetName, vetContact)
                            Toast.makeText(context, "Vet info loaded successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            // No vetInfo map found - create one with default values
                            Toast.makeText(context, "Creating new vet info for this pet", Toast.LENGTH_SHORT).show()
                            
                            // Default empty values
                            updateUIWithVetInfo("", "")
                            
                            // Create the vetInfo map in Firestore
                            val vetInfo = hashMapOf(
                                "vetName" to "",
                                "vetContact" to "",
                                "nextAppointment" to "",
                                "notes" to "",
                                "lastUpdated" to System.currentTimeMillis()
                            )
                            
                            // Update the pet document with the new vetInfo map
                            db.collection("pets").document(petId)
                                .update("vetInfo", vetInfo)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Vet info initialized successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to initialize vet info: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } catch (e: Exception) {
                        // Handle any parsing errors
                        Toast.makeText(context, "Error parsing vet data: ${e.message}", Toast.LENGTH_SHORT).show()
                        updateUIWithVetInfo("", "")
                    }
                } else {
                    // Pet document doesn't exist - create it with default values
                    Toast.makeText(context, "Pet document not found. Creating default pet document.", Toast.LENGTH_SHORT).show()
                    
                    // Create default pet data
                    val defaultPetData = hashMapOf(
                        "name" to "New Pet",
                        "species" to "Dog",
                        "breed" to "Mixed",
                        "age" to "0",
                        "weight" to "0",
                        "ownerId" to (userId ?: "user1"),
                        "vetInfo" to hashMapOf(
                            "vetName" to "",
                            "vetContact" to "",
                            "nextAppointment" to "",
                            "notes" to "",
                            "lastUpdated" to System.currentTimeMillis()
                        ),
                        "createdAt" to System.currentTimeMillis(),
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    
                    // Create the pet document with the specified ID
                    db.collection("pets").document(petId)
                        .set(defaultPetData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pet document created successfully", Toast.LENGTH_SHORT).show()
                            updateUIWithVetInfo("", "")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error creating pet document: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    
                    // Show empty fields
                    updateUIWithVetInfo("", "")
                }
                setLoadingState(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading pet data: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
    }
    
    private fun updateUIWithVetInfo(vetName: String, vetContact: String) {
        vetNameEditText.setText(vetName)
        vetContactEditText.setText(vetContact)
    }
    
    private fun saveVetData() {
        // Get input values
        val vetName = vetNameEditText.text.toString().trim()
        val vetContact = vetContactEditText.text.toString().trim()
        
        // Validate input
        if (vetName.isEmpty()) {
            vetNameEditText.error = "Veterinarian name is required"
            return
        }
        
        // If we don't have a pet ID, can't save
        if (petId == null) {
            Toast.makeText(context, "No pet selected to save vet info", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state
        setLoadingState(true)
        Toast.makeText(context, "Saving vet data for pet ID: $petId", Toast.LENGTH_SHORT).show()
        
        // First try to get existing vet info to preserve other fields
        db.collection("pets").document(petId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get existing vetInfo to preserve other fields
                    val existingVetInfo = document.get("vetInfo") as? Map<*, *>
                    val updatedVetInfo = mutableMapOf<String, Any>()
                    
                    // Copy existing values if any
                    if (existingVetInfo != null) {
                        existingVetInfo.forEach { (key, value) ->
                            if (key is String && value != null) {
                                updatedVetInfo[key] = value
                            }
                        }
                    }
                    
                    // Update with new values
                    updatedVetInfo["vetName"] = vetName
                    updatedVetInfo["vetContact"] = vetContact
                    updatedVetInfo["lastUpdated"] = System.currentTimeMillis()
                    
                    // Update in Firestore
                    db.collection("pets").document(petId!!)
                        .update("vetInfo", updatedVetInfo)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Veterinarian info saved successfully", Toast.LENGTH_SHORT).show()
                            setLoadingState(false)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error updating vet info: ${e.message}", Toast.LENGTH_SHORT).show()
                            // Try direct set as fallback
                            trySetVetInfo(vetName, vetContact)
                        }
                } else {
                    // Document doesn't exist, create new
                    trySetVetInfo(vetName, vetContact)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error getting pet document: ${e.message}", Toast.LENGTH_SHORT).show()
                // Try direct set as fallback
                trySetVetInfo(vetName, vetContact)
            }
    }
    
    private fun trySetVetInfo(vetName: String, vetContact: String) {
        // Create basic vetInfo with necessary fields
        val vetInfo = hashMapOf(
            "vetName" to vetName,
            "vetContact" to vetContact,
            "nextAppointment" to "",
            "notes" to "",
            "lastUpdated" to System.currentTimeMillis()
        )
        
        // Include the vetInfo field in the pet data map
        val petData = hashMapOf(
            "vetInfo" to vetInfo
        )
        
        // Merge with existing document to preserve other fields
        db.collection("pets").document(petId!!)
            .set(petData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Veterinarian info saved successfully", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
            .addOnFailureListener { innerE ->
                Toast.makeText(context, "Error setting vet info: ${innerE.message}", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
    }
    
    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            saveButton.isEnabled = false
            saveButton.text = "Saving..."
            
            // Disable fields during loading
            vetNameEditText.isEnabled = !isLoading
            vetContactEditText.isEnabled = !isLoading
        } else {
            saveButton.isEnabled = true
            saveButton.text = "Save Changes"
            
            // Re-enable fields
            vetNameEditText.isEnabled = true
            vetContactEditText.isEnabled = true
        }
    }
    
    companion object {
        private const val ARG_PET_ID = "pet_id"
        
        @JvmStatic
        fun newInstance(petId: String? = null) =
            VetInfoFragment().apply {
                arguments = Bundle().apply {
                    if (petId != null) {
                        putString(ARG_PET_ID, petId)
                    }
                }
            }
    }
} 