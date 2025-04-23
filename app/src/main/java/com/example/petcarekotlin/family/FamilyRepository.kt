package com.example.petcarekotlin.family

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Repository for interacting with the family collection in Firestore
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
     * @return The newly created family's ID
     */
    suspend fun createFamily(name: String): String = suspendCoroutine { continuation ->
        val currentUser = auth.currentUser
        if (currentUser == null) {
            continuation.resumeWithException(IllegalStateException("User not logged in"))
            return@suspendCoroutine
        }
        
        val userId = currentUser.uid
        val timestamp = System.currentTimeMillis()
        
        val familyMember = FamilyMember(
            userId = userId,
            name = currentUser.displayName ?: "",
            email = currentUser.email ?: "",
            role = "owner",
            joinedAt = timestamp,
            permissions = mapOf(
                "canEditPets" to true,
                "canInviteMembers" to true,
                "canRemoveMembers" to true
            )
        )
        
        val newFamily = hashMapOf(
            "name" to name,
            "ownerId" to userId,
            "members" to listOf(familyMember),
            "createdAt" to timestamp,
            "updatedAt" to timestamp,
            "pets" to listOf<String>()
        )
        
        familiesCollection.add(newFamily)
            .addOnSuccessListener { documentReference ->
                val familyId = documentReference.id
                
                // Update the document with its ID
                documentReference.update("familyId", familyId)
                
                // Add the family to the user's families list
                usersCollection.document(userId)
                    .update("families", com.google.firebase.firestore.FieldValue.arrayUnion(familyId))
                    .addOnSuccessListener {
                        continuation.resume(familyId)
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
     * Get a family by ID
     * 
     * @param familyId ID of the family to get
     * @return The family object
     */
    suspend fun getFamily(familyId: String): FamilyModel = suspendCoroutine { continuation ->
        familiesCollection.document(familyId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val family = document.toObject<FamilyModel>()
                    continuation.resume(family!!)
                } else {
                    continuation.resumeWithException(IllegalStateException("Family not found"))
                }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
    
    /**
     * Add a pet to a family
     * 
     * @param familyId ID of the family to add the pet to
     * @param petId ID of the pet to add
     */
    suspend fun addPetToFamily(familyId: String, petId: String) = suspendCoroutine<Unit> { continuation ->
        // Add the pet to the family's pets list
        familiesCollection.document(familyId)
            .update("pets", com.google.firebase.firestore.FieldValue.arrayUnion(petId))
            .addOnSuccessListener {
                // Update the pet's familyId field
                petsCollection.document(petId)
                    .update("familyId", familyId)
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
     * Add a member to a family
     * 
     * @param familyId ID of the family to add the member to
     * @param email Email of the user to add
     */
    suspend fun inviteMemberByEmail(familyId: String, email: String) = suspendCoroutine<Unit> { continuation ->
        // Check if the current user has permission to invite members
        val currentUser = auth.currentUser
        if (currentUser == null) {
            continuation.resumeWithException(IllegalStateException("User not logged in"))
            return@suspendCoroutine
        }
        
        // Find user by email
        usersCollection.whereEqualTo("email", email).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    continuation.resumeWithException(IllegalStateException("User with email not found"))
                    return@addOnSuccessListener
                }
                
                val userDoc = querySnapshot.documents[0]
                val userId = userDoc.id
                
                // Get the family to check permissions and update members
                familiesCollection.document(familyId).get()
                    .addOnSuccessListener { familyDoc ->
                        if (!familyDoc.exists()) {
                            continuation.resumeWithException(IllegalStateException("Family not found"))
                            return@addOnSuccessListener
                        }
                        
                        val family = familyDoc.toObject<FamilyModel>()!!
                        
                        // Check if current user has permission to invite
                        val currentMember = family.members.find { it.userId == currentUser.uid }
                        if (currentMember == null || currentMember.role != "owner" && 
                            (currentMember.permissions["canInviteMembers"] != true)) {
                            continuation.resumeWithException(IllegalStateException("No permission to invite members"))
                            return@addOnSuccessListener
                        }
                        
                        // Check if the user is already a member
                        if (family.members.any { it.userId == userId }) {
                            continuation.resumeWithException(IllegalStateException("User is already a member"))
                            return@addOnSuccessListener
                        }
                        
                        // Create new member object
                        val newMember = FamilyMember(
                            userId = userId,
                            name = userDoc.getString("displayName") ?: "",
                            email = email,
                            role = "member",
                            joinedAt = System.currentTimeMillis(),
                            permissions = mapOf(
                                "canEditPets" to true,
                                "canInviteMembers" to false,
                                "canRemoveMembers" to false
                            )
                        )
                        
                        // Add the member to the family
                        val updatedMembers = family.members.toMutableList()
                        updatedMembers.add(newMember)
                        
                        familiesCollection.document(familyId)
                            .update(
                                "members", updatedMembers,
                                "updatedAt", System.currentTimeMillis()
                            )
                            .addOnSuccessListener {
                                // Add the family to the user's families list
                                usersCollection.document(userId)
                                    .update("families", com.google.firebase.firestore.FieldValue.arrayUnion(familyId))
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
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
    
    /**
     * Get all families for the current user
     * 
     * @return List of families the user belongs to
     */
    suspend fun getUserFamilies(): List<FamilyModel> = suspendCoroutine { continuation ->
        val currentUser = auth.currentUser
        if (currentUser == null) {
            continuation.resumeWithException(IllegalStateException("User not logged in"))
            return@suspendCoroutine
        }
        
        val userId = currentUser.uid
        
        // Get user's families list
        usersCollection.document(userId).get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    continuation.resume(listOf())
                    return@addOnSuccessListener
                }
                
                val familyIds = userDoc.get("families") as? List<String> ?: listOf()
                if (familyIds.isEmpty()) {
                    continuation.resume(listOf())
                    return@addOnSuccessListener
                }
                
                // Get all families by IDs
                familiesCollection.whereIn("familyId", familyIds).get()
                    .addOnSuccessListener { querySnapshot ->
                        val families = querySnapshot.documents.mapNotNull { it.toObject<FamilyModel>() }
                        continuation.resume(families)
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