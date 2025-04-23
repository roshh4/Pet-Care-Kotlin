package com.example.petcarekotlin.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineExceptionHandler

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    
    // LiveData for signup result
    private val _signupResult = MutableLiveData<SignupResult?>()
    val signupResult: LiveData<SignupResult?> = _signupResult
    
    // LiveData for login result
    private val _loginResult = MutableLiveData<LoginResult?>()
    val loginResult: LiveData<LoginResult?> = _loginResult
    
    // Coroutine exception handler
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        // Handle global coroutine exceptions
        _signupResult.postValue(SignupResult.Error(exception.message ?: "Unknown error occurred"))
        _loginResult.postValue(LoginResult.Error(exception.message ?: "Unknown error occurred"))
    }
    
    /**
     * Register a new user
     */
    fun registerUser(username: String, email: String, fullName: String, password: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val userId = repository.registerUser(username, email, fullName, password)
                _signupResult.value = SignupResult.Success(userId)
            } catch (e: Exception) {
                _signupResult.value = SignupResult.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Login with username/email and password
     */
    fun loginUser(usernameOrEmail: String, password: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val userId = repository.loginUser(usernameOrEmail, password)
                _loginResult.value = LoginResult.Success(userId)
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Validate signup input fields
     * 
     * @return Validation result with error message if any
     */
    fun validateSignupInput(
        username: String,
        email: String,
        fullName: String, 
        password: String,
        confirmPassword: String
    ): ValidationResult {
        // Username validation
        if (username.isBlank()) {
            return ValidationResult.Error("Username cannot be empty")
        }
        if (username.length < 3) {
            return ValidationResult.Error("Username must be at least 3 characters")
        }
        
        // Email validation
        if (email.isBlank()) {
            return ValidationResult.Error("Email cannot be empty")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult.Error("Invalid email format")
        }
        
        // Full name validation
        if (fullName.isBlank()) {
            return ValidationResult.Error("Full name cannot be empty")
        }
        
        // Password validation
        if (password.isBlank()) {
            return ValidationResult.Error("Password cannot be empty")
        }
        if (password.length < 6) {
            return ValidationResult.Error("Password must be at least 6 characters")
        }
        
        // Confirm password validation
        if (password != confirmPassword) {
            return ValidationResult.Error("Passwords do not match")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Clear signup result
     */
    fun clearSignupResult() {
        _signupResult.value = null
    }
    
    /**
     * Clear login result
     */
    fun clearLoginResult() {
        _loginResult.value = null
    }
}

/**
 * Sealed class for signup result
 */
sealed class SignupResult {
    data class Success(val userId: String) : SignupResult()
    data class Error(val message: String) : SignupResult()
}

/**
 * Sealed class for login result
 */
sealed class LoginResult {
    data class Success(val userId: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

/**
 * Sealed class for validation result
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
} 