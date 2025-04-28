package com.example.petcarekotlin.models

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
    val createdBy: String = "",  // UserID of the family creator
    val members: List<String> = listOf(),  // List of userIds who are family members
    val admins: List<String> = listOf(),  // List of userIds who have admin privileges
    val pets: List<String> = listOf(),  // List of petIds shared within the family
    val inviteCode: String = "",  // Unique invite code for joining the family
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) 