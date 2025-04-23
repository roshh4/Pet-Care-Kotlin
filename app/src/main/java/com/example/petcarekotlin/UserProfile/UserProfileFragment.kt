package com.example.petcarekotlin.UserProfile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.core.AppPageFragment
import com.example.petcarekotlin.auth.LoginPageFragment
import com.example.petcarekotlin.R
import com.example.petcarekotlin.home.HomepageLogsFragment
import com.example.petcarekotlin.models.PetModel
import com.example.petcarekotlin.pets.PetsListAdapter
import com.example.petcarekotlin.profile.PetInfoFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UserProfileFragment : Fragment() {
    
    private lateinit var petListContainer: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyPetsText: TextView
    private lateinit var adapter: PetsListAdapter
    
    private val db = Firebase.firestore
    private var userId = "user1" // Default user ID for testing

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for user profile sidebar
        val view = inflater.inflate(R.layout.fragment_user_profile, container, false)
        
        // Initialize views
        petListContainer = view.findViewById(R.id.petsListContainer)
        recyclerView = view.findViewById(R.id.petsRecyclerView)
        emptyPetsText = view.findViewById(R.id.emptyPetsText)
        
        // Get user ID from arguments if available
        arguments?.getString("user_id")?.let {
            userId = it
        }
        
        setupClickListeners(view)
        setupPetsList()
        loadUserPets()
        
        return view
    }
    
    private fun setupPetsList() {
        // Set up RecyclerView with adapter
        adapter = PetsListAdapter(emptyList()) { pet ->
            navigateToPetDetail(pet)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
    
    private fun navigateToPetDetail(pet: PetModel) {
        // Navigate to pet detail and close the drawer
        closeDrawer()
        
        // Create and show the PetDetailsFragment with the selected pet ID
        val fragment = com.example.petcarekotlin.pets.PetDetailsFragment.newInstance(pet.petId)
        
        // Use the parent activity's fragment manager to replace the main container
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun loadUserPets() {
        // Show loading state
        showLoadingState()
        
        // First try to load from the pets collection
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { petsSnapshot ->
                if (!petsSnapshot.isEmpty) {
                    // Convert documents to pet models
                    val petsList = petsSnapshot.documents.mapNotNull { doc ->
                        try {
                            PetModel(
                                petId = doc.id,
                                name = doc.getString("name") ?: "",
                                species = doc.getString("species") ?: "",
                                breed = doc.getString("breed") ?: "",
                                age = doc.getString("age") ?: "",
                                weight = doc.getString("weight") ?: "",
                                ownerId = doc.getString("ownerId") ?: "",
                                photoUrl = doc.getString("photoUrl"),
                                createdAt = doc.getLong("createdAt") ?: 0,
                                updatedAt = doc.getLong("lastUpdated") ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (petsList.isNotEmpty()) {
                        showPets(petsList)
                    } else {
                        tryLoadPetsFromUserDocument()
                    }
                } else {
                    // No pets found in pets collection, try user document
                    tryLoadPetsFromUserDocument()
                }
            }
            .addOnFailureListener {
                tryLoadPetsFromUserDocument()
            }
    }
    
    private fun tryLoadPetsFromUserDocument() {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    // Get the pets list field
                    val petIds = userDoc.get("pets") as? List<*>
                    
                    if (!petIds.isNullOrEmpty()) {
                        // We have pet IDs, try to load them
                        val stringPetIds = petIds.mapNotNull { it as? String }
                        if (stringPetIds.isNotEmpty()) {
                            Toast.makeText(context, "Found ${stringPetIds.size} pets", Toast.LENGTH_SHORT).show()
                            // Load pets data from the pet documents
                            loadPetsData(stringPetIds)
                        } else {
                            showEmptyState()
                        }
                    } else {
                        // No pets list, try other fields
                        loadPetsFromUserField(userDoc)
                    }
                } else {
                    showEmptyState()
                }
            }
            .addOnFailureListener {
                showEmptyState()
            }
    }
    
    private fun loadPetsData(petIds: List<String>) {
        if (petIds.isEmpty()) {
            showEmptyState()
            return
        }
        
        // Debug log
        Toast.makeText(context, "Trying to load pets: ${petIds.joinToString()}", Toast.LENGTH_LONG).show()
        
        val petsList = mutableListOf<PetModel>()
        var completedQueries = 0
        
        for (petId in petIds) {
            db.collection("pets").document(petId)
                .get()
                .addOnSuccessListener { petDoc ->
                    // Log the result for each pet
                    if (petDoc.exists()) {
                        Toast.makeText(context, "Pet found: ${petDoc.id}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Pet not found: $petId", Toast.LENGTH_SHORT).show()
                    }
                    
                    if (petDoc.exists()) {
                        try {
                            val pet = PetModel(
                                petId = petDoc.id,
                                name = petDoc.getString("name") ?: "",
                                species = petDoc.getString("species") ?: "",
                                breed = petDoc.getString("breed") ?: "",
                                age = petDoc.getString("age") ?: "",
                                weight = petDoc.getString("weight") ?: "",
                                ownerId = petDoc.getString("ownerId") ?: userId,
                                photoUrl = petDoc.getString("photoUrl"),
                                createdAt = petDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = petDoc.getLong("lastUpdated") ?: System.currentTimeMillis()
                            )
                            petsList.add(pet)
                        } catch (e: Exception) {
                            // If there's an error parsing the pet data, create a basic placeholder
                            val pet = PetModel(
                                petId = petDoc.id,
                                name = petDoc.id,
                                ownerId = userId
                            )
                            petsList.add(pet)
                        }
                    } else {
                        // If pet document doesn't exist, let's create it with default values
                        val newPet = PetModel(
                            petId = petId,
                            name = "Pet $petId",
                            species = "Unknown",
                            breed = "Unknown",
                            age = "0",
                            weight = "0",
                            ownerId = userId,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        petsList.add(newPet)
                        
                        // Create the pet document in Firestore
                        createPetDocument(newPet)
                    }
                    
                    completedQueries++
                    if (completedQueries == petIds.size) {
                        if (petsList.isNotEmpty()) {
                            showPets(petsList)
                        } else {
                            showEmptyState()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading pet: $petId - ${e.message}", Toast.LENGTH_SHORT).show()
                    
                    // Create a placeholder pet
                    val pet = PetModel(
                        petId = petId,
                        name = "Pet $petId",
                        ownerId = userId
                    )
                    petsList.add(pet)
                    
                    completedQueries++
                    if (completedQueries == petIds.size) {
                        if (petsList.isNotEmpty()) {
                            showPets(petsList)
                        } else {
                            showEmptyState()
                        }
                    }
                }
        }
    }
    
    private fun createPetDocument(pet: PetModel) {
        // Create a map of pet data
        val petData = hashMapOf(
            "name" to pet.name,
            "species" to pet.species,
            "breed" to pet.breed,
            "age" to pet.age,
            "weight" to pet.weight,
            "ownerId" to userId,
            "createdAt" to System.currentTimeMillis(),
            "lastUpdated" to System.currentTimeMillis()
        )
        
        // Create the pet document in Firestore with the exact petId
        db.collection("pets").document(pet.petId)
            .set(petData)
            .addOnSuccessListener {
                Toast.makeText(context, "Created pet document: ${pet.petId}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to create pet document: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadPetsFromUserField(userDoc: com.google.firebase.firestore.DocumentSnapshot) {
        try {
            // Try to get pet from user document
            val petData = userDoc.get("pet") ?: userDoc.get("pet1")
            
            if (petData is Map<*, *>) {
                // Convert the map to a pet model
                val petId = "pet1" // Use a default ID for inline pet data
                val name = (petData["name"] as? String) ?: ""
                val species = (petData["species"] as? String) ?: ""
                val breed = (petData["breed"] as? String) ?: ""
                val age = (petData["age"]?.toString()) ?: ""
                val weight = (petData["weight"]?.toString()) ?: ""
                
                val pet = PetModel(
                    petId = petId,
                    name = name,
                    species = species,
                    breed = breed,
                    age = age,
                    weight = weight,
                    ownerId = userId
                )
                
                showPets(listOf(pet))
            } else {
                showEmptyState()
            }
        } catch (e: Exception) {
            showEmptyState()
        }
    }
    
    private fun showPets(pets: List<PetModel>) {
        if (pets.isEmpty()) {
            showEmptyState()
            return
        }
        
        // Update UI
        emptyPetsText.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        
        // Update adapter with pets
        adapter.updatePets(pets)
    }
    
    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyPetsText.visibility = View.VISIBLE
    }
    
    private fun showLoadingState() {
        // You could add a progress indicator here
        recyclerView.visibility = View.GONE
        emptyPetsText.visibility = View.GONE
    }
    
    private fun setupClickListeners(view: View) {
        // Close button
        view.findViewById<ImageView>(R.id.close_button)?.setOnClickListener {
            closeDrawer()
        }
        
        // Your Pets section is now handled by the RecyclerView
        
        // Add Pet button
        view.findViewById<Button>(R.id.add_pet_button)?.setOnClickListener {
            // Show add pet dialog
            showAddPetDialog()
        }
        
        // Pet Family section
        view.findViewById<View>(R.id.manage_family)?.setOnClickListener {
            // Show manage family in sidebar
            val appPageFragment = parentFragment as? AppPageFragment
            appPageFragment?.showManageFamilyInSidebar()
        }
        
        view.findViewById<View>(R.id.generate_invite)?.setOnClickListener {
            // Show invite QR code
            (parentFragment as? AppPageFragment)?.let {
                it.showInviteQrCodeSheet()
            }
        }
        
        view.findViewById<View>(R.id.leave_family)?.setOnClickListener {
            // Handle leave family
            closeDrawer()
        }
        
        // Theme setting
        view.findViewById<View>(R.id.theme_setting)?.setOnClickListener {
            // Handle theme setting
            closeDrawer()
        }
        
        // Logout button
        view.findViewById<View>(R.id.logout_button)?.setOnClickListener {
            // Navigate back to login
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginPageFragment())
                .commit()
            closeDrawer()
        }
    }
    
    private fun showAddPetDialog() {
        // Find the HomepageLogsFragment and call its showAddPetDialog method
        val appPageFragment = parentFragment as? AppPageFragment
        appPageFragment?.let { appPage ->
            // First close the drawer
            closeDrawer()
            
            // Try to find the HomepageLogsFragment
            val homepageLogsFragment = appPage.childFragmentManager
                .findFragmentByTag("home")?.childFragmentManager
                ?.findFragmentById(R.id.logs_container) as? HomepageLogsFragment
                
            homepageLogsFragment?.showAddPetDialog() ?: run {
                Toast.makeText(context, "Could not access pet management. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun closeDrawer() {
        (parentFragment as? AppPageFragment)?.let {
            it.closeDrawer()
        }
    }
    
    companion object {
        @JvmStatic
        fun newInstance(userId: String = "user1") =
            UserProfileFragment().apply {
                arguments = Bundle().apply {
                    putString("user_id", userId)
                }
            }
    }
} 