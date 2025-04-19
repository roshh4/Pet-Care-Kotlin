package com.example.petcarekotlin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class LoginPageFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login_page, container, false)

        val loginButton = view.findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val appPageFragment = AppPageFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, appPageFragment) // replace with your container ID
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}