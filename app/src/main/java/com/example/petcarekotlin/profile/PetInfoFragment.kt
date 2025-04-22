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
    private var currentUserId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pet_info, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get current user id
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
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
        
        // Get pet ID from arguments or fetch default pet
        arguments?.let {
            petId = it.getString(ARG_PET_ID)
        }
        
        if (petId != null) {
            loadPetById(petId!!)
        } else {
            fetchDefaultPet()
        }
        
        // Set up click listeners
        setupClickListeners()
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
    
    private fun fetchDefaultPet() {
        // If no specific pet ID provided, fetch the first pet associated with the current user
        if (currentUserId == null) {
            showError("User not authenticated")
            return
        }
        
        // Show loading state
        setLoadingState(true)
        
        db.collection("pets")
            .whereEqualTo("ownerId", currentUserId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val petDocument = documents.documents[0]
                    petId = petDocument.id
                    
                    // Load the pet data
                    updateUIWithPetData(petDocument.data)
                } else {
                    // No pets found, show empty state
                    showEmptyState()
                }
                
                setLoadingState(false)
            }
            .addOnFailureListener { e ->
                showError("Error fetching pets: ${e.message}")
                setLoadingState(false)
            }
    }
    
    private fun loadPetById(petId: String) {
        // Show loading state
        setLoadingState(true)
        
        db.collection("pets").document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Update UI with pet data
                    updateUIWithPetData(document.data)
                } else {
                    showError("Pet not found")
                }
                
                setLoadingState(false)
            }
            .addOnFailureListener { e ->
                showError("Error loading pet data: ${e.message}")
                setLoadingState(false)
            }
    }
    
    private fun updateUIWithPetData(petData: Map<String, Any>?) {
        if (petData == null) return
        
        try {
            // Extract pet data with safe casting
            val name = petData["name"] as? String ?: ""
            val species = petData["species"] as? String ?: ""
            val breed = petData["breed"] as? String ?: ""
            
            // Handle numeric fields which could be different types
            val age = when (val ageValue = petData["age"]) {
                is Long -> ageValue.toString()
                is Int -> ageValue.toString()
                is String -> ageValue
                else -> ""
            }
            
            val weight = when (val weightValue = petData["weight"]) {
                is Double -> weightValue.toString()
                is Float -> weightValue.toString()
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
            showError("Error parsing pet data: ${e.message}")
        }
    }
    
    private fun savePetData() {
        // Validate inputs
        val name = petNameEditText.text.toString().trim()
        val species = speciesEditText.text.toString().trim()
        val breed = breedEditText.text.toString().trim()
        val ageText = ageEditText.text.toString().trim()
        val weightText = weightEditText.text.toString().trim()
        
        // Validate required fields
        var hasError = false
        
        if (name.isEmpty()) {
            petNameEditText.error = "Pet name is required"
            hasError = true
        }
        
        if (species.isEmpty()) {
            speciesEditText.error = "Species is required"
            hasError = true
        }
        
        if (breed.isEmpty()) {
            breedEditText.error = "Breed is required"
            hasError = true
        }
        
        if (ageText.isEmpty()) {
            ageEditText.error = "Age is required"
            hasError = true
        }
        
        if (weightText.isEmpty()) {
            weightEditText.error = "Weight is required"
            hasError = true
        }
        
        if (hasError) return
        
        // Convert numeric values properly
        val age = ageText.toIntOrNull()
        if (age == null) {
            ageEditText.error = "Please enter a valid number"
            return
        }
        
        val weight = weightText.toDoubleOrNull()
        if (weight == null) {
            weightEditText.error = "Please enter a valid number"
            return
        }
        
        // Disable save button to prevent duplicate submissions
        saveButton.isEnabled = false
        
        // Create pet data map
        val petData = hashMapOf(
            "name" to name,
            "species" to species,
            "breed" to breed,
            "age" to age,
            "weight" to weight,
            "lastUpdated" to System.currentTimeMillis()
        )
        
        // If creating a new pet, add the owner ID
        if (petId == null && currentUserId != null) {
            petData["ownerId"] = currentUserId!!
            petData["createdAt"] = System.currentTimeMillis()
        }
        
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
                    
                    saveButton.isEnabled = true
                }
                .addOnFailureListener { e ->
                    showError("Error updating pet: ${e.message}")
                    saveButton.isEnabled = true
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
                    
                    saveButton.isEnabled = true
                }
                .addOnFailureListener { e ->
                    showError("Error adding pet: ${e.message}")
                    saveButton.isEnabled = true
                }
        }
    }
    
    private fun setLoadingState(isLoading: Boolean) {
        // TODO: Add progress indicator if needed
        if (isLoading) {
            saveButton.isEnabled = false
        } else {
            saveButton.isEnabled = true
        }
    }
    
    private fun showEmptyState() {
        // Reset fields to show empty state
        petNameHeader.text = "New Pet"
        petDetailsHeader.text = "Add your pet details"
        
        petNameEditText.setText("")
        speciesEditText.setText("")
        breedEditText.setText("")
        ageEditText.setText("")
        weightEditText.setText("")
    }
    
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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