package com.example.petcarekotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the profile layout (your middle content)
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load HeaderFragment into header container
        childFragmentManager.beginTransaction()
            .replace(R.id.header_container, HeaderFragment())
            .commit()

        // Load FooterFragment into footer container
        childFragmentManager.beginTransaction()
            .replace(R.id.footer_container, FooterFragment())
            .commit()
    }
}
