package com.example.petcarekotlin.models

/**
 * Data model for a pet in Firestore
 * 
 * Firestore Structure:
 * Collection: pets
 * Document: {petId}
 */
data class PetModel(
    val petId: String = "",
    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val age: String = "",
    val weight: String = "",
    val ownerId: String = "",  // ID of the user who owns the pet
    val familyId: String? = null,  // ID of the family the pet belongs to (optional)
    val photoUrl: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val vetInfo: VetInfo = VetInfo(),
    val reminders: List<Reminder> = listOf()
)

/**
 * Data model for veterinarian information
 */
data class VetInfo(
    val vetName: String = "",
    val vetContact: String = "",
    val nextAppointment: String = "",
    val notes: String = ""
)

/**
 * Data model for a pet reminder
 */
data class Reminder(
    val reminderId: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Long = 0,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrencePattern: String = "", // "daily", "weekly", "monthly", etc.
    val createdBy: String = "",
    val assignedTo: String? = null
) 