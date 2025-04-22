package com.example.petcarekotlin.core

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.petcarekotlin.R
import android.util.Log

class FooterFragment : Fragment() {

    interface OnFooterNavigationListener {
        fun onFooterItemSelected(itemId: Int)
    }

    private var navListener: OnFooterNavigationListener? = null
    private lateinit var bottomNav: BottomNavigationView
    
    // Default selected item ID
    private var selectedItemId: Int = R.id.nav_home
    
    // Flag to prevent recursive calls
    private var isUpdatingSelectedItem = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navListener = parentFragment as? OnFooterNavigationListener
        if (navListener == null) {
            Log.w("FooterFragment", "Parent fragment doesn't implement OnFooterNavigationListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        navListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_footer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNav = view.findViewById(R.id.bottom_nav_menu)

        // Set the initial selected item without triggering listener
        isUpdatingSelectedItem = true
        bottomNav.selectedItemId = selectedItemId
        isUpdatingSelectedItem = false

        bottomNav.setOnItemSelectedListener { item ->
            // Prevent handling if we're programmatically updating
            if (!isUpdatingSelectedItem) {
                // Only notify if it's a different item
                if (item.itemId != selectedItemId) {
                    selectedItemId = item.itemId
                    navListener?.onFooterItemSelected(item.itemId)
                }
            }
            true
        }
    }
    
    /**
     * Sets the selected navigation item programmatically
     * @param itemId The resource ID of the menu item to select
     * @param notifyListener Whether to notify the listener of this selection (default: false)
     */
    fun setSelectedItem(itemId: Int, notifyListener: Boolean = false) {
        // Set flag to prevent listener from handling this change
        isUpdatingSelectedItem = true
        
        try {
            // Update state
            selectedItemId = itemId
            
            // Update UI if view is initialized
            if (::bottomNav.isInitialized) {
                // Get the menu item and check it
                val menuItem = bottomNav.menu.findItem(itemId)
                menuItem?.isChecked = true
            }
            
            // Notify listener if requested
            if (notifyListener) {
                navListener?.onFooterItemSelected(itemId)
            }
        } finally {
            // Always reset flag even if an exception occurs
            isUpdatingSelectedItem = false
        }
    }
    
    /**
     * Show or hide the entire footer
     * @param visible Whether the footer should be visible
     */
    fun setVisibility(visible: Boolean) {
        view?.visibility = if (visible) View.VISIBLE else View.GONE
    }
    
    /**
     * Enable or disable a specific navigation item
     * @param itemId The resource ID of the menu item to enable/disable
     * @param enabled Whether the item should be enabled
     */
    fun setItemEnabled(itemId: Int, enabled: Boolean) {
        if (::bottomNav.isInitialized) {
            val menuItem = bottomNav.menu.findItem(itemId)
            menuItem?.isEnabled = enabled
        }
    }
} 