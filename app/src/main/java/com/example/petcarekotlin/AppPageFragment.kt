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

class AppPageFragment : Fragment(), FooterFragment.OnFooterNavigationListener {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sidebarView: View
    private lateinit var manageFamilyView: View
    private var isShowingManageFamily = false
    
    // Bottom sheet dialog for QR code
    private var inviteQrDialog: BottomSheetDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_page, container, false)
        
        // Initialize the drawer layout
        drawerLayout = view.findViewById(R.id.drawer_layout)
        
        // Inflate manage family view for later use
        manageFamilyView = inflater.inflate(R.layout.layout_manage_family, null)
        
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
        
        // Set up manage family view
        setupManageFamilyView()
        
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
        // Make sure we show the main sidebar, not manage family
        if (isShowingManageFamily) {
            showMainSidebar()
        }
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
            // Navigate to ManageFamilyFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ManageFamilyFragment())
                .addToBackStack(null)
                .commit()
            drawerLayout.closeDrawers()
        }
        
        view.findViewById<View>(R.id.generate_invite)?.setOnClickListener {
            // Show invite QR code
            showInviteQrCodeSheet()
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
    
    private fun setupManageFamilyView() {
        // Back button click handler
        manageFamilyView.findViewById<ImageView>(R.id.back_button)?.setOnClickListener {
            showMainSidebar()
        }
        
        // Close button click handler
        manageFamilyView.findViewById<ImageView>(R.id.close_button)?.setOnClickListener {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END)
        }
        
        // Invite new member button
        manageFamilyView.findViewById<Button>(R.id.invite_new_member_button)?.setOnClickListener {
            showInviteQrCodeSheet()
        }
    }
    
    fun showManageFamilyScreen() {
        // Get parent view group of the drawer
        val parent = drawerLayout
        
        // Remove the current sidebar view
        parent.removeView(sidebarView)
        
        // Apply the drawer parameters to manage family view
        manageFamilyView.layoutParams = DrawerLayout.LayoutParams(
            sidebarView.layoutParams.width,
            ViewGroup.LayoutParams.MATCH_PARENT,
            androidx.core.view.GravityCompat.END
        )
        
        // Add manage family view to drawer
        parent.addView(manageFamilyView)
        
        // Update the sidebar reference
        sidebarView = manageFamilyView
        isShowingManageFamily = true
    }
    
    private fun showMainSidebar() {
        if (!isShowingManageFamily) return
        
        // Get original sidebar from its parent
        val originalSidebar = drawerLayout.getChildAt(0)
        
        // Get parent view group of the drawer
        val parent = drawerLayout
        
        // Remove the manage family view
        parent.removeView(manageFamilyView)
        
        // Add original sidebar back
        parent.addView(originalSidebar)
        
        // Update sidebar reference
        sidebarView = originalSidebar
        isShowingManageFamily = false
    }
    
    private fun showInviteQrCodeSheet() {
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
