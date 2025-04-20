package com.example.petcarekotlin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FoodLogAdapter(private val logs: List<FoodLog>) :
    RecyclerView.Adapter<FoodLogAdapter.FoodLogViewHolder>() {

    inner class FoodLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeText: TextView = itemView.findViewById(R.id.logTime)
        val authorText: TextView = itemView.findViewById(R.id.logAuthor)
        val amountText: TextView = itemView.findViewById(R.id.logAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_log, parent, false)
        return FoodLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodLogViewHolder, position: Int) {
        val log = logs[position]
        holder.timeText.text = log.time
        holder.authorText.text = "by ${log.author}"
        holder.amountText.text = log.amount
    }

    override fun getItemCount() = logs.size
}
