package com.example.petcarekotlin.foodlogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R

// Keep the data class here
data class FoodLog(
    val time: String,
    val author: String,
    val amount: String
)

class FoodLogAdapter(private val logs: MutableList<FoodLog>) :
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
    
    // Update logs with new data
    fun updateLogs(newLogs: List<FoodLog>) {
        logs.clear()
        logs.addAll(newLogs)
        notifyDataSetChanged()
    }
} 