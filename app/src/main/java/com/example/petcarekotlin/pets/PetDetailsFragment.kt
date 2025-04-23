package com.example.petcarekotlin.pets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.petcarekotlin.R
import com.example.petcarekotlin.models.PetModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Fragment for displaying pet details
 */
class PetDetailsFragment : Fragment() {
    
    private lateinit var petImage: ImageView
    private lateinit var petNameTextView: TextView
    private lateinit var petBreedTextView: TextView
    private lateinit var petAgeTextView: TextView
    private lateinit var petWeightTextView: TextView
    private lateinit var petSpeciesTextView: TextView
    private lateinit var backButton: Button
    
    private val db = Firebase.firestore
    private var petId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pet_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        petImage = view.findViewById(R.id.petImage)
        petNameTextView = view.findViewById(R.id.petNameTextView)
        petBreedTextView = view.findViewById(R.id.petBreedTextView)
        petAgeTextView = view.findViewById(R.id.petAgeTextView)
        petWeightTextView = view.findViewById(R.id.petWeightTextView)
        petSpeciesTextView = view.findViewById(R.id.petSpeciesTextView)
        backButton = view.findViewById(R.id.backButton)
        
        // Set up edit button
        val editPetButton: Button = view.findViewById(R.id.editPetButton)
        editPetButton.setOnClickListener {
            navigateToEditPet()
        }
        
        // Get pet ID from arguments
        arguments?.let {
            petId = it.getString(ARG_PET_ID)
            if (petId != null) {
                loadPetDetails(petId!!)
            } else {
                showError("Pet ID not provided")
            }
        } ?: showError("No arguments provided")
        
        // Set up back button
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
    
    private fun loadPetDetails(petId: String) {
        // Show loading state
        showLoading(true)
        
        // Debug log
        Toast.makeText(context, "Loading pet details for ID: $petId", Toast.LENGTH_SHORT).show()
        
        // Query Firestore for pet details
        db.collection("pets").document(petId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                
                if (document.exists()) {
                    try {
                        // Extract pet data
                        val name = document.getString("name") ?: ""
                        val species = document.getString("species") ?: ""
                        val breed = document.getString("breed") ?: ""
                        val age = document.getString("age") ?: ""
                        val weight = document.getString("weight") ?: ""
                        
                        // Create pet model
                        val pet = PetModel(
                            petId = document.id,
                            name = name,
                            species = species,
                            breed = breed,
                            age = age,
                            weight = weight
                        )
                        
                        // Display pet details
                        displayPetDetails(pet)
                    } catch (e: Exception) {
                        showError("Error parsing pet data: ${e.message}")
                    }
                } else {
                    // Pet doesn't exist, create a new default pet
                    val defaultPet = PetModel(
                        petId = petId,
                        name = "Pet $petId",
                        species = "Unknown",
                        breed = "Unknown",
                        age = "0",
                        weight = "0",
                        createdAt = System.currentTimeMillis()
                    )
                    
                    // Create the pet in Firestore
                    createPetDocument(defaultPet)
                    
                    // Display the default pet
                    displayPetDetails(defaultPet)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Error loading pet details: ${e.message}")
            }
    }
    
    private fun createPetDocument(pet: PetModel) {
        // Create pet data map
        val petData = hashMapOf(
            "name" to pet.name,
            "species" to pet.species,
            "breed" to pet.breed,
            "age" to pet.age,
            "weight" to pet.weight,
            "ownerId" to "user1", // Default to user1 if not provided
            "createdAt" to System.currentTimeMillis(),
            "lastUpdated" to System.currentTimeMillis()
        )
        
        // Save to Firestore with exact petId
        db.collection("pets").document(pet.petId)
            .set(petData)
            .addOnSuccessListener {
                Toast.makeText(context, "Created new pet: ${pet.petId}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to create pet: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun displayPetDetails(pet: PetModel) {
        // Update UI with pet details
        petNameTextView.text = pet.name
        petSpeciesTextView.text = "Species: ${pet.species}"
        petBreedTextView.text = "Breed: ${pet.breed}"
        petAgeTextView.text = "Age: ${pet.age} years"
        petWeightTextView.text = "Weight: ${pet.weight} kg"
        
        // Set pet image (using placeholder for now)
        petImage.setImageResource(R.drawable.ic_pet_placeholder)
    }
    
    private fun showLoading(isLoading: Boolean) {
        // Set visibility based on loading state
        if (isLoading) {
            petNameTextView.text = "Loading..."
            // Add more loading indicators as needed
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        petNameTextView.text = "Error loading pet"
    }
    
    private fun navigateToEditPet() {
        if (petId == null) {
            Toast.makeText(context, "No pet ID available", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Navigate to PetInfoFragment for editing
        val fragment = com.example.petcarekotlin.profile.PetInfoFragment.newInstance(petId)
        
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    companion object {
        private const val ARG_PET_ID = "pet_id"
        
        @JvmStatic
        fun newInstance(petId: String) =
            PetDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PET_ID, petId)
                }
            }
    }
} 