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
import android.util.Log
import android.content.Context
import com.google.firebase.Timestamp

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
        
        // Get pet ID from arguments or try to fetch the default pet
        arguments?.let {
            petId = it.getString(ARG_PET_ID)
            if (petId != null) {
                loadVetInfo(petId!!)
            } else {
                // Try to fetch the default pet for this user
                fetchDefaultPet()
            }
        } ?: fetchDefaultPet() // If no arguments, try to fetch default pet
        
        // Set up save button click listener
        saveButton.setOnClickListener {
            saveVetInfo()
        }
    }
    
    private fun loadVetInfo(petId: String) {
        db.collection("pets").document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val vetInfo = document.get("vetInfo") as? Map<String, Any>
                    if (vetInfo != null) {
                        vetNameEditText.setText(vetInfo["vetName"] as? String ?: "")
                        vetContactEditText.setText(vetInfo["contact"] as? String ?: "")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("VetInfoFragment", "Error loading vet info", e)
            }
    }
    
    private fun saveVetInfo() {
        if (petId == null) {
            Toast.makeText(context, "No pet selected to save vet info", Toast.LENGTH_SHORT).show()
            return
        }
        
        val vetName = vetNameEditText.text.toString().trim()
        val vetContact = vetContactEditText.text.toString().trim()
        
        val vetInfo = hashMapOf(
            "vetName" to vetName,
            "contact" to vetContact,
            "notes" to "Regular checkup",
            "nextAppointment" to Timestamp(java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.MONTH, 6)
            }.time)
        )
        
        db.collection("pets").document(petId!!)
            .update("vetInfo", vetInfo)
            .addOnSuccessListener {
                Toast.makeText(context, "Veterinarian info saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving vet info: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun fetchDefaultPet() {
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
        petId = sharedPrefs.getString("CURRENT_PET_ID", null)
        
        if (petId != null) {
            loadVetInfo(petId!!)
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