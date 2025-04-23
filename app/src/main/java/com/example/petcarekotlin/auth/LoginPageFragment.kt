package com.example.petcarekotlin.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.petcarekotlin.R
import com.example.petcarekotlin.core.AppPageFragment
import com.example.petcarekotlin.navigation.NavigationHelper

class LoginPageFragment : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signupText: TextView
    private lateinit var viewModel: AuthViewModel
    private lateinit var navigationHelper: NavigationHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login_page, container, false)

        // Initialize views
        usernameEditText = view.findViewById(R.id.username)
        passwordEditText = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.loginButton)
        signupText = view.findViewById(R.id.signupText)

        // Initialize navigation helper
        navigationHelper = NavigationHelper(requireActivity().supportFragmentManager)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Check if we have arguments with username and password (coming from signup)
        arguments?.let { args ->
            args.getString("username")?.let { username ->
                usernameEditText.setText(username)
            }
            args.getString("password")?.let { password ->
                passwordEditText.setText(password)
            }
        }

        // Login button click listener
        loginButton.setOnClickListener {
            loginUser()
        }

        // Sign up text click listener
        signupText.setOnClickListener {
            // Navigate to the SignupFragment
            navigationHelper.navigateTo(SignupFragment())
        }
        
        // Observe login result
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is LoginResult.Success -> {
                    Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                    // Navigate to the main app page
                    navigationHelper.navigateTo(AppPageFragment())
                }
                is LoginResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    enableLoginButton()
                }
                null -> { /* Initial state, do nothing */ }
            }
        }

        return view
    }
    
    private fun loginUser() {
        val usernameInput = usernameEditText.text.toString().trim()
        val passwordInput = passwordEditText.text.toString().trim()

        // Check if input fields are not empty
        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(context, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        disableLoginButton()
        
        // Login using ViewModel
        viewModel.loginUser(usernameInput, passwordInput)
    }
    
    private fun disableLoginButton() {
        loginButton.isEnabled = false
        loginButton.text = "Logging in..."
    }
    
    private fun enableLoginButton() {
        loginButton.isEnabled = true
        loginButton.text = "Login"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearLoginResult()
    }
} 