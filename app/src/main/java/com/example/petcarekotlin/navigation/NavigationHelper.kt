package com.example.petcarekotlin.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.petcarekotlin.R

/**
 * Helper class for navigating between fragments
 */
class NavigationHelper(private val fragmentManager: FragmentManager) {

    /**
     * Navigate to a fragment, replacing the current one
     * @param fragment The fragment to navigate to
     * @param addToBackStack Whether to add the transaction to the back stack
     * @param args Optional arguments to pass to the fragment
     */
    fun navigateTo(fragment: Fragment, addToBackStack: Boolean = true, args: Bundle? = null) {
        // Add arguments if provided
        if (args != null) {
            fragment.arguments = args
        }
        
        // Create transaction
        val transaction = fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
        
        // Add to back stack if needed
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        
        // Commit the transaction
        transaction.commit()
    }

    /**
     * Navigate back
     * @return true if navigation was successful, false otherwise
     */
    fun navigateBack(): Boolean {
        return if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
            true
        } else {
            false
        }
    }
} 