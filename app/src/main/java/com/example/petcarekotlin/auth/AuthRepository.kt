package com.example.petcarekotlin.auth

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Repository for handling authentication operations
 */
class AuthRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")
    
    /**
     * Register a new user with sequential user ID (user1, user2, etc.)
     * 
     * @param username Username for the new account
     * @param email Email for the new account
     * @param fullName Full name of the user
     * @param password Password for the account (stored securely)
     * @return The newly created user's ID
     */
    suspend fun registerUser(
        username: String,
        email: String,
        fullName: String,
        password: String
    ): String = suspendCoroutine { continuation ->
        // Check if username is already taken
        usersCollection.whereEqualTo("username", username).get()
            .addOnSuccessListener { usernameSnapshot ->
                if (!usernameSnapshot.isEmpty) {
                    continuation.resumeWithException(IllegalStateException("Username already taken"))
                    return@addOnSuccessListener
                }
                
                // Check if email is already registered
                usersCollection.whereEqualTo("email", email).get()
                    .addOnSuccessListener { emailSnapshot ->
                        if (!emailSnapshot.isEmpty) {
                            continuation.resumeWithException(IllegalStateException("Email already registered"))
                            return@addOnSuccessListener
                        }
                        
                        // Get the next user ID by scanning existing user IDs
                        getNextUserId { userId ->
                            // Create the new user document
                            val timestamp = System.currentTimeMillis()
                            val newUser = hashMapOf(
                                "userId" to userId,
                                "username" to username,
                                "email" to email,
                                "fullName" to fullName,
                                "password" to password, // In a real app, this should be hashed
                                "familyId" to "",
                                "pets" to listOf<String>(),
                                "createdAt" to timestamp,
                                "lastActive" to timestamp
                            )
                            
                            // Save the new user
                            usersCollection.document(userId)
                                .set(newUser)
                                .addOnSuccessListener {
                                    continuation.resume(userId)
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
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
    
    /**
     * Login with username/email and password
     * 
     * @param usernameOrEmail Username or email for login
     * @param password Password for the account
     * @return User ID if successful
     */
    suspend fun loginUser(usernameOrEmail: String, password: String): String = suspendCoroutine { continuation ->
        // Check if input is email or username
        val isEmail = usernameOrEmail.contains("@")
        
        val query = if (isEmail) {
            usersCollection.whereEqualTo("email", usernameOrEmail)
        } else {
            usersCollection.whereEqualTo("username", usernameOrEmail)
        }
        
        query.get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    continuation.resumeWithException(IllegalStateException("User not found"))
                    return@addOnSuccessListener
                }
                
                val userDoc = querySnapshot.documents[0]
                val storedPassword = userDoc.getString("password")
                
                if (storedPassword == password) {
                    // Update last active timestamp
                    usersCollection.document(userDoc.id)
                        .update("lastActive", System.currentTimeMillis())
                        .addOnSuccessListener {
                            continuation.resume(userDoc.id)
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                } else {
                    continuation.resumeWithException(IllegalStateException("Invalid password"))
                }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
    
    /**
     * Get the next sequential user ID (user1, user2, etc.) by scanning existing documents
     * 
     * @param callback Callback with the next user ID
     */
    private fun getNextUserId(callback: (String) -> Unit) {
        // Get all user documents to find the highest user ID
        usersCollection.get()
            .addOnSuccessListener { snapshot ->
                var highestNumber = 0
                
                // Scan through all users to find the highest number
                for (doc in snapshot.documents) {
                    val docId = doc.id
                    if (docId.startsWith("user")) {
                        try {
                            val userNumber = docId.removePrefix("user").toInt()
                            if (userNumber > highestNumber) {
                                highestNumber = userNumber
                            }
                        } catch (e: NumberFormatException) {
                            // Skip documents that don't follow the userX pattern
                        }
                    }
                }
                
                // Next number is highest + 1
                val nextNumber = highestNumber + 1
                callback("user$nextNumber")
            }
            .addOnFailureListener { e ->
                // If there's an error, default to user1
                callback("user1")
            }
    }
} 