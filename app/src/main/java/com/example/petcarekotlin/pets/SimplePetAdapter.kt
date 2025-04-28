package com.example.petcarekotlin.pets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R
import com.example.petcarekotlin.models.PetModel

class SimplePetAdapter(
    private val petList: List<PetModel>,
    private val onPetClicked: (PetModel) -> Unit
) : RecyclerView.Adapter<SimplePetAdapter.PetViewHolder>() {

    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val petIcon: ImageView = itemView.findViewById(R.id.petIcon)
        val petName: TextView = itemView.findViewById(R.id.petName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = petList[position]
        
        holder.petName.text = pet.name
        
        // You can set a specific icon based on pet species if needed
        // holder.petIcon.setImageResource(getIconForSpecies(pet.species))
        
        holder.itemView.setOnClickListener {
            onPetClicked(pet)
        }
    }

    override fun getItemCount(): Int = petList.size
} 