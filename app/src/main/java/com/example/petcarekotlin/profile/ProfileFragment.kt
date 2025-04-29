package com.example.petcarekotlin.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.petcarekotlin.R
import com.example.petcarekotlin.profile.PetInfoFragment
import com.example.petcarekotlin.profile.GalleryFragment
import com.example.petcarekotlin.profile.VetInfoFragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add child fragments
        childFragmentManager.beginTransaction().apply {
            replace(R.id.pet_info_container, PetInfoFragment())
            replace(R.id.gallery_container, GalleryFragment())
            replace(R.id.vet_info_container, VetInfoFragment())
            commit()
        }
    }
} 