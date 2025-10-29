package com.example.reminderapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView

sealed class SettingItem {
    data class Simple(val title: String) : SettingItem()
    data class Toggle(val title: String, val description: String, var enabled: Boolean, val onToggle: (Boolean) -> Unit) : SettingItem()
}

class SettingsAdapter(
    private val items: List<SettingItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SIMPLE = 0
        private const val TYPE_TOGGLE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SettingItem.Simple -> TYPE_SIMPLE
            is SettingItem.Toggle -> TYPE_TOGGLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TOGGLE -> ToggleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_setting_toggle, parent, false))
            else -> SimpleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_setting, parent, false))
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SettingItem.Simple -> (holder as SimpleViewHolder).bind(item)
            is SettingItem.Toggle -> (holder as ToggleViewHolder).bind(item)
        }
    }

    inner class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.settingItemText)
        fun bind(item: SettingItem.Simple) {
            textView.text = item.title
        }
    }

    inner class ToggleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.settingToggleTitle)
        private val desc: TextView = itemView.findViewById(R.id.settingToggleDescription)
        private val toggle: Switch = itemView.findViewById(R.id.settingToggleSwitch)
        fun bind(item: SettingItem.Toggle) {
            title.text = item.title
            desc.text = item.description
            toggle.isChecked = item.enabled
            toggle.setOnCheckedChangeListener { _, checked ->
                item.onToggle(checked)
            }
        }
    }
}
