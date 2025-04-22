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
import com.example.petcarekotlin.R
import com.example.petcarekotlin.navigation.NavigationHelper
import com.google.firebase.firestore.FirebaseFirestore

class SignupFragment : Fragment() {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var signupButton: Button
    private lateinit var loginTextView: TextView
    private lateinit var navigationHelper: NavigationHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)

        // Initialize views
        usernameEditText = view.findViewById(R.id.usernameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
        signupButton = view.findViewById(R.id.signupButton)
        loginTextView = view.findViewById(R.id.loginText)

        // Initialize navigation helper
        navigationHelper = NavigationHelper(requireActivity().supportFragmentManager)

        // Setup signup button click listener
        signupButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            when {
                username.isEmpty() -> {
                    Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
                password.isEmpty() -> {
                    Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
                password != confirmPassword -> {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    signupButton.isEnabled = false
                    signupButton.text = "Creating Account..."
                    registerUser(username, password)
                }
            }
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

        return view
    }

    private fun registerUser(username: String, password: String) {
        val db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("users")

        // Check if username already exists
        usersRef.whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Username is available, create new user
                    val user = hashMapOf(
                        "username" to username,
                        "password" to password
                    )

                    usersRef.add(user)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show()
                            
                            // Navigate back to login with the credentials
                            val args = Bundle().apply {
                                putString("username", username)
                                putString("password", password)
                            }
                            navigationHelper.navigateTo(LoginPageFragment(), args = args)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to create account", Toast.LENGTH_SHORT).show()
                            signupButton.isEnabled = true
                            signupButton.text = "Sign Up"
                        }
                } else {
                    Toast.makeText(context, "Username already exists", Toast.LENGTH_SHORT).show()
                    signupButton.isEnabled = true
                    signupButton.text = "Sign Up"
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error checking username", Toast.LENGTH_SHORT).show()
                signupButton.isEnabled = true
                signupButton.text = "Sign Up"
            }
    }
} 