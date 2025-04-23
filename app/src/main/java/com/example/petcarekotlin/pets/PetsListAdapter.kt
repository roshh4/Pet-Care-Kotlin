package com.example.petcarekotlin.pets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R
import com.example.petcarekotlin.models.PetModel

class PetsListAdapter(
    private var petsList: List<PetModel> = emptyList(),
    private val onPetClicked: (PetModel) -> Unit
) : RecyclerView.Adapter<PetsListAdapter.PetViewHolder>() {

    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val petImage: ImageView = itemView.findViewById(R.id.petImage)
        val petName: TextView = itemView.findViewById(R.id.petName)
        val petDetails: TextView = itemView.findViewById(R.id.petDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = petsList[position]
        
        holder.petName.text = pet.name
        holder.petDetails.text = "${pet.breed}, ${pet.age} years"
        
        // Set a default pet image
        holder.petImage.setImageResource(R.drawable.ic_pet_placeholder)
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onPetClicked(pet)
        }
    }

    override fun getItemCount(): Int = petsList.size

    fun updatePets(newPets: List<PetModel>) {
        petsList = newPets
        notifyDataSetChanged()
    }
} 