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
        notificationIcon = view.findViewById(R.id.notificationIcon)
        
        petNameEditText = view.findViewById(R.id.petNameEditText)
        speciesEditText = view.findViewById(R.id.speciesEditText)
        breedEditText = view.findViewById(R.id.breedEditText)
        ageEditText = view.findViewById(R.id.ageEditText)
        weightEditText = view.findViewById(R.id.weightEditText)
        saveButton = view.findViewById(R.id.saveButton)
        
        // Get current user ID (if available)
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "user1" // Default for testing
        
        // Get pet ID from arguments or try to fetch the default pet
        arguments?.let {
            petId = it.getString(ARG_PET_ID)
            if (petId != null) {
                loadPetData(petId!!)
            } else {
                // Try to fetch the default pet for this user
                fetchDefaultPet()
            }
        } ?: fetchDefaultPet() // If no arguments, try to fetch default pet
        
        // Set up click listeners
        setupClickListeners()
    }
    
    private fun fetchDefaultPet() {
        // Show loading state
        setLoadingState(true)
        
        // Query Firestore for the user's pets
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .limit(1) // Just get the first pet for now
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Get the first pet document
                    val document = documents.documents[0]
                    petId = document.id
                    // Load the pet data
                    loadPetDataFromDocument(document)
                } else {
                    // No pets found, show empty state
                    showEmptyState()
                }
                setLoadingState(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching pets: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
    }
    
    private fun loadPetData(petId: String) {
        // Show loading state
        setLoadingState(true)
        
        db.collection("pets").document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    loadPetDataFromDocument(document)
                } else {
                    showEmptyState()
                    Toast.makeText(context, "Pet not found", Toast.LENGTH_SHORT).show()
                }
                setLoadingState(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading pet data: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    petId = documentReference.id
                    Toast.makeText(context, "Pet added successfully", Toast.LENGTH_SHORT).show()
                    
                    // Update the header text
                    petNameHeader.text = name
                    petDetailsHeader.text = "$breed • $age years old"
                    
                    setLoadingState(false)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error adding pet: ${e.message}", Toast.LENGTH_SHORT).show()
                    setLoadingState(false)
                }
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