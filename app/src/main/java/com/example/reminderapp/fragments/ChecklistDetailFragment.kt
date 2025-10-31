package com.example.reminderapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.ChecklistItemAdapter
import com.example.reminderapp.R
import com.example.reminderapp.data.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChecklistDetailFragment : Fragment() {
    private lateinit var titleView: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var editNewItem: EditText
    private lateinit var addButton: Button

    private lateinit var repository: ChecklistRepository
    private var currentList: ChecklistListEntity? = null
    private lateinit var adapter: ChecklistItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_checklist_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = ReminderDatabase.getDatabase(requireContext())
        repository = ChecklistRepository(db.checklistDao())

        titleView = view.findViewById(R.id.textListTitle)
        recycler = view.findViewById(R.id.recyclerChecklist)
        editNewItem = view.findViewById(R.id.editNewItem)
        addButton = view.findViewById(R.id.buttonAddItem)

        adapter = ChecklistItemAdapter(emptyList(), onToggle = { item ->
            lifecycleScope.launch { repository.toggleItem(item) }
        }, onDelete = { item ->
            lifecycleScope.launch { repository.deleteItem(item) }
        })
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val listId = arguments?.getLong("list_id", -1L) ?: -1L
        observeListAndItems(listId)

        addButton.setOnClickListener {
            val text = editNewItem.text.toString().trim()
            if (text.isNotEmpty()) {
                val listId = currentList?.id
                if (listId != null) {
                    lifecycleScope.launch { repository.addItem(listId, text) }
                    editNewItem.setText("")
                }
            }
        }

        titleView.setOnClickListener { showRenameDialog() }
    }

    private fun observeListAndItems(listId: Long) {
        lifecycleScope.launch {
            if (listId > 0) {
                repository.getListById(listId).collectLatest { list ->
                    if (list != null) {
                        currentList = list
                        titleView.text = list.name
                        observeItems(list.id)
                    }
                }
            } else {
                repository.getOrCreateDefaultList().collectLatest { list ->
                    if (list == null) {
                        lifecycleScope.launch { repository.ensureDefaultListExists() }
                    } else {
                        currentList = list
                        titleView.text = list.name
                        observeItems(list.id)
                    }
                }
            }
        }
    }

    private fun observeItems(listId: Long) {
        lifecycleScope.launch {
            repository.getItems(listId).collectLatest { items ->
                adapter.update(items)
            }
        }
    }

    private fun showRenameDialog() {
        val input = EditText(requireContext())
        input.setText(currentList?.name ?: "")
        AlertDialog.Builder(requireContext())
            .setTitle("Rename list")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                val list = currentList
                if (name.isNotEmpty() && list != null) {
                    lifecycleScope.launch { repository.renameList(list, name) }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}


