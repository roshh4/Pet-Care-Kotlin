package com.example.petcarekotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HomepageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the homepage layout which contains the two FrameLayouts
        return inflater.inflate(R.layout.fragment_home_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add both fragments into their respective containers
        childFragmentManager.beginTransaction()
            .replace(R.id.logs_container, HomepageLogsFragment())
            .replace(R.id.activity_container, ActivityFeedFragment())
            .commit()
    }
}
