package com.example.androidmediapractice.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.androidmediapractice.R

class MainAdapter(private val tasks: List<String>, private val onClick: (Int) -> Unit = {}) : RecyclerView.Adapter<MainAdapter.ViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_main, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tasks[position],onClick)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(task : String, onClick: (Int) -> Unit) {
            (itemView as TextView).text = task
            itemView.setOnClickListener { onClick(layoutPosition) }
        }
    }
}