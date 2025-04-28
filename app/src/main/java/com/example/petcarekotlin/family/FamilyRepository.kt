package com.example.petcarekotlin.family

import com.example.petcarekotlin.models.FamilyModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Repository for interacting with the families collection in Firestore
 */
class FamilyRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val familiesCollection = db.collection("families")
    private val usersCollection = db.collection("users")
    private val petsCollection = db.collection("pets")
    
    /**
     * Create a new family
     * 
     * @param name Name of the family
     * @param creatorId ID of the user creating the family
     * @return The newly created family model
     */
    suspend fun createFamily(name: String, creatorId: String): FamilyModel = suspendCoroutine { continuation ->
        // Get the next sequential family ID
        getNextFamilyId { familyId ->
        val inviteCode = generateInviteCode()
        val timestamp = System.currentTimeMillis()
        
        val family = FamilyModel(
            familyId = familyId,
            name = name,
            createdBy = creatorId,
            members = listOf(creatorId),
            admins = listOf(creatorId),
            inviteCode = inviteCode,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        
        // Create the family document
        familiesCollection.document(familyId)
            .set(family)
            .addOnSuccessListener {
                // Update the user's record with the new family
                usersCollection.document(creatorId)
                    .update("families", FieldValue.arrayUnion(familyId))
                    .addOnSuccessListener {
                        // Set as default family if user has no default
                        usersCollection.document(creatorId).get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.getString("defaultFamilyId").isNullOrEmpty()) {
                                    usersCollection.document(creatorId)
                                        .update("defaultFamilyId", familyId)
                                        .addOnSuccessListener {
                                            continuation.resume(family)
                                        }
                                        .addOnFailureListener { e ->
                                            // If setting default fails, still return success
                                            continuation.resume(family)
                                        }
                                } else {
                                    continuation.resume(family)
                                }
                            }
                            .addOnFailureListener { e ->
                                // If getting user fails, still return success
                                continuation.resume(family)
                            }
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
            }
    }
    
    /**
     * Remove a member from the family
     * 
     * @param familyId ID of the family
     * @param memberId ID of the member to remove
     */
    suspend fun removeMember(familyId: String, memberId: String) = suspendCoroutine<Unit> { continuation ->
        familiesCollection.document(familyId)
            .get()
            .addOnSuccessListener { familyDoc ->
                if (!familyDoc.exists()) {
                    continuation.resumeWithException(IllegalStateException("Family not found"))
                    return@addOnSuccessListener
                }
                
                val family = familyDoc.toObject<FamilyModel>()
                
                if (family == null) {
                    continuation.resumeWithException(IllegalStateException("Error retrieving family data"))
                    return@addOnSuccessListener
                }
                
                // Verify user is a member
                if (!family.members.contains(memberId)) {
                    continuation.resumeWithException(IllegalStateException("User is not a member of this family"))
                    return@addOnSuccessListener
                }
                
                // Remove user from family
                val updateMap = mutableMapOf<String, Any>()
                updateMap["members"] = FieldValue.arrayRemove(memberId)
                updateMap["updatedAt"] = System.currentTimeMillis()
                
                // Also remove from admins if applicable
                if (family.admins.contains(memberId)) {
                    updateMap["admins"] = FieldValue.arrayRemove(memberId)
                }
                
                familiesCollection.document(familyId)
                    .update(updateMap)
                    .addOnSuccessListener {
                        // Remove family from user's families
                        usersCollection.document(memberId)
                            .update("families", FieldValue.arrayRemove(familyId))
                            .addOnSuccessListener {
                                // Also clear default family if it's this one
                                usersCollection.document(memberId)
                                    .get()
                                    .addOnSuccessListener { userDoc ->
                                        if (userDoc.getString("defaultFamilyId") == familyId) {
                                            usersCollection.document(memberId)
                                                .update("defaultFamilyId", "")
                                                .addOnSuccessListener {
                                                    continuation.resume(Unit)
                                                }
                                                .addOnFailureListener { e ->
                                                    continuation.resume(Unit)
                                                }
                                        } else {
                                            continuation.resume(Unit)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        continuation.resume(Unit)
                                    }
                            }
                            .addOnFailureListener { e ->
                                continuation.resumeWithException(e)
                            }
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
     * Make a member an admin in the family
     * 
     * @param familyId ID of the family
     * @param memberId ID of the member to promote
     * @param adminId ID of the admin performing the promotion
     */
    suspend fun makeAdmin(familyId: String, memberId: String, adminId: String) = suspendCoroutine<Unit> { continuation ->
        familiesCollection.document(familyId)
            .get()
            .addOnSuccessListener { familyDoc ->
                if (!familyDoc.exists()) {
                    continuation.resumeWithException(IllegalStateException("Family not found"))
                    return@addOnSuccessListener
                }
                
                val family = familyDoc.toObject<FamilyModel>()
                
                if (family == null) {
                    continuation.resumeWithException(IllegalStateException("Error retrieving family data"))
                    return@addOnSuccessListener
                }
                
                // Verify admin permissions
                if (!family.admins.contains(adminId)) {
                    continuation.resumeWithException(IllegalStateException("You don't have permission to make admins"))
                    return@addOnSuccessListener
                }
                
                // Check if member is in the family
                if (!family.members.contains(memberId)) {
                    continuation.resumeWithException(IllegalStateException("User is not a member of this family"))
                    return@addOnSuccessListener
                }
                
                // Check if member is already an admin
                if (family.admins.contains(memberId)) {
                    continuation.resume(Unit) // Already an admin, no action needed
                    return@addOnSuccessListener
                }
                
                familiesCollection.document(familyId)
                    .update(
                        "admins", FieldValue.arrayUnion(memberId),
                        "updatedAt", System.currentTimeMillis()
                    )
                    .addOnSuccessListener {
                        continuation.resume(Unit)
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
     * Transfer family ownership to another member
     * 
     * @param familyId ID of the family
     * @param newOwnerId ID of the member to make owner
     * @param currentOwnerId ID of the current owner
     */
    suspend fun transferOwnership(familyId: String, newOwnerId: String, currentOwnerId: String) = suspendCoroutine<Unit> { continuation ->
        familiesCollection.document(familyId)
            .get()
            .addOnSuccessListener { familyDoc ->
                if (!familyDoc.exists()) {
                    continuation.resumeWithException(IllegalStateException("Family not found"))
                    return@addOnSuccessListener
                }
                
                val family = familyDoc.toObject<FamilyModel>()
                
                if (family == null) {
                    continuation.resumeWithException(IllegalStateException("Error retrieving family data"))
                    return@addOnSuccessListener
                }
                
                // Verify ownership
                if (family.createdBy != currentOwnerId) {
                    continuation.resumeWithException(IllegalStateException("Only the owner can transfer ownership"))
                    return@addOnSuccessListener
                }
                
                // Check if new owner is in the family
                if (!family.members.contains(newOwnerId)) {
                    continuation.resumeWithException(IllegalStateException("New owner must be a member of the family"))
                    return@addOnSuccessListener
                }
                
                // Make sure new owner is an admin first
                val updateMap = mutableMapOf<String, Any>()
                updateMap["createdBy"] = newOwnerId
                updateMap["updatedAt"] = System.currentTimeMillis()
                
                // Make sure new owner is in admins list
                if (!family.admins.contains(newOwnerId)) {
                    updateMap["admins"] = FieldValue.arrayUnion(newOwnerId)
                }
                
                familiesCollection.document(familyId)
                    .update(updateMap)
                    .addOnSuccessListener {
                        continuation.resume(Unit)
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
     * Add a pet to the family
     * 
     * @param familyId ID of the family
     * @param petId ID of the pet to add
     * @param userId ID of the user adding the pet
     */
    suspend fun addPetToFamily(familyId: String, petId: String, userId: String) = suspendCoroutine<Unit> { continuation ->
        familiesCollection.document(familyId)
            .get()
            .addOnSuccessListener { familyDoc ->
                if (!familyDoc.exists()) {
                    continuation.resumeWithException(IllegalStateException("Family not found"))
                    return@addOnSuccessListener
                }
                
                val family = familyDoc.toObject<FamilyModel>()
                
                if (family == null) {
                    continuation.resumeWithException(IllegalStateException("Error retrieving family data"))
                    return@addOnSuccessListener
                }
                
                // Verify user is a member
                if (!family.members.contains(userId)) {
                    continuation.resumeWithException(IllegalStateException("You must be a member of the family to add pets"))
                    return@addOnSuccessListener
                }
                
                // Add pet to family
                familiesCollection.document(familyId)
                    .update(
                        "pets", FieldValue.arrayUnion(petId),
                        "updatedAt", System.currentTimeMillis()
                    )
                    .addOnSuccessListener {
                        // Also update the pet's familyId field
                        db.collection("pets").document(petId)
                            .update("familyId", familyId)
                            .addOnSuccessListener {
                                continuation.resume(Unit)
                            }
                            .addOnFailureListener { e ->
                                // If updating pet fails, still consider the operation successful
                                continuation.resume(Unit)
                            }
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
     * Remove a pet from the family
     * 
     * @param familyId ID of the family
     * @param petId ID of the pet to remove
     * @param userId ID of the user removing the pet
     */
    suspend fun removePetFromFamily(familyId: String, petId: String, userId: String) = suspendCoroutine<Unit> { continuation ->
        familiesCollection.document(familyId)
            .get()
            .addOnSuccessListener { familyDoc ->
                if (!familyDoc.exists()) {
                    continuation.resumeWithException(IllegalStateException("Family not found"))
                    return@addOnSuccessListener
                }
                
                val family = familyDoc.toObject<FamilyModel>()
                
                if (family == null) {
                    continuation.resumeWithException(IllegalStateException("Error retrieving family data"))
                    return@addOnSuccessListener
                }
                
                // Verify user is a member
                if (!family.members.contains(userId)) {
                    continuation.resumeWithException(IllegalStateException("You must be a member of the family to manage pets"))
                    return@addOnSuccessListener
                }
                
                // Remove pet from family
                familiesCollection.document(familyId)
                    .update(
                        "pets", FieldValue.arrayRemove(petId),
                        "updatedAt", System.currentTimeMillis()
                    )
                    .addOnSuccessListener {
                        // Also clear the pet's familyId field
                        db.collection("pets").document(petId)
                            .update("familyId", null)
                            .addOnSuccessListener {
                                continuation.resume(Unit)
                            }
                            .addOnFailureListener { e ->
                                // If updating pet fails, still consider the operation successful
                                continuation.resume(Unit)
                            }
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
     * Get the next sequential ID for families (family1, family2, etc.)
     * 
     * @param callback Callback with the next available ID
     */
    private fun getNextFamilyId(callback: (String) -> Unit) {
        familiesCollection.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    callback("family1") // Start with family1 if no families exist
                } else {
                    // Find the maximum ID and add 1
                    var maxId = 0
                    for (doc in snapshot.documents) {
                        val docId = doc.id
                        if (docId.startsWith("family")) {
                            // Extract the number after "family"
                            val numberStr = docId.substring(6) // "family" is 6 characters
                            val number = numberStr.toIntOrNull() ?: 0
                            if (number > maxId) {
                                maxId = number
                            }
                        }
                    }
                    callback("family${maxId + 1}")
                }
            }
            .addOnFailureListener { e ->
                // Fallback to a timestamp-based ID in case of error
                callback("family${(System.currentTimeMillis() % 10000).toInt()}")
            }
    }
    
    /**
     * Generate a random invite code
     * 
     * @return A random 6-character invite code
     */
    private fun generateInviteCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }
    
    /**
     * Get a user document by ID
     *
     * @param userId ID of the user to retrieve
     * @return The user document snapshot
     */
    suspend fun getUserById(userId: String) = usersCollection.document(userId).get().await()

    /**
     * Get all families a user belongs to
     * 
     * @param userId ID of the user
     * @return List of families the user belongs to
     */
    suspend fun getUserFamilies(userId: String): List<FamilyModel> = suspendCoroutine { continuation ->
        usersCollection.document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val familyIds = userDoc.get("families") as? List<*>
                    
                    if (familyIds.isNullOrEmpty()) {
                        continuation.resume(emptyList())
                        return@addOnSuccessListener
                    }
                    
                    val stringFamilyIds = familyIds.mapNotNull { it as? String }
                    if (stringFamilyIds.isEmpty()) {
                        continuation.resume(emptyList())
                        return@addOnSuccessListener
                    }
                    
                    // Query for all families user belongs to
                    familiesCollection.whereArrayContains("members", userId)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val families = querySnapshot.documents.mapNotNull { doc ->
                                doc.toObject<FamilyModel>()
                            }
                            continuation.resume(families)
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                } else {
                    continuation.resume(emptyList())
                }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    /**
     * Remove a member from a family
     * 
     * @param familyId ID of the family
     * @param memberId ID of the member to remove
     * @param userId ID of the user performing the removal (must be admin)
     */
    suspend fun removeMember(familyId: String, memberId: String, userId: String) = suspendCoroutine<Unit> { continuation ->
        familiesCollection.document(familyId)
            .get()
            .addOnSuccessListener { familyDoc ->
                if (!familyDoc.exists()) {
                    continuation.resumeWithException(IllegalStateException("Family not found"))
                    return@addOnSuccessListener
                }
                
                val family = familyDoc.toObject<FamilyModel>()
                
                if (family == null) {
                    continuation.resumeWithException(IllegalStateException("Error retrieving family data"))
                    return@addOnSuccessListener
                }
                
                // Verify admin permissions
                if (!family.admins.contains(userId)) {
                    continuation.resumeWithException(IllegalStateException("You don't have permission to remove members"))
                    return@addOnSuccessListener
                }
                
                // Cannot remove the owner
                if (family.createdBy == memberId) {
                    continuation.resumeWithException(IllegalStateException("Cannot remove the family owner"))
                    return@addOnSuccessListener
                }
                
                // Check if member is in the family
                if (!family.members.contains(memberId)) {
                    continuation.resumeWithException(IllegalStateException("User is not a member of this family"))
                    return@addOnSuccessListener
                }
                
                // Remove member from family
                val updateMap = mutableMapOf<String, Any>()
                updateMap["members"] = FieldValue.arrayRemove(memberId)
                updateMap["updatedAt"] = System.currentTimeMillis()
                
                // Also remove from admins if applicable
                if (family.admins.contains(memberId)) {
                    updateMap["admins"] = FieldValue.arrayRemove(memberId)
                }
                
                familiesCollection.document(familyId)
                    .update(updateMap)
                    .addOnSuccessListener {
                        // Remove family from user's families
                        usersCollection.document(memberId)
                            .update("families", FieldValue.arrayRemove(familyId))
                            .addOnSuccessListener {
                                // Also clear default family if it's this one
                                usersCollection.document(memberId)
                                    .get()
                                    .addOnSuccessListener { userDoc ->
                                        if (userDoc.getString("defaultFamilyId") == familyId) {
                                            usersCollection.document(memberId)
                                                .update("defaultFamilyId", "")
                                                .addOnSuccessListener {
                                                    continuation.resume(Unit)
                                                }
                                                .addOnFailureListener { e ->
                                                    continuation.resume(Unit)
                                                }
                                        } else {
                                            continuation.resume(Unit)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        continuation.resume(Unit)
                                    }
                            }
                            .addOnFailureListener { e ->
                                continuation.resumeWithException(e)
                            }
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
     * Leave a family
     * 
     * @param familyId ID of the family to leave
     * @param userId ID of the user leaving
     */
    suspend fun leaveFamily(familyId: String, userId: String) = suspendCoroutine<Unit> { continuation ->
        familiesCollection.document(familyId)
            .get()
            .addOnSuccessListener { familyDoc ->
                if (!familyDoc.exists()) {
                    continuation.resumeWithException(IllegalStateException("Family not found"))
                    return@addOnSuccessListener
                }
                
                val family = familyDoc.toObject<FamilyModel>()
                
                if (family == null) {
                    continuation.resumeWithException(IllegalStateException("Error retrieving family data"))
                    return@addOnSuccessListener
                }
                
                // Owner cannot leave without transferring ownership first
                if (family.createdBy == userId) {
                    continuation.resumeWithException(IllegalStateException("As the owner, you must transfer ownership before leaving"))
                    return@addOnSuccessListener
                }
                
                // Check if user is in the family
                if (!family.members.contains(userId)) {
                    continuation.resumeWithException(IllegalStateException("You are not a member of this family"))
                    return@addOnSuccessListener
                }
                
                // Remove user from family
                val updateMap = mutableMapOf<String, Any>()
                updateMap["members"] = FieldValue.arrayRemove(userId)
                updateMap["updatedAt"] = System.currentTimeMillis()
                
                // Also remove from admins if applicable
                if (family.admins.contains(userId)) {
                    updateMap["admins"] = FieldValue.arrayRemove(userId)
                }
                
                familiesCollection.document(familyId)
                    .update(updateMap)
                    .addOnSuccessListener {
                        // Remove family from user's families
                        usersCollection.document(userId)
                            .update("families", FieldValue.arrayRemove(familyId))
                            .addOnSuccessListener {
                                // Also clear default family if it's this one
                                usersCollection.document(userId)
                                    .get()
                                    .addOnSuccessListener { userDoc ->
                                        if (userDoc.getString("defaultFamilyId") == familyId) {
                                            usersCollection.document(userId)
                                                .update("defaultFamilyId", "")
                                                .addOnSuccessListener {
                                                    continuation.resume(Unit)
                                                }
                                                .addOnFailureListener { e ->
                                                    continuation.resume(Unit)
                                                }
                                        } else {
                                            continuation.resume(Unit)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        continuation.resume(Unit)
                                    }
                            }
                            .addOnFailureListener { e ->
                                continuation.resumeWithException(e)
                            }
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
} 