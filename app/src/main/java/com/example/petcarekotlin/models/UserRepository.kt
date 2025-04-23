package com.example.petcarekotlin.models

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Repository for interacting with the users collection in Firestore
 */
class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")
    private val petsCollection = db.collection("pets")
    
    /**
     * Get user data for the current user
     * 
     * @return The user model
     */
    suspend fun getCurrentUser(): UserModel = suspendCoroutine { continuation ->
        val currentUser = auth.currentUser
        if (currentUser == null) {
            continuation.resumeWithException(IllegalStateException("User not logged in"))
            return@suspendCoroutine
        }
        
        usersCollection.document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject<UserModel>()
                    continuation.resume(user!!)
                } else {
                    // Create a new user document if it doesn't exist
                    val newUser = UserModel(
                        userId = currentUser.uid,
                        email = currentUser.email ?: "",
                        displayName = currentUser.displayName ?: "",
                        photoUrl = currentUser.photoUrl?.toString() ?: "",
                        createdAt = System.currentTimeMillis(),
                        lastActive = System.currentTimeMillis()
                    )
                    
                    usersCollection.document(currentUser.uid)
                        .set(newUser)
                        .addOnSuccessListener {
                            continuation.resume(newUser)
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
    
    /**
     * Add a pet to the current user's pets array
     * 
     * @param petId ID of the pet to add
     */
    suspend fun addPetToUser(petId: String) = suspendCoroutine<Unit> { continuation ->
        val currentUser = auth.currentUser
        if (currentUser == null) {
            continuation.resumeWithException(IllegalStateException("User not logged in"))
            return@suspendCoroutine
        }
        
        // Add the pet to the user's pets list
        usersCollection.document(currentUser.uid)
            .update("pets", com.google.firebase.firestore.FieldValue.arrayUnion(petId))
            .addOnSuccessListener {
                // Update the pet's owner if not already set
                petsCollection.document(petId).get()
                    .addOnSuccessListener { petDoc ->
                        if (petDoc.exists() && petDoc.getString("ownerId").isNullOrEmpty()) {
                            petsCollection.document(petId)
                                .update("ownerId", currentUser.uid)
                                .addOnSuccessListener {
                                    continuation.resume(Unit)
                                }
                                .addOnFailureListener { e ->
                                    continuation.resumeWithException(e)
                                }
                        } else {
                            continuation.resume(Unit)
                        }
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
            .addOnFailureListener { e ->
                // If updating fails, the user document might not exist, so try creating it
                val userData = hashMapOf(
                    "userId" to currentUser.uid,
                    "email" to (currentUser.email ?: ""),
                    "displayName" to (currentUser.displayName ?: ""),
                    "pets" to listOf(petId),
                    "createdAt" to System.currentTimeMillis(),
                    "lastActive" to System.currentTimeMillis()
                )
                
                usersCollection.document(currentUser.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener { innerE ->
                        continuation.resumeWithException(innerE)
                    }
            }
    }
    
    /**
     * Remove a pet from the current user's pets array
     * 
     * @param petId ID of the pet to remove
     */
    suspend fun removePetFromUser(petId: String) = suspendCoroutine<Unit> { continuation ->
        val currentUser = auth.currentUser
        if (currentUser == null) {
            continuation.resumeWithException(IllegalStateException("User not logged in"))
            return@suspendCoroutine
        }
        
        // Remove the pet from the user's pets list
        usersCollection.document(currentUser.uid)
            .update("pets", com.google.firebase.firestore.FieldValue.arrayRemove(petId))
            .addOnSuccessListener {
                continuation.resume(Unit)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
    
    /**
     * Get all pets for the current user
     * 
     * @return List of pets owned by the user
     */
    suspend fun getUserPets(): List<PetModel> = suspendCoroutine { continuation ->
        val currentUser = auth.currentUser
        if (currentUser == null) {
            continuation.resumeWithException(IllegalStateException("User not logged in"))
            return@suspendCoroutine
        }
        
        // Get user's pets list
        usersCollection.document(currentUser.uid).get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    continuation.resume(listOf())
                    return@addOnSuccessListener
                }
                
                val petIds = userDoc.get("pets") as? List<String> ?: listOf()
                if (petIds.isEmpty()) {
                    continuation.resume(listOf())
                    return@addOnSuccessListener
                }
                
                // Get all pets by IDs
                petsCollection.whereIn("petId", petIds).get()
                    .addOnSuccessListener { querySnapshot ->
                        val pets = querySnapshot.documents.mapNotNull { it.toObject<PetModel>() }
                        continuation.resume(pets)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
    
    /**
     * Set default pet for the user
     * 
     * @param petId ID of the pet to set as default
     */
    suspend fun setDefaultPet(petId: String) = suspendCoroutine<Unit> { continuation ->
        val currentUser = auth.currentUser
        if (currentUser == null) {
            continuation.resumeWithException(IllegalStateException("User not logged in"))
            return@suspendCoroutine
        }
        
        // Update default pet
        usersCollection.document(currentUser.uid)
            .update("defaultPetId", petId)
            .addOnSuccessListener {
                continuation.resume(Unit)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
    
    /**
     * Update user profile data
     * 
     * @param displayName Display name for the user
     * @param photoUrl Profile photo URL
     */
    suspend fun updateUserProfile(displayName: String, photoUrl: String? = null) = suspendCoroutine<Unit> { continuation ->
        val currentUser = auth.currentUser
        if (currentUser == null) {
            continuation.resumeWithException(IllegalStateException("User not logged in"))
            return@suspendCoroutine
        }
        
        val updates = hashMapOf<String, Any>(
            "displayName" to displayName,
            "lastActive" to System.currentTimeMillis()
        )
        
        if (photoUrl != null) {
            updates["photoUrl"] = photoUrl
        }
        
        usersCollection.document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                continuation.resume(Unit)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
} 