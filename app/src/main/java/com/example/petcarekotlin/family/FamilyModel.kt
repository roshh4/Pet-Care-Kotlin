package com.example.petcarekotlin.family

/**
 * Data model for a family in Firestore
 * 
 * Firestore Structure:
 * Collection: families
 * Document: {familyId}
 */
data class FamilyModel(
    val familyId: String = "",
    val name: String = "",
    val ownerId: String = "",  // ID of user who created the family
    val members: List<FamilyMember> = listOf(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val pets: List<String> = listOf()  // List of pet IDs associated with this family
)

/**
 * Data model for a family member
 */
data class FamilyMember(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "member",  // "owner", "admin", "member"
    val joinedAt: Long = 0,
    val permissions: Map<String, Boolean> = mapOf()  // e.g., "canEditPets" -> true
) 