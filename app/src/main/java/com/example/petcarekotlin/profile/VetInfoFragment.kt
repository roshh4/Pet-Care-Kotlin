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
    
    private lateinit var vetNameEditText: EditText
    private lateinit var vetContactEditText: EditText
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
        
        // Clear any default text from the XML
        vetNameEditText.setText("")
        vetContactEditText.setText("")
        
        // Get current user ID
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "user1" // Default for testing
        
        // Get pet ID from arguments or try to fetch the default pet
        arguments?.let {
            petId = it.getString(ARG_PET_ID)
        }
        
        // Load vet data if we have a pet ID
        if (petId != null) {
            loadVetData(petId!!)
        } else {
            // Try to fetch the default pet and its vet info
            fetchDefaultPet()
        }
        
        // Set up save button click listener
        saveButton.setOnClickListener {
            saveVetData()
        }
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
                    
                    // Load the vet data for this pet
                    loadVetData(petId!!)
                } else {
                    // No pets found, just show empty fields
                    Toast.makeText(context, "No pets found. Create a pet first.", Toast.LENGTH_SHORT).show()
                    setLoadingState(false)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching pets: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
    }
    
    private fun loadVetData(petId: String) {
        // Show loading state
        setLoadingState(true)
        
        // Log for debugging
        println("Fetching vet data for pet ID: $petId")
        
        // Get the pet document which contains the vetInfo map (with capital I)
        db.collection("pets").document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        println("Pet document found: ${document.id}")
                        println("Document data: ${document.data}")
                        
                        // Get the vetInfo map from the document - note the capital "I"
                        val vetInfoMap = document.get("vetInfo") as? Map<*, *>
                        
                        println("VetInfo map: $vetInfoMap")
                        
                        if (vetInfoMap != null) {
                            // Extract vetName and vetContact from the map
                            val vetName = vetInfoMap["vetName"] as? String ?: ""
                            val vetContact = vetInfoMap["vetContact"] as? String ?: ""
                            
                            println("Extracted vetName: $vetName, vetContact: $vetContact")
                            
                            // Update UI with the extracted data
                            updateUIWithVetInfo(vetName, vetContact)

                        } else {
                            // No vetInfo map found - show empty fields
                            println("No vetInfo map found in document")
                            println("Available fields: ${document.data?.keys}")
                            updateUIWithVetInfo("", "")
                            
                            // Show message
                            Toast.makeText(context, "No vet info found for this pet", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        // Handle any parsing errors
                        println("Error parsing vet data: ${e.message}")
                        e.printStackTrace()
                        Toast.makeText(context, "Error parsing vet data: ${e.message}", Toast.LENGTH_SHORT).show()
                        updateUIWithVetInfo("", "")
                    }
                } else {
                    println("Pet document not found")
                    Toast.makeText(context, "Pet not found", Toast.LENGTH_SHORT).show()
                }
                setLoadingState(false)
            }
            .addOnFailureListener { e ->
                println("Error loading pet data: ${e.message}")
                e.printStackTrace()
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
        
        // Log for debugging
        println("Saving vet data for pet ID: $petId")
        println("VetName: $vetName, VetContact: $vetContact")
        
        // Create vetInfo map with our values - preserving existing data
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
                    
                    // Update in Firestore with capital "I" in vetInfo
                    db.collection("pets").document(petId!!)
                        .update("vetInfo", updatedVetInfo)
                        .addOnSuccessListener {
                            println("Vet info saved successfully")
                            Toast.makeText(context, "Veterinarian info saved successfully", Toast.LENGTH_SHORT).show()
                            setLoadingState(false)
                        }
                        .addOnFailureListener { e ->
                            println("Error updating vet info: ${e.message}")
                            e.printStackTrace()
                            trySetVetInfo(vetName, vetContact)
                        }
                } else {
                    // Document doesn't exist, create new
                    trySetVetInfo(vetName, vetContact)
                }
            }
            .addOnFailureListener { e ->
                println("Error getting document to update: ${e.message}")
                e.printStackTrace()
                
                // Try direct set as fallback
                trySetVetInfo(vetName, vetContact)
            }
    }
    
    private fun trySetVetInfo(vetName: String, vetContact: String) {
        // Create basic vetInfo
        val vetInfo = hashMapOf(
            "vetName" to vetName,
            "vetContact" to vetContact,
            "lastUpdated" to System.currentTimeMillis()
        )
        
        // Use capital "I" in vetInfo field name
        val petData = hashMapOf(
            "vetInfo" to vetInfo
        )
        
        db.collection("pets").document(petId!!)
            .set(petData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                println("Vet info set successfully with merge option")
                Toast.makeText(context, "Veterinarian info saved successfully", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
            .addOnFailureListener { innerE ->
                println("Error setting vet info: ${innerE.message}")
                innerE.printStackTrace()
                Toast.makeText(context, "Error saving vet info: ${innerE.message}", Toast.LENGTH_SHORT).show()
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