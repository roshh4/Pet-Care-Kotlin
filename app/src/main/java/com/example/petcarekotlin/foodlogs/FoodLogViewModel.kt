package com.example.petcarekotlin.foodlogs

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
    private val _foodLogs = MutableLiveData<List<FoodLogModel>>()
    val foodLogs: LiveData<List<FoodLogModel>> = _foodLogs
    
    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // Format for displaying dates
    private val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    
    // Load food logs for a specific pet
    fun loadFoodLogs(petId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = repository.getFoodLogsForPet(petId)
                if (result.isSuccess) {
                    _foodLogs.value = result.getOrNull() ?: emptyList()
                } else {
                    val error = result.exceptionOrNull()
                    if (error?.message?.contains("requires an index") == true) {
                        _errorMessage.value = "Database index not ready. Please wait a few minutes and try again."
                    } else {
                        _errorMessage.value = "Failed to load food logs: ${error?.message}"
                    }
                }
            } catch (e: Exception) {
                if (e.message?.contains("requires an index") == true) {
                    _errorMessage.value = "Database index not ready. Please wait a few minutes and try again."
                } else {
                    _errorMessage.value = "Error: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Add a new food log
    fun addFoodLog(petId: String, amount: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = repository.addFoodLog(petId, amount)
                if (result.isSuccess) {
                    // Reload logs after adding
                    loadFoodLogs(petId)
                } else {
                    _errorMessage.value = "Failed to add food log: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Convert FoodLogModel to UI-friendly FoodLog
    fun convertToUiModel(foodLogModel: FoodLogModel): FoodLog {
        val formattedTime = dateFormat.format(foodLogModel.createdAt)
        return FoodLog(
            time = formattedTime,
            author = foodLogModel.userName,
            amount = foodLogModel.amount
        )
    }
} 