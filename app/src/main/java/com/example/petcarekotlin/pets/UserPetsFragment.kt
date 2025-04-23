package com.example.petcarekotlin.pets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R
import com.example.petcarekotlin.models.PetModel
import com.example.petcarekotlin.profile.PetInfoFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UserPetsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: PetsListAdapter
    private val db = Firebase.firestore
    
    // The user ID whose pets we want to display
    private var userId: String = "user1" // Default for testing
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_pets, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get user ID from arguments if available
        arguments?.getString(ARG_USER_ID)?.let {
            userId = it
        }
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.petsRecyclerView)
        emptyView = view.findViewById(R.id.emptyPetsView)
        
        // Setup adapter
        adapter = PetsListAdapter(emptyList()) { pet ->
            navigateToPetDetail(pet)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        
        // Load user's pets
        loadUserPets()
    }
    
    private fun loadUserPets() {
        // First try to load from the "pets" collection
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { petsSnapshot ->
                if (!petsSnapshot.isEmpty) {
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
                    showPets(petsList)
                } else {
                    // If no pets found in the pets collection, try to get them from the user document
                    loadPetsFromUserDocument()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading pets: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }
    
    private fun loadPetsFromUserDocument() {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val petIds = userDoc.get("pets") as? List<*> ?: emptyList<String>()
                    
                    if (petIds.isNotEmpty()) {
                        // We have pet IDs in the user document, but we also need to check
                        // if there's pet data directly in the user document
                        val petId = petIds.firstOrNull() as? String
                        
                        if (petId != null) {
                            // If we have a pet ID, check if it exists in the pets collection
                            db.collection("pets").document(petId)
                                .get()
                                .addOnSuccessListener { petDoc ->
                                    if (petDoc.exists()) {
                                        // Pet exists in pets collection, load all pet IDs
                                        loadPetsByIds(petIds.filterIsInstance<String>())
                                    } else {
                                        // Pet might be stored directly in user document
                                        loadPetsFromUserField(userDoc)
                                    }
                                }
                                .addOnFailureListener {
                                    // On failure, try to get pets from user document
                                    loadPetsFromUserField(userDoc)
                                }
                        } else {
                            // No valid pet ID, try direct field
                            loadPetsFromUserField(userDoc)
                        }
                    } else {
                        // No pet IDs, check for direct field
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
    
    private fun loadPetsByIds(petIds: List<String>) {
        if (petIds.isEmpty()) {
            showEmptyState()
            return
        }
        
        val petsList = mutableListOf<PetModel>()
        var completedQueries = 0
        
        for (petId in petIds) {
            db.collection("pets").document(petId)
                .get()
                .addOnSuccessListener { petDoc ->
                    if (petDoc.exists()) {
                        try {
                            val pet = PetModel(
                                petId = petDoc.id,
                                name = petDoc.getString("name") ?: "",
                                species = petDoc.getString("species") ?: "",
                                breed = petDoc.getString("breed") ?: "",
                                age = petDoc.getString("age") ?: "",
                                weight = petDoc.getString("weight") ?: "",
                                ownerId = petDoc.getString("ownerId") ?: "",
                                photoUrl = petDoc.getString("photoUrl"),
                                createdAt = petDoc.getLong("createdAt") ?: 0,
                                updatedAt = petDoc.getLong("lastUpdated") ?: 0
                            )
                            petsList.add(pet)
                        } catch (e: Exception) {
                            // Skip invalid pet documents
                        }
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
                .addOnFailureListener {
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
    
    private fun loadPetsFromUserField(userDoc: com.google.firebase.firestore.DocumentSnapshot) {
        // This method handles the case where pets are stored directly in the user document
        try {
            // Check if there's a pet field
            val petData = userDoc.get("pet") ?: userDoc.get("pet1")
            
            if (petData is Map<*, *>) {
                // We have a pet stored directly in the user doc
                val petName = petData["name"] as? String ?: ""
                val petBreed = petData["breed"] as? String ?: ""
                val petSpecies = petData["species"] as? String ?: ""
                val petAge = (petData["age"] ?: "").toString()
                val petWeight = (petData["weight"] ?: "").toString()
                
                val pet = PetModel(
                    petId = "pet1", // Generated ID since it's inline in user doc
                    name = petName,
                    species = petSpecies,
                    breed = petBreed,
                    age = petAge,
                    weight = petWeight,
                    ownerId = userId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
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
        recyclerView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        
        // Update adapter data
        adapter.updatePets(pets)
    }
    
    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
    }
    
    private fun navigateToPetDetail(pet: PetModel) {
        // Navigate to PetInfoFragment with the selected pet ID
        val fragment = PetInfoFragment.newInstance(pet.petId)
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    companion object {
        private const val ARG_USER_ID = "user_id"
        
        @JvmStatic
        fun newInstance(userId: String = "user1") =
            UserPetsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                }
            }
    }
} 