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
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Main Sections
        childFragmentManager.beginTransaction()
            .replace(R.id.pet_info_container, PetInfoFragment())
            .commit()

        childFragmentManager.beginTransaction()
            .replace(R.id.weight_info_container, WeightInfoFragment())
            .commit()

        childFragmentManager.beginTransaction()
            .replace(R.id.gallery_container, GalleryFragment())
            .commit()

        childFragmentManager.beginTransaction()
            .replace(R.id.vet_info_container, VetInfoFragment())
            .commit()
    }
}
