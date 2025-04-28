package com.example.petcarekotlin.foodlogs

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcarekotlin.models.FoodLogModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class FoodLogViewModel : ViewModel() {
    private val repository = FoodLogRepository()
    
    // LiveData for food logs
    private val _foodLogs = MutableLiveData<List<FoodLogModel>>(emptyList())
    val foodLogs: LiveData<List<FoodLogModel>> = _foodLogs
    
    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage
    
    // Current user information
    private var userId: String = ""
    private var username: String = ""
    private var userFullName: String = ""
    
    // Initialize user information from SharedPreferences
    fun initializeUserInfo(context: Context) {
        val sharedPrefs = context.getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
        userId = sharedPrefs.getString("CURRENT_USER_ID", "") ?: ""
        username = sharedPrefs.getString("CURRENT_USERNAME", "") ?: ""
        
        // If we have a userId, fetch the user's full name from Firestore
        if (userId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val result = repository.getUserFullName(userId)
                    userFullName = result ?: ""
                } catch (e: Exception) {
                    // If there's an error fetching full name, use username as fallback
                    userFullName = username
                }
            }
        }
    }
    
    // Load food logs for a specific pet
    fun loadFoodLogs(petId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                val result = repository.getFoodLogsForPet(petId)
                
                if (result.isSuccess) {
                    _foodLogs.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load food logs: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading food logs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Add a new food log
    fun addFoodLog(petId: String, amount: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                // Use the user information from SharedPreferences if available
                val result = if (userId.isNotEmpty()) {
                    repository.addFoodLogWithUser(petId, amount, userId, username, userFullName)
                } else {
                    repository.addFoodLog(petId, amount)
                }
                
                if (result.isSuccess) {
                    // Reload the logs to show the new entry
                    loadFoodLogs(petId)
                } else {
                    _errorMessage.value = "Failed to add food log: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error adding food log: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // Convert FoodLogModel to UI FoodLog
    fun convertToUiModel(model: FoodLogModel): FoodLog {
        // Format date to display time
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        
        // Use the timestamp if available, otherwise use createdAt
        val date = model.timestamp?.toDate() ?: model.createdAt
        val timeStr = timeFormat.format(date)
        val dateStr = dateFormat.format(date)
        
        // Determine the author name to display
        val authorName = when {
            model.userFullName.isNotEmpty() -> model.userFullName
            model.userName.isNotEmpty() -> model.userName
            else -> "Unknown"
        }
        
        return FoodLog(
            time = "$dateStr at $timeStr",
            author = authorName,
            amount = model.amount
        )
    }
}
