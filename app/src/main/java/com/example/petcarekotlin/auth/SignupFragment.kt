package com.example.petcarekotlin.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.petcarekotlin.R
import com.example.petcarekotlin.navigation.NavigationHelper

class SignupFragment : Fragment() {
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var fullNameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var signupButton: Button
    private lateinit var loginTextView: TextView
    private lateinit var navigationHelper: NavigationHelper
    
    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)

        // Initialize views
        usernameEditText = view.findViewById(R.id.usernameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        fullNameEditText = view.findViewById(R.id.fullNameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
        signupButton = view.findViewById(R.id.signupButton)
        loginTextView = view.findViewById(R.id.loginText)

        // Initialize navigation helper
        navigationHelper = NavigationHelper(requireActivity().supportFragmentManager)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        
        // Setup signup button click listener
        signupButton.setOnClickListener {
            signupUser()
        }

        // Setup login text view click listener
        loginTextView.setOnClickListener {
            // Pass entered username and password back to login fragment
            val args = Bundle().apply {
                putString("username", usernameEditText.text.toString().trim())
                putString("password", passwordEditText.text.toString().trim())
            }
            navigationHelper.navigateTo(LoginPageFragment(), args = args)
        }
        
        // Observe signup result
        viewModel.signupResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is SignupResult.Success -> {
                    Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show()
                    
                    // Navigate back to login with the credentials
                    val args = Bundle().apply {
                        putString("username", usernameEditText.text.toString().trim())
                        putString("password", passwordEditText.text.toString().trim())
                    }
                    navigationHelper.navigateTo(LoginPageFragment(), args = args)
                }
                is SignupResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    enableSignupButton()
                }
                null -> { /* Initial state, do nothing */ }
            }
        }

        return view
    }
    
    private fun signupUser() {
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val fullName = fullNameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        
        // Validate input fields
        val validationResult = viewModel.validateSignupInput(
            username, email, fullName, password, confirmPassword
        )
        
        when (validationResult) {
            is ValidationResult.Success -> {
                disableSignupButton()
                viewModel.registerUser(username, email, fullName, password)
            }
            is ValidationResult.Error -> {
                Toast.makeText(context, validationResult.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun disableSignupButton() {
        signupButton.isEnabled = false
        signupButton.text = "Creating Account..."
    }
    
    private fun enableSignupButton() {
        signupButton.isEnabled = true
        signupButton.text = "Sign Up"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearSignupResult()
    }
} 