package com.example.petcarekotlin.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FoodLogModel(
    @DocumentId val id: String = "",
    val petId: String = "",
    val userId: String = "",
    val userName: String = "",
    val amount: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val createdAt: Date = Date()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "petId" to petId,
            "userId" to userId,
            "userName" to userName,
            "amount" to amount,
            "createdAt" to createdAt
        )
    }
} 