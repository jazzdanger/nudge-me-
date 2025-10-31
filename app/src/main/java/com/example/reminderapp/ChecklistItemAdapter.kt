package com.example.reminderapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.data.ChecklistItemEntity

class ChecklistItemAdapter(
    private var items: List<ChecklistItemEntity>,
    private val onToggle: (ChecklistItemEntity) -> Unit,
    private val onDelete: (ChecklistItemEntity) -> Unit
) : RecyclerView.Adapter<ChecklistItemAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
        val title: TextView = itemView.findViewById(R.id.textTitle)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.checkBox.isChecked = item.isChecked
        holder.checkBox.setOnClickListener { onToggle(item) }
        holder.deleteButton.setOnClickListener { onDelete(item) }
    }

    fun update(newItems: List<ChecklistItemEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}


