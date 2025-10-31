package com.example.reminderapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.data.ChecklistListEntity

class ChecklistListAdapter(
    private var lists: List<ChecklistListEntity>,
    private val getCount: (Long) -> Int,
    private val onClick: (ChecklistListEntity) -> Unit
) : RecyclerView.Adapter<ChecklistListAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.textListName)
        val count: TextView = itemView.findViewById(R.id.textCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist_list, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = lists.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val list = lists[position]
        holder.name.text = list.name
        holder.count.text = getCount(list.id).toString()
        holder.itemView.setOnClickListener { onClick(list) }
    }

    fun update(newLists: List<ChecklistListEntity>) {
        lists = newLists
        notifyDataSetChanged()
    }
}


