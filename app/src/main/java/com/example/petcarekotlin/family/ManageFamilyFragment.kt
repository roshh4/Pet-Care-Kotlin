package com.example.petcarekotlin.family

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import android.util.DisplayMetrics
import com.example.petcarekotlin.core.AppPageFragment
import com.example.petcarekotlin.R
import com.google.android.material.bottomsheet.BottomSheetDialog

class ManageFamilyFragment : Fragment() {
    
    private var inviteQrDialog: BottomSheetDialog? = null
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_manage_family, container, false)
        
        // Set up back button
        view.findViewById<ImageView>(R.id.back_button)?.setOnClickListener {
            // Return to UserProfileFragment in sidebar
            (parentFragment as? AppPageFragment)?.showUserProfileInSidebar()
        }
        
        // Set up close button
        view.findViewById<ImageView>(R.id.close_button)?.setOnClickListener {
            // Close the drawer
            (parentFragment as? AppPageFragment)?.closeDrawer()
        }
        
        // Set up invite new member button
        view.findViewById<Button>(R.id.invite_new_member_button)?.setOnClickListener {
            showInviteQrCodeSheet()
        }
        
        // Set up member options
        setupMemberOptions(view.findViewById(R.id.member1_options))
        setupMemberOptions(view.findViewById(R.id.member2_options))
        
        // Set width to 4/5 of the screen
        setLayoutWidth(view)
        
        return view
    }
    
    private fun setLayoutWidth(view: View) {
        // Get screen width
        val displayMetrics = DisplayMetrics()
        val windowManager = requireActivity().windowManager
        
        view.post {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                val screenWidth = bounds.width()
                val params = view.layoutParams
                params.width = (screenWidth * 4) / 5
                view.layoutParams = params
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels
                val params = view.layoutParams
                params.width = (screenWidth * 4) / 5
                view.layoutParams = params
            }
        }
    }
    
    private fun setupMemberOptions(optionsButton: ImageView?) {
        optionsButton?.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(R.menu.member_options_menu, popup.menu)
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_remove -> {
                        // Handle remove action
                        Toast.makeText(requireContext(), "Member removed from family", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_make_owner -> {
                        // Handle make owner action
                        Toast.makeText(requireContext(), "Ownership transferred", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            
            popup.show()
        }
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
    
    override fun onDestroy() {
        super.onDestroy()
        // Dismiss dialog if it's showing when fragment is destroyed
        if (inviteQrDialog?.isShowing == true) {
            inviteQrDialog?.dismiss()
        }
    }
} 