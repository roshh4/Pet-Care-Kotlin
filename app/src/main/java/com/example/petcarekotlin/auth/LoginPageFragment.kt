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
import com.example.petcarekotlin.R
import com.example.petcarekotlin.core.AppPageFragment
import com.google.firebase.firestore.FirebaseFirestore

class LoginPageFragment : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signupText: TextView

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

        // Check if we have arguments with username and password (coming from signup)
        arguments?.let { args ->
            args.getString("username")?.let { username ->
                usernameEditText.setText(username)
            }
            args.getString("password")?.let { password ->
                passwordEditText.setText(password)
            }
        }

        // Firebase Firestore instance
        val db = FirebaseFirestore.getInstance()

        // Login button click listener
        loginButton.setOnClickListener {
            val usernameInput = usernameEditText.text.toString().trim()
            val passwordInput = passwordEditText.text.toString().trim()

            // Check if input fields are not empty
            if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(context, "Please fill in both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading state
            loginButton.isEnabled = false
            loginButton.text = "Logging in..."

            // Fetch the user data from Firestore
            db.collection("users")
                .whereEqualTo("username", usernameInput) // Match username
                .get()
                .addOnCompleteListener { task ->
                    // Reset button state
                    loginButton.isEnabled = true
                    loginButton.text = "Login"
                    
                    if (task.isSuccessful) {
                        val document = task.result?.documents?.firstOrNull()
                        if (document != null) {
                            // Check if password matches
                            val storedPassword = document.getString("password")
                            if (storedPassword == passwordInput) {
                                // Login successful, navigate to the next fragment
                                val appPageFragment = AppPageFragment()
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, appPageFragment)
                                    .commit()
                            } else {
                                // Incorrect password
                                Toast.makeText(context, "Incorrect username or password", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // User not found
                            Toast.makeText(context, "Incorrect username or password", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Error in Firestore query
                        Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Sign up text click listener
        signupText.setOnClickListener {
            // Navigate to the SignupFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SignupFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
} 