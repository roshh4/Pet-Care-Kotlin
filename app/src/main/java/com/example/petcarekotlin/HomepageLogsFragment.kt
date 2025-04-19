package com.example.petcarekotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

data class PetInfo(
    val name: String,
    val breed: String,
    val age: String,
    val lastFedTime: String,
    val fedBy: String,
    val foodRemaining: String,
    val estimatedDaysLeft: String,
    val vetDateTime: String,
    val vetDetails: String
)

class HomepageLogsFragment : Fragment() {

    private lateinit var petList: List<PetInfo>
    private var currentIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_homepage_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        petList = listOf(
            PetInfo(
                name = "Buddy",
                breed = "Golden Retriever",
                age = "3 years old",
                lastFedTime = "Today, 8:30 AM",
                fedBy = "Mike",
                foodRemaining = "1.2 kg",
                estimatedDaysLeft = "3 days left",
                vetDateTime = "May 15, 2025 - 2:00 PM",
                vetDetails = "Dr. Martinez - Annual Checkup"
            )
        )

        updateUI(view)

        view.findViewById<Button>(R.id.switchPetButton).setOnClickListener {
            currentIndex = (currentIndex + 1) % petList.size
            updateUI(view)
        }
    }

    private fun updateUI(view: View) {
        val pet = petList[currentIndex]

        view.findViewById<TextView>(R.id.petNameTextView).text = "🐾 ${pet.name}"
        view.findViewById<TextView>(R.id.petDetailsTextView).text = "${pet.breed} • ${pet.age}"
        view.findViewById<TextView>(R.id.lastFedTextView).text = pet.lastFedTime
        view.findViewById<TextView>(R.id.fedByTextView).text = pet.fedBy
        view.findViewById<TextView>(R.id.foodRemainingTextView).text = pet.foodRemaining
        view.findViewById<TextView>(R.id.estimatedDaysLeftTextView).text = pet.estimatedDaysLeft
        view.findViewById<TextView>(R.id.vetDateTimeTextView).text = pet.vetDateTime
        view.findViewById<TextView>(R.id.vetDetailsTextView).text = pet.vetDetails
    }
}
