package com.example.petcarekotlin.core

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import com.example.petcarekotlin.R
import com.example.petcarekotlin.core.AppPageFragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HeaderFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HeaderFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var titleTextView: TextView
    private lateinit var backButton: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var appLogo: ImageView
    
    // Fixed title - will never change during navigation
    private val fixedHeaderTitle: String = "Pet Care Reminder"
    
    // Listener for back button click (we'll still keep this for functionality)
    private var onBackClickListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_header, container, false)
        
        // Initialize views
        titleTextView = view.findViewById(R.id.header_title)
        backButton = view.findViewById(R.id.back_button)
        profileIcon = view.findViewById(R.id.profile_icon)
        appLogo = view.findViewById(R.id.app_logo)
        
        // Set the fixed header title
        titleTextView.text = fixedHeaderTitle
        
        // Always show the paw icon instead of back button
        backButton.visibility = View.GONE
        appLogo.visibility = View.VISIBLE
        
        // Keep back navigation functionality by making the paw icon act as a back button when needed
        appLogo.setOnClickListener {
            if (onBackClickListener != null) {
                onBackClickListener?.invoke()
            }
        }
        
        // Set click listener for profile icon
        profileIcon.setOnClickListener {
            // Find the parent fragment and open the drawer
            val parentFragment = parentFragment
            if (parentFragment is AppPageFragment) {
                // Open the drawer in the parent AppPageFragment
                parentFragment.openDrawer()
            }
        }
        
        return view
    }

    /**
     * Title will always be "Pet Care Reminder"
     */
    fun setTitle(title: String) {
        // Do nothing - we keep the fixed title
        if (::titleTextView.isInitialized) {
            titleTextView.text = fixedHeaderTitle
        }
    }
    
    /**
     * We always show the paw logo now, but we still track if we're in a "back" state
     * to determine the behavior of the paw icon click
     */
    fun showBackButton(show: Boolean) {
        // In this updated version, we always show the paw icon
        // but we'll change its click behavior based on whether we're on a details page
        
        if (::appLogo.isInitialized) {
            if (show) {
                // Make the paw logo clickable for back navigation
                appLogo.isClickable = true
                appLogo.isFocusable = true
            } else {
                // On main screens, the paw logo is just decorative
                appLogo.isClickable = false
                appLogo.isFocusable = false
                // Clear any click listeners
                appLogo.setOnClickListener(null)
            }
        }
    }
    
    /**
     * Set a custom action for back navigation
     */
    fun setOnBackClickListener(listener: () -> Unit) {
        onBackClickListener = listener
        
        // Update the paw logo to use this listener if it's initialized
        if (::appLogo.isInitialized) {
            appLogo.setOnClickListener {
                listener.invoke()
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HeaderFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HeaderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
} 