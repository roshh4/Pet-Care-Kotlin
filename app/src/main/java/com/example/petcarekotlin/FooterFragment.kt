package com.example.petcarekotlin

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomnavigation.BottomNavigationView

class FooterFragment : Fragment() {

    interface OnFooterNavigationListener {
        fun onFooterItemSelected(itemId: Int)
    }

    private var navListener: OnFooterNavigationListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navListener = parentFragment as? OnFooterNavigationListener
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

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottom_nav_menu)

        bottomNav.setOnItemSelectedListener { item ->
            navListener?.onFooterItemSelected(item.itemId)
            true
        }
    }
}
