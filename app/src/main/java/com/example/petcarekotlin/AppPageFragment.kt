package com.example.petcarekotlin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class AppPageFragment : Fragment(), FooterFragment.OnFooterNavigationListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load Home on startup
        loadFragment(HomePageFragment())
    }

    override fun onFooterItemSelected(itemId: Int) {
        val fragment = when (itemId) {
            R.id.nav_home -> HomePageFragment()
            R.id.nav_profile -> ProfileFragment()
            R.id.nav_reminders -> RemindersFragment()
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
