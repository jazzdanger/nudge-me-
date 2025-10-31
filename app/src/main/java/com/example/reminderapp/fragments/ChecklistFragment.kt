package com.example.reminderapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reminderapp.R
import com.example.reminderapp.ChecklistListAdapter
import com.example.reminderapp.data.ChecklistRepository
import com.example.reminderapp.data.ReminderDatabase
import com.example.reminderapp.data.ReminderRepository
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChecklistFragment : Fragment() {
    private lateinit var textCount: TextView
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var listsRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var addListButton: Button
    private lateinit var checklistRepository: ChecklistRepository
    private lateinit var listAdapter: ChecklistListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_checklist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = ReminderDatabase.getDatabase(requireContext())
        reminderRepository = ReminderRepository(database.reminderDao())
        checklistRepository = ChecklistRepository(database.checklistDao())

        listsRecycler = view.findViewById(R.id.recyclerLists)
        addListButton = view.findViewById(R.id.buttonAddList)

        setupLists()
    }

    private fun setupLists() {
        listAdapter = ChecklistListAdapter(emptyList(), getCount = { id ->
            // Synchronously read latest count snapshot held in a var updated by a collector
            counts[id] ?: 0
        }, onClick = { list ->
            val frag = ChecklistDetailFragment()
            val args = Bundle()
            args.putLong("list_id", list.id)
            frag.arguments = args
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(com.example.reminderapp.R.id.fragmentContainer, frag)
                .addToBackStack(null)
                .commit()
        })
        listsRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        listsRecycler.adapter = listAdapter

        observeListsAndCounts()

        addListButton.setOnClickListener {
            val input = android.widget.EditText(requireContext())
            input.hint = "List name"
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("New list")
                .setView(input)
                .setPositiveButton("Create") { _, _ ->
                    val name = input.text.toString().trim().ifEmpty { "List" }
                    lifecycleScope.launch { checklistRepository.createList(name) }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private val counts = mutableMapOf<Long, Int>()

    private fun observeListsAndCounts() {
        lifecycleScope.launch {
            checklistRepository.getAllLists().collectLatest { lists ->
                listAdapter.update(lists)
                // For each list, start a collector for its count
                lists.forEach { list ->
                    lifecycleScope.launch {
                        checklistRepository.getItemsCount(list.id).collectLatest { c ->
                            counts[list.id] = c
                            listAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }
}


