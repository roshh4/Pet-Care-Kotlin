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
        
        // Create and show the PetInfoFragment with the selected pet ID
        val fragment = PetInfoFragment.newInstance(pet.petId)
        
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
                    val petsList = userDoc.get("pets") as? List<*>
                    
                    if (!petsList.isNullOrEmpty()) {
                        // Check if we need to load pet details
                        loadPetsFromUserField(userDoc)
                    } else {
                        showEmptyState()
                    }
                } else {
                    showEmptyState()
                }
            }
            .addOnFailureListener {
                showEmptyState()
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