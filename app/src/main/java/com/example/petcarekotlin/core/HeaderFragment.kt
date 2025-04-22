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
    
    // Title text to be displayed in the header
    private var headerTitle: String = "Pet Care"
    
    // Flag to control back button visibility
    private var showBackButton: Boolean = false
    
    // Listener for back button click
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
        
        // Set the header title
        titleTextView.text = headerTitle
        
        // Set back button visibility
        updateBackButtonVisibility()
        
        // Set click listener for profile icon
        profileIcon.setOnClickListener {
            // Find the parent fragment and open the drawer
            val parentFragment = parentFragment
            if (parentFragment is AppPageFragment) {
                // Open the drawer in the parent AppPageFragment
                parentFragment.openDrawer()
            }
        }
        
        // Set click listener for back button
        backButton.setOnClickListener {
            onBackClickListener?.invoke() ?: activity?.onBackPressed()
        }
        
        return view
    }

    /**
     * Set the title to be displayed in the header
     */
    fun setTitle(title: String) {
        headerTitle = title
        if (::titleTextView.isInitialized) {
            titleTextView.text = title
        }
    }
    
    /**
     * Show or hide the back button
     */
    fun showBackButton(show: Boolean) {
        showBackButton = show
        if (::backButton.isInitialized) {
            updateBackButtonVisibility()
        }
    }
    
    /**
     * Update visibility of back button and app logo
     */
    private fun updateBackButtonVisibility() {
        if (showBackButton) {
            backButton.visibility = View.VISIBLE
            appLogo.visibility = View.GONE
        } else {
            backButton.visibility = View.GONE
            appLogo.visibility = View.VISIBLE
        }
    }
    
    /**
     * Set a custom action for the back button
     */
    fun setOnBackClickListener(listener: () -> Unit) {
        onBackClickListener = listener
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