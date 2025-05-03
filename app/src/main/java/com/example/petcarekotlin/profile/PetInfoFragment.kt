package com.example.petcarekotlin.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.petcarekotlin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream

class PetInfoFragment : Fragment() {
    
    private lateinit var profileImage: ImageView
    private lateinit var editPhotoButton: ImageView
    private lateinit var petNameHeader: TextView
    private lateinit var petDetailsHeader: TextView
    
    private lateinit var petNameEditText: TextInputEditText
    private lateinit var speciesEditText: TextInputEditText
    private lateinit var breedEditText: TextInputEditText
    private lateinit var ageEditText: TextInputEditText
    private lateinit var weightEditText: EditText
    private lateinit var saveButton: Button
    
    private val db = Firebase.firestore
    private var petId: String? = null
    private var userId: String? = null
    private var currentImageBase64: String? = null
    
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val CAMERA_REQUEST_CODE = 101
    }
    
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
        
        // Get current user ID and pet ID from SharedPreferences
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
        userId = sharedPrefs.getString("CURRENT_USER_ID", null)
        petId = sharedPrefs.getString("CURRENT_PET_ID", null)
        
        if (petId != null) {
            loadPetData(petId!!)
        } else {
            showEmptyState()
            Toast.makeText(context, "No pet selected", Toast.LENGTH_SHORT).show()
        }
        
        // Set up click listeners
        setupClickListeners()
    }
    
    private fun loadPetData(petId: String) {
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
            
            // Load pet image if available
            val petImage = document.getString("petimage")
            if (!petImage.isNullOrEmpty()) {
                try {
                    val imageBytes = Base64.decode(petImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    profileImage.setImageBitmap(bitmap)
                    currentImageBase64 = petImage
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.ic_pet_placeholder)
                }
            } else {
                profileImage.setImageResource(R.drawable.ic_pet_placeholder)
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
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }
        
        // Save button click
        saveButton.setOnClickListener {
            savePetData()
        }
    }
    
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }
    
    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required to take photos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap?
            imageBitmap?.let {
                // Display the captured image
                profileImage.setImageBitmap(it)
                
                // Convert bitmap to Base64 string
                val byteArrayOutputStream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()
                currentImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                
                // Save the image to Firestore
                savePetImage()
            }
        }
    }
    
    private fun savePetData() {
        if (petId == null) {
            Toast.makeText(context, "No pet selected", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate inputs
        val name = petNameEditText.text.toString().trim()
        val species = speciesEditText.text.toString().trim()
        val breed = breedEditText.text.toString().trim()
        val age = ageEditText.text.toString().trim()
        val weight = weightEditText.text.toString().trim()
        
        // Validate required fields
        if (name.isEmpty()) {
            petNameEditText.error = "Pet name is required"
            return
        }
        
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
        
        // Update pet document
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
    }
    
    private fun savePetImage() {
        if (petId == null || currentImageBase64 == null) return
        
        setLoadingState(true)
        
        db.collection("pets").document(petId!!)
            .update("petimage", currentImageBase64)
            .addOnSuccessListener {
                Toast.makeText(context, "Pet image updated successfully", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating pet image: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
    }
} 