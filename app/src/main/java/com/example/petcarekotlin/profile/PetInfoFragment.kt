package com.example.petcarekotlin.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.petcarekotlin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PetInfoFragment : Fragment() {
    
    private lateinit var profileImage: ImageView
    private lateinit var editPhotoButton: ImageView
    private lateinit var petNameHeader: TextView
    private lateinit var petDetailsHeader: TextView
    private lateinit var notificationIcon: ImageView
    
    private lateinit var petNameEditText: EditText
    private lateinit var speciesEditText: EditText
    private lateinit var breedEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var weightEditText: EditText
    private lateinit var saveButton: Button
    
    private val db = Firebase.firestore
    private var petId: String? = null
    private var userId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pet_info, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI elements
        profileImage = view.findViewById(R.id.profileImage)
        editPhotoButton = view.findViewById(R.id.editPhotoButton)
        petNameHeader = view.findViewById(R.id.petNameHeader)
        petDetailsHeader = view.findViewById(R.id.petDetailsHeader)
        
        petNameEditText = view.findViewById(R.id.petNameEditText)
        speciesEditText = view.findViewById(R.id.speciesEditText)
        breedEditText = view.findViewById(R.id.breedEditText)
        ageEditText = view.findViewById(R.id.ageEditText)
        weightEditText = view.findViewById(R.id.weightEditText)
        saveButton = view.findViewById(R.id.saveButton)
        
        // Get current user ID from SharedPreferences
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
        userId = sharedPrefs.getString("CURRENT_USER_ID", "user1") // Default for testing
        
        // Show debug information
        val username = sharedPrefs.getString("CURRENT_USERNAME", "unknown")
        Toast.makeText(context, "Logged in as: $username, User ID: $userId", Toast.LENGTH_LONG).show()
        
        // Get pet ID from arguments or try to fetch the default pet
        arguments?.let {
            petId = it.getString(ARG_PET_ID)
            if (petId != null) {
                Toast.makeText(context, "Loading specified pet: $petId", Toast.LENGTH_SHORT).show()
                loadPetData(petId!!)
            } else {
                // Try to fetch the default pet for this user
                Toast.makeText(context, "Fetching default pet for user: $userId", Toast.LENGTH_SHORT).show()
                fetchDefaultPet()
            }
        } ?: fetchDefaultPet() // If no arguments, try to fetch default pet
        
        // Set up click listeners
        setupClickListeners()
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
                            
                            // Now load the pet data using this ID
                            loadPetData(firstPetId)
                        } else {
                            // Invalid pet ID
                            Toast.makeText(context, "Invalid pet ID found in array", Toast.LENGTH_SHORT).show()
                            createNewPet()
                        }
                    } else {
                        // No pets found in the array - create a new one
                        Toast.makeText(context, "No pets found for this user - creating new pet", Toast.LENGTH_SHORT).show()
                        createNewPet()
                    }
                } else {
                    // User document doesn't exist
                    Toast.makeText(context, "User not found: $userId", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                    setLoadingState(false)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
                setLoadingState(false)
            }
    }
    
    private fun createNewPet() {
        // Create a new pet ID
        val newPetId = "pet" + System.currentTimeMillis().toString().takeLast(5)
        
        // Create default pet data
        val defaultPetData = hashMapOf(
            "name" to "New Pet",
            "species" to "Dog",
            "breed" to "Mixed",
            "age" to "0",
            "weight" to "0",
            "ownerId" to (userId ?: "user1"),
            "createdAt" to System.currentTimeMillis(),
            "lastUpdated" to System.currentTimeMillis()
        )
        
        // Create the pet document
        db.collection("pets").document(newPetId)
            .set(defaultPetData)
            .addOnSuccessListener {
                // Set the pet ID
                petId = newPetId
                
                // Add this pet to the user's pets array
                updateUserPetsArray(newPetId)
                
                // Load the pet data
                loadPetDataFromDefault(defaultPetData)
                
                Toast.makeText(context, "Created new pet: $newPetId", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error creating pet: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
                setLoadingState(false)
            }
    }
    
    private fun loadPetDataFromDefault(petData: Map<String, Any>) {
        try {
            // Extract data from the map
            val name = petData["name"] as? String ?: ""
            val species = petData["species"] as? String ?: ""
            val breed = petData["breed"] as? String ?: ""
            val age = petData["age"] as? String ?: ""
            val weight = petData["weight"] as? String ?: ""
            
            // Update headers
            petNameHeader.text = name
            petDetailsHeader.text = "$breed • $age years old"
            
            // Update form fields
            petNameEditText.setText(name)
            speciesEditText.setText(species)
            breedEditText.setText(breed)
            ageEditText.setText(age)
            weightEditText.setText(weight)
            
            // Set pet image
            try {
                profileImage.setImageResource(R.drawable.ic_pet_placeholder)
            } catch (e: Exception) {
                // Fallback handling if resource not found
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error setting pet data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadPetData(petId: String) {
        // Show loading state
        setLoadingState(true)
        
        // Log the pet ID being loaded
        Toast.makeText(context, "Loading pet: $petId", Toast.LENGTH_SHORT).show()
        
        db.collection("pets").document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Pet document exists, load its data
                    loadPetDataFromDocument(document)
                    setLoadingState(false)
                } else {
                    // Pet document doesn't exist, create a new one with default values
                    Toast.makeText(context, "Creating new pet document for ID: $petId", Toast.LENGTH_SHORT).show()
                    
                    // Create default pet data
                    val defaultPetData = hashMapOf(
                        "name" to "New Pet",
                        "species" to "Dog",
                        "breed" to "Mixed",
                        "age" to "0",
                        "weight" to "0",
                        "ownerId" to (userId ?: "user1"),
                        "createdAt" to System.currentTimeMillis(),
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    
                    // Create the pet document with the specified ID
                    db.collection("pets").document(petId)
                        .set(defaultPetData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pet document created successfully", Toast.LENGTH_SHORT).show()
                            
                            // Now load the data from the new document
                            db.collection("pets").document(petId)
                                .get()
                                .addOnSuccessListener { newDocument ->
                                    loadPetDataFromDocument(newDocument)
                                    setLoadingState(false)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error loading new pet: ${e.message}", Toast.LENGTH_SHORT).show()
                                    showEmptyState()
                                    setLoadingState(false)
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error creating pet document: ${e.message}", Toast.LENGTH_SHORT).show()
                            showEmptyState()
                            setLoadingState(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading pet data: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
                setLoadingState(false)
            }
    }
    
    private fun loadPetDataFromDocument(document: com.google.firebase.firestore.DocumentSnapshot) {
        try {
            // Extract pet data fields with proper type conversion
            val name = document.getString("name") ?: ""
            val species = document.getString("species") ?: ""
            val breed = document.getString("breed") ?: ""
            
            // Handle numeric values properly
            val age = when (val ageValue = document.get("age")) {
                is Long -> ageValue.toString()
                is String -> ageValue
                is Double -> ageValue.toInt().toString()
                else -> ""
            }
            
            val weight = when (val weightValue = document.get("weight")) {
                is Double -> weightValue.toString()
                is Long -> weightValue.toString()
                is String -> weightValue
                else -> ""
            }
            
            // Update headers
            petNameHeader.text = name
            petDetailsHeader.text = "$breed • $age years old"
            
            // Update form fields
            petNameEditText.setText(name)
            speciesEditText.setText(species)
            breedEditText.setText(breed)
            ageEditText.setText(age)
            weightEditText.setText(weight)
            
            // Set pet image (using placeholder for now)
            try {
                profileImage.setImageResource(R.drawable.ic_pet_placeholder)
            } catch (e: Exception) {
                // Fallback handling if resource not found
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error parsing pet data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showEmptyState() {
        // Clear all fields and show default values
        petNameHeader.text = "No Pet Found"
        petDetailsHeader.text = "Add your pet details below"
        
        petNameEditText.setText("")
        speciesEditText.setText("")
        breedEditText.setText("")
        ageEditText.setText("")
        weightEditText.setText("")
    }
    
    private fun setLoadingState(isLoading: Boolean) {
        // Show/hide loading indicator and disable/enable form fields
        if (isLoading) {
            saveButton.isEnabled = false
            saveButton.text = "Loading..."
        } else {
            saveButton.isEnabled = true
            saveButton.text = "Save Changes"
        }
    }
    
    private fun setupClickListeners() {
        // Edit photo button click
        editPhotoButton.setOnClickListener {
            Toast.makeText(context, "Photo selection will be implemented", Toast.LENGTH_SHORT).show()
        }
        
        // Notification button click
        notificationIcon.setOnClickListener {
            Toast.makeText(context, "Notifications will be implemented", Toast.LENGTH_SHORT).show()
        }
        
        // Save button click
        saveButton.setOnClickListener {
            savePetData()
        }
    }
    
    private fun savePetData() {
        // Validate inputs
        val name = petNameEditText.text.toString().trim()
        val species = speciesEditText.text.toString().trim()
        val breed = breedEditText.text.toString().trim()
        val age = ageEditText.text.toString().trim()
        val weight = weightEditText.text.toString().trim()
        
        // Validate required fields
        var hasError = false
        
        if (name.isEmpty()) {
            petNameEditText.error = "Pet name is required"
            hasError = true
        }
        
        if (hasError) return
        
        // Disable save button to prevent duplicate submissions
        setLoadingState(true)
        
        // Create pet data map
        val petData = hashMapOf(
            "name" to name,
            "species" to species,
            "breed" to breed,
            "age" to age,
            "weight" to weight,
            "lastUpdated" to System.currentTimeMillis(),
            "ownerId" to userId
        )
        
        // Update or create pet document
        if (petId != null) {
            // Update existing pet
            db.collection("pets").document(petId!!)
                .update(petData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(context, "Pet updated successfully", Toast.LENGTH_SHORT).show()
                    
                    // Update the header text
                    petNameHeader.text = name
                    petDetailsHeader.text = "$breed • $age years old"
                    
                    // Ensure this pet is in the user's pets array
                    updateUserPetsArray(petId!!)
                    
                    setLoadingState(false)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error updating pet: ${e.message}", Toast.LENGTH_SHORT).show()
                    setLoadingState(false)
                }
        } else {
            // Create new pet
            db.collection("pets")
                .add(petData)
                .addOnSuccessListener { documentReference ->
                    val newPetId = documentReference.id
                    petId = newPetId
                    
                    Toast.makeText(context, "Pet added successfully", Toast.LENGTH_SHORT).show()
                    
                    // Update the header text
                    petNameHeader.text = name
                    petDetailsHeader.text = "$breed • $age years old"
                    
                    // Add this pet to the user's pets array
                    updateUserPetsArray(newPetId)
                    
                    setLoadingState(false)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error adding pet: ${e.message}", Toast.LENGTH_SHORT).show()
                    setLoadingState(false)
                }
        }
    }
    
    private fun updateUserPetsArray(petId: String) {
        // Get the current user document
        db.collection("users").document(userId ?: "user1")
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    // Get the current pets array
                    val currentPets = userDoc.get("pets") as? List<String> ?: listOf()
                    
                    // Check if the pet ID is already in the array
                    if (!currentPets.contains(petId)) {
                        // Add the pet ID to the array
                        val updatedPets = currentPets.toMutableList()
                        updatedPets.add(petId)
                        
                        // Update the user document
                        db.collection("users").document(userId ?: "user1")
                            .update("pets", updatedPets)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Pet added to user profile", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error updating user profile: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(context, "User document not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error accessing user document: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    companion object {
        private const val ARG_PET_ID = "pet_id"
        
        @JvmStatic
        fun newInstance(petId: String? = null) =
            PetInfoFragment().apply {
                arguments = Bundle().apply {
                    if (petId != null) {
                        putString(ARG_PET_ID, petId)
                    }
                }
            }
    }
} 