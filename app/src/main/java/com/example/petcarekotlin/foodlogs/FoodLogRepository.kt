package com.example.petcarekotlin.foodlogs

import com.example.petcarekotlin.models.FoodLogModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class FoodLogRepository {
    private val db = FirebaseFirestore.getInstance()
    private val foodLogsCollection = db.collection("foodLogs")
    private val petsCollection = db.collection("pets")
    private val auth = FirebaseAuth.getInstance()

    // Get current user ID or return empty string if not logged in
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    // Add a new food log for a pet with incremental ID and update pet's foodLogs array
    suspend fun addFoodLog(petId: String, amount: String): Result<FoodLogModel> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = currentUserId
                
                // Get user display name (or use "You" as fallback)
                val userName = auth.currentUser?.displayName ?: "You"
                
                // Get the next sequential ID
                val nextId = getNextFoodLogId()
                
                // Create food log model
                val foodLog = FoodLogModel(
                    id = nextId,
                    petId = petId,
                    userId = userId,
                    userName = userName,
                    amount = amount,
                    createdAt = Date()
                )
                
                // Add to Firestore with explicit ID
                foodLogsCollection.document(nextId)
                    .set(foodLog.toMap())
                    .await()
                
                // Update the pet document to include this food log ID in its array
                updatePetWithFoodLog(petId, nextId)
                
                // Return success with the log including the new ID
                Result.success(foodLog)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Update pet document to include the food log ID
    private suspend fun updatePetWithFoodLog(petId: String, foodLogId: String) {
        return withContext(Dispatchers.IO) {
            try {
                // Check if the pet has a foodLogs field already
                val petDoc = petsCollection.document(petId).get().await()
                
                if (petDoc.exists()) {
                    // Get the pet document reference
                    val petRef = petsCollection.document(petId)
                    
                    // Add the food log ID to the pet's foodLogs array
                    // If the field doesn't exist, it will be created
                    petRef.update("foodLogs", FieldValue.arrayUnion(foodLogId))
                        .await()
                } else {
                    // Log that pet doesn't exist (you might want to handle this differently)
                    println("Pet with ID $petId doesn't exist, can't update foodLogs array")
                }
            } catch (e: Exception) {
                // Log the error but don't fail the entire operation
                println("Error updating pet with food log: ${e.message}")
            }
        }
    }

    // Get the next sequential food log ID
    private suspend fun getNextFoodLogId(): String {
        return withContext(Dispatchers.IO) {
            try {
                // Get all existing food logs to find highest number
                val snapshot = foodLogsCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                // Extract all document IDs that match our pattern (foodlog1, foodlog2, etc.)
                val docIds = snapshot.documents
                    .map { it.id }
                    .filter { it.startsWith("foodlog") }
                
                val maxId = if (docIds.isEmpty()) {
                    0 // No existing foodlog IDs, start with 1
                } else {
                    // Find the highest number
                    docIds.mapNotNull { id ->
                        val numberPart = id.removePrefix("foodlog")
                        numberPart.toIntOrNull()
                    }.maxOrNull() ?: 0
                }
                
                // Return next ID in sequence
                "foodlog${maxId + 1}"
            } catch (e: Exception) {
                // If any error occurs, use a timestamp-based fallback
                "foodlog1_${System.currentTimeMillis()}"
            }
        }
    }

    // Get food logs for a specific pet
    suspend fun getFoodLogsForPet(petId: String): Result<List<FoodLogModel>> {
        return withContext(Dispatchers.IO) {
            try {
                // Try to get the food logs from the pet document first
                val petDoc = petsCollection.document(petId).get().await()
                
                // Check if the pet has a foodLogs array
                val foodLogIds = if (petDoc.exists()) {
                    val foodLogsArray = petDoc.get("foodLogs") as? List<*>
                    foodLogsArray?.filterIsInstance<String>() ?: emptyList()
                } else {
                    emptyList()
                }
                
                // If pet has food log IDs in its array, fetch those specific logs
                val foodLogs = if (foodLogIds.isNotEmpty()) {
                    // Fetch specific food logs by IDs
                    val logsList = mutableListOf<FoodLogModel>()
                    
                    // Since Firestore doesn't support whereIn with ordering,
                    // we need to fetch logs individually and sort them later
                    for (logId in foodLogIds) {
                        val docSnapshot = foodLogsCollection.document(logId).get().await()
                        if (docSnapshot.exists()) {
                            val foodLog = FoodLogModel(
                                id = docSnapshot.id,
                                petId = docSnapshot.getString("petId") ?: "",
                                userId = docSnapshot.getString("userId") ?: "",
                                userName = docSnapshot.getString("userName") ?: "",
                                amount = docSnapshot.getString("amount") ?: "",
                                timestamp = docSnapshot.getTimestamp("timestamp"),
                                createdAt = docSnapshot.getDate("createdAt") ?: Date()
                            )
                            logsList.add(foodLog)
                        }
                    }
                    
                    // Sort logs by creation date (newest first)
                    logsList.sortedByDescending { it.createdAt }
                } else {
                    // Fallback to the old query method if pet doesn't have food log IDs
                    val snapshot = foodLogsCollection
                        .whereEqualTo("petId", petId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get()
                        .await()
                    
                    snapshot.documents.mapNotNull { doc ->
                        val foodLog = FoodLogModel(
                            id = doc.id,
                            petId = doc.getString("petId") ?: "",
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            amount = doc.getString("amount") ?: "",
                            timestamp = doc.getTimestamp("timestamp"),
                            createdAt = doc.getDate("createdAt") ?: Date()
                        )
                        foodLog
                    }
                }
                
                Result.success(foodLogs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Delete a food log and remove it from the pet's array
    suspend fun deleteFoodLog(logId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Get the food log to find its petId
                val logDoc = foodLogsCollection.document(logId).get().await()
                
                if (logDoc.exists()) {
                    val petId = logDoc.getString("petId")
                    
                    // Delete the food log document
                    foodLogsCollection.document(logId).delete().await()
                    
                    // If we have a petId, remove this log from the pet's array
                    if (!petId.isNullOrEmpty()) {
                        petsCollection.document(petId)
                            .update("foodLogs", FieldValue.arrayRemove(logId))
                            .await()
                    }
                }
                
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 