package com.example.petcarekotlin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import android.widget.TextView
import android.widget.ImageView
import android.view.ViewGroup.LayoutParams
import android.util.DisplayMetrics
import android.view.WindowInsets

class AppPageFragment : Fragment(), FooterFragment.OnFooterNavigationListener {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sidebarView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_page, container, false)
        
        // Initialize the drawer layout
        drawerLayout = view.findViewById(R.id.drawer_layout)
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the sidebar view (first child with layout_gravity=end)
        for (i in 0 until drawerLayout.childCount) {
            val child = drawerLayout.getChildAt(i)
            val params = child.layoutParams as DrawerLayout.LayoutParams
            if (params.gravity == androidx.core.view.GravityCompat.END) {
                sidebarView = child
                break
            }
        }
        
        // Set the sidebar width to half of the screen width
        setSidebarHalfWidth()
        
        // Set up sidebar menu items
        setupSidebarMenu(sidebarView)
        
        // Close button click handler
        sidebarView.findViewById<ImageView>(R.id.close_button)?.setOnClickListener {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END)
        }
        
        loadFragment(HomePageFragment())
    }
    
    private fun setSidebarHalfWidth() {
        // Get screen width
        val displayMetrics = DisplayMetrics()
        val windowManager = requireActivity().windowManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            val screenWidth = bounds.width()
            val params = sidebarView.layoutParams
            params.width = screenWidth / 2
            sidebarView.layoutParams = params
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val params = sidebarView.layoutParams
            params.width = screenWidth / 2
            sidebarView.layoutParams = params
        }
    }
    
    // Function to open the drawer from HeaderFragment
    fun openDrawer() {
        drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
    }
    
    private fun setupSidebarMenu(view: View) {
        // Your Pets section
        view.findViewById<View>(R.id.pet_buddy)?.setOnClickListener {
            // Handle pet click
            drawerLayout.closeDrawers()
        }
        
        view.findViewById<View>(R.id.pet_whiskers)?.setOnClickListener {
            // Handle pet click
            drawerLayout.closeDrawers()
        }
        
        view.findViewById<View>(R.id.pet_rex)?.setOnClickListener {
            // Handle pet click
            drawerLayout.closeDrawers()
        }
        
        // Pet Family section
        view.findViewById<View>(R.id.manage_family)?.setOnClickListener {
            // Handle manage family
            drawerLayout.closeDrawers()
        }
        
        view.findViewById<View>(R.id.generate_invite)?.setOnClickListener {
            // Handle generate invite
            drawerLayout.closeDrawers()
        }
        
        view.findViewById<View>(R.id.leave_family)?.setOnClickListener {
            // Handle leave family
            drawerLayout.closeDrawers()
        }
        
        // Theme setting
        view.findViewById<View>(R.id.theme_setting)?.setOnClickListener {
            // Handle theme setting
            drawerLayout.closeDrawers()
        }
        
        // Logout button
        view.findViewById<View>(R.id.logout_button)?.setOnClickListener {
            // Navigate back to login
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginPageFragment())
                .commit()
            drawerLayout.closeDrawers()
        }
    }

    override fun onFooterItemSelected(itemId: Int) {
        val fragment = when (itemId) {
            R.id.nav_home -> HomePageFragment()
            R.id.nav_profile -> ProfileFragment()
            R.id.nav_foodlogs -> FoodLogsFragment()
            else -> null
        }

        fragment?.let {
            loadFragment(it)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
