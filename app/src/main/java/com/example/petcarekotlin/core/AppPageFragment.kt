package com.example.petcarekotlin.core

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
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.GravityCompat
import com.example.petcarekotlin.R
import com.example.petcarekotlin.core.FooterFragment
import com.example.petcarekotlin.core.HeaderFragment
import com.example.petcarekotlin.family.ManageFamilyFragment
import com.example.petcarekotlin.foodlogs.FoodLogsFragment
import com.example.petcarekotlin.home.HomePageFragment
import com.example.petcarekotlin.profile.ProfileFragment
import com.example.petcarekotlin.UserProfile.UserProfileFragment
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import android.util.Log
import android.widget.Toast
import com.example.petcarekotlin.home.HomepageLogsFragment

class AppPageFragment : Fragment(), FooterFragment.OnFooterNavigationListener {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sidebarView: View
    private lateinit var sidebarContainer: FrameLayout
    private lateinit var headerFragment: HeaderFragment
    private lateinit var footerFragment: FooterFragment
    
    // Keep track of current main fragment for navigation
    private var currentFragmentTag: String = "home"
    private var currentFragmentId: Int = R.id.nav_home
    
    // Bottom sheet dialog for QR code
    private var inviteQrDialog: BottomSheetDialog? = null

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

        // Get reference to the header and footer fragments
        headerFragment = childFragmentManager.findFragmentById(R.id.header_fragment) as HeaderFragment
        footerFragment = childFragmentManager.findFragmentById(R.id.footer_fragment) as FooterFragment
        
        // Set initial header title
        headerFragment.setTitle("Home")
        
        // Find the sidebar view (first child with layout_gravity=end)
        for (i in 0 until drawerLayout.childCount) {
            val child = drawerLayout.getChildAt(i)
            val params = child.layoutParams as DrawerLayout.LayoutParams
            if (params.gravity == GravityCompat.END) {
                sidebarView = child
                break
            }
        }
        
        // Set the sidebar width to 4/5 of the screen width
        setSidebarWidth()
        
        // Find or create the sidebar container
        sidebarContainer = sidebarView.findViewById(R.id.sidebar_container)
        
        // Close button click handler
        sidebarView.findViewById<ImageView>(R.id.close_button)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        
        // Setup back pressed callback
        setupBackPressedHandler()
        
        // Load UserProfileFragment in the sidebar container
        loadSidebarFragment(UserProfileFragment())
        
        // Load main content - Home fragment
        loadFragment(HomePageFragment(), "home", R.id.nav_home)
    }
    
    private fun setupBackPressedHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    // If drawer is open, close it
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else if (currentFragmentTag != "home") {
                    // If not on home, go to home
                    loadFragment(HomePageFragment(), "home", R.id.nav_home)
                    headerFragment.setTitle("Home")
                    headerFragment.showBackButton(false)
                } else {
                    // Otherwise, allow normal back behavior
                    isEnabled = false
                    requireActivity().onBackPressed()
                    isEnabled = true
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }
    
    private fun setSidebarWidth() {
        // Get screen width
        val displayMetrics = DisplayMetrics()
        val windowManager = requireActivity().windowManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            val screenWidth = bounds.width()
            val params = sidebarView.layoutParams
            params.width = (screenWidth * 4) / 5
            sidebarView.layoutParams = params
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val params = sidebarView.layoutParams
            params.width = (screenWidth * 4) / 5
            sidebarView.layoutParams = params
        }
    }
    
    // Function to open the drawer from HeaderFragment
    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.END)
    }
    
    // Function to close the drawer
    fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.END)
    }
    
    // Replace sidebar content with ManageFamilyFragment
    fun showManageFamilyInSidebar() {
        loadSidebarFragment(ManageFamilyFragment())
    }
    
    // Replace sidebar content with UserProfileFragment
    fun showUserProfileInSidebar() {
        loadSidebarFragment(UserProfileFragment())
    }
    
    // Load fragment in sidebar container
    private fun loadSidebarFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.sidebar_container, fragment)
            .commit()
    }
    
    fun showInviteQrCodeSheet() {
        // Create bottom sheet dialog
        inviteQrDialog = BottomSheetDialog(requireContext())
        
        // Inflate QR code layout
        val inviteView = layoutInflater.inflate(R.layout.layout_invite_qr_code, null)
        
        // Set the content view
        inviteQrDialog?.setContentView(inviteView)
        
        // Set up close button
        inviteView.findViewById<Button>(R.id.close_invite)?.setOnClickListener {
            inviteQrDialog?.dismiss()
        }
        
        // Set height to 3/4 of screen
        val displayMetrics = DisplayMetrics()
        val windowManager = requireActivity().windowManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            val screenHeight = bounds.height()
            val bottomSheetHeight = (screenHeight * 0.75).toInt()
            
            inviteView.findViewById<View>(R.id.close_invite)?.post {
                val bottomSheet = inviteQrDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                val layoutParams = bottomSheet?.layoutParams
                layoutParams?.height = bottomSheetHeight
                bottomSheet?.layoutParams = layoutParams
            }
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenHeight = displayMetrics.heightPixels
            val bottomSheetHeight = (screenHeight * 0.75).toInt()
            
            inviteView.findViewById<View>(R.id.close_invite)?.post {
                val bottomSheet = inviteQrDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                val layoutParams = bottomSheet?.layoutParams
                layoutParams?.height = bottomSheetHeight
                bottomSheet?.layoutParams = layoutParams
            }
        }
        
        // Show the dialog
        inviteQrDialog?.show()
    }

    override fun onFooterItemSelected(itemId: Int) {
        // Don't process if we're already on this fragment
        if (itemId == currentFragmentId) {
            return
        }
        
        // Update current fragment ID for back handling
        currentFragmentId = itemId
        
        val fragment = when (itemId) {
            R.id.nav_home -> {
                headerFragment.setTitle("Home")
                headerFragment.showBackButton(false)
                currentFragmentTag = "home"
                HomePageFragment()
            }
            R.id.nav_profile -> {
                headerFragment.setTitle("Profile")
                headerFragment.showBackButton(true)
                currentFragmentTag = "profile"
                ProfileFragment()
            }
            R.id.nav_foodlogs -> {
                headerFragment.setTitle("Food Logs")
                headerFragment.showBackButton(true)
                currentFragmentTag = "foodlogs"
                
                // We'll let FoodLogsFragment handle getting the current pet ID
                FoodLogsFragment()
            }
            else -> null
        }

        fragment?.let {
            // Load the fragment without updating the footer
            loadFragmentInternal(it, currentFragmentTag)
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String, navId: Int) {
        // Update current fragment tracking
        currentFragmentTag = tag
        currentFragmentId = navId
        
        // Load the fragment
        loadFragmentInternal(fragment, tag)
        
        // Update footer selection
        updateFooterSelection(navId)
    }
    
    private fun loadFragmentInternal(fragment: Fragment, tag: String) {
        // Load the fragment
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }
    
    private fun updateFooterSelection(navId: Int) {
        if (::footerFragment.isInitialized) {
            try {
                footerFragment.setSelectedItem(navId)
            } catch (e: Exception) {
                Log.e("AppPageFragment", "Error updating footer selection", e)
            }
        }
    }
    
    // Handle navigation to a specific section directly
    fun navigateTo(navId: Int) {
        // Only navigate if it's a different destination
        if (navId != currentFragmentId && ::footerFragment.isInitialized) {
            try {
                footerFragment.setSelectedItem(navId, true) // This will trigger onFooterItemSelected
            } catch (e: Exception) {
                Log.e("AppPageFragment", "Error navigating to section", e)
            }
        }
    }

    // Function to show the pet info page
    fun showPetInfoPage() {
        // Get current user ID from shared preferences
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
        val userId = sharedPrefs.getString("CURRENT_USER_ID", "user1")
        val username = sharedPrefs.getString("CURRENT_USERNAME", "unknown")
        
        // Update UI
        headerFragment.setTitle("Pet Details")
        headerFragment.showBackButton(true)
        
        // Create and load the fragment
        val fragment = com.example.petcarekotlin.profile.PetInfoFragment()
        currentFragmentTag = "petinfo"
        
        // Load the fragment
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, "petinfo")
            .commit()
    }

    fun switchToHome() {
        // Update current fragment ID
        currentFragmentId = R.id.nav_home
        currentFragmentTag = "home"
        
        // Update header
        headerFragment.setTitle("Home")
        headerFragment.showBackButton(false)
        
        // Load the HomePageFragment
        val fragment = HomePageFragment()
        loadFragmentInternal(fragment, "home")
        
        // Update footer selection
        footerFragment.setSelectedItem(R.id.nav_home)
    }
} 