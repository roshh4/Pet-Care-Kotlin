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
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.Button
import android.widget.FrameLayout
import com.example.petcarekotlin.family.ManageFamilyFragment
import com.example.petcarekotlin.profile.UserProfileFragment

class AppPageFragment : Fragment(), FooterFragment.OnFooterNavigationListener {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sidebarView: View
    private lateinit var sidebarContainer: FrameLayout
    
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

        // Find the sidebar view (first child with layout_gravity=end)
        for (i in 0 until drawerLayout.childCount) {
            val child = drawerLayout.getChildAt(i)
            val params = child.layoutParams as DrawerLayout.LayoutParams
            if (params.gravity == androidx.core.view.GravityCompat.END) {
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
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END)
        }
        
        // Load UserProfileFragment in the sidebar container
        loadSidebarFragment(UserProfileFragment())
        
        // Load main content
        loadFragment(HomePageFragment())
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
        drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
    }
    
    // Function to close the drawer
    fun closeDrawer() {
        drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END)
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
