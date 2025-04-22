package com.example.petcarekotlin.UserProfile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.petcarekotlin.core.AppPageFragment
import com.example.petcarekotlin.auth.LoginPageFragment
import com.example.petcarekotlin.R

class UserProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for user profile sidebar
        val view = inflater.inflate(R.layout.fragment_user_profile, container, false)
        
        setupClickListeners(view)
        
        return view
    }
    
    private fun setupClickListeners(view: View) {
        // Close button
        view.findViewById<ImageView>(R.id.close_button)?.setOnClickListener {
            closeDrawer()
        }
        
        // Your Pets section
        view.findViewById<View>(R.id.pet_buddy)?.setOnClickListener {
            // Handle pet click
            closeDrawer()
        }
        
        view.findViewById<View>(R.id.pet_whiskers)?.setOnClickListener {
            // Handle pet click
            closeDrawer()
        }
        
        view.findViewById<View>(R.id.pet_rex)?.setOnClickListener {
            // Handle pet click
            closeDrawer()
        }
        
        // Add Pet button
        view.findViewById<Button>(R.id.add_pet_button)?.setOnClickListener {
            // Show add pet dialog or navigate to add pet screen
            Toast.makeText(context, "Add Pet functionality coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Pet Family section
        view.findViewById<View>(R.id.manage_family)?.setOnClickListener {
            // Show manage family in sidebar
            val appPageFragment = parentFragment as? AppPageFragment
            appPageFragment?.showManageFamilyInSidebar()
        }
        
        view.findViewById<View>(R.id.generate_invite)?.setOnClickListener {
            // Show invite QR code
            (parentFragment as? AppPageFragment)?.let {
                it.showInviteQrCodeSheet()
            }
        }
        
        view.findViewById<View>(R.id.leave_family)?.setOnClickListener {
            // Handle leave family
            closeDrawer()
        }
        
        // Theme setting
        view.findViewById<View>(R.id.theme_setting)?.setOnClickListener {
            // Handle theme setting
            closeDrawer()
        }
        
        // Logout button
        view.findViewById<View>(R.id.logout_button)?.setOnClickListener {
            // Navigate back to login
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginPageFragment())
                .commit()
            closeDrawer()
        }
    }
    
    private fun closeDrawer() {
        (parentFragment as? AppPageFragment)?.let {
            it.closeDrawer()
        }
    }
} 