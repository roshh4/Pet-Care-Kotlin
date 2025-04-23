package com.example.petcarekotlin.models

/**
 * Data model for a user in Firestore
 * 
 * Firestore Structure:
 * Collection: users
 * Document: {userId}
 */
data class UserModel(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val pets: List<String> = listOf(),  // List of pet IDs owned by user
    val families: List<String> = listOf(),  // List of family IDs user belongs to
    val defaultPetId: String? = null,  // ID of the pet to show by default
    val defaultFamilyId: String? = null,  // ID of the family to show by default
    val createdAt: Long = 0,
    val lastActive: Long = 0,
    val settings: Map<String, Any> = mapOf()  // User preferences and settings
) 