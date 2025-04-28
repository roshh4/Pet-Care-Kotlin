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
        
        // Display pet name or fallback to "Unnamed Pet" if empty
        holder.petName.text = if (pet.name.isNotBlank()) pet.name else "Unnamed Pet"
        
        // Build and display pet details
        val details = buildString {
            // Add species if available
            if (pet.species.isNotBlank() && pet.species != "Unknown") {
                append(pet.species)
                
                // Add breed if available and not Unknown
                if (pet.breed.isNotBlank() && pet.breed != "Unknown") {
                    append(" (${pet.breed})")
                }
                
                // Add comma before age if both species and age exist
                if (pet.age.isNotBlank() && pet.age != "0") {
                    append(", ")
                }
            }
            
            // Add age if available
            if (pet.age.isNotBlank() && pet.age != "0") {
                append("${pet.age} years")
            }
            
            // Add weight if available
            if (pet.weight.isNotBlank() && pet.weight != "0") {
                if (length > 0) append(", ")
                append("${pet.weight} kg")
            }
            
            // Default text if no details available
            if (isEmpty()) {
                append("No details available")
            }
        }
        
        holder.petDetails.text = details
        
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