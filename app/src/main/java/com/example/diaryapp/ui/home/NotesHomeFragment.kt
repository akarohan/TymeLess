package com.example.diaryapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.diaryapp.R
import com.example.diaryapp.databinding.FragmentNotesHomeBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.os.Handler
import android.os.Looper

class NotesHomeFragment : Fragment() {
    private var _binding: FragmentNotesHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var notesViewModel: NotesHomeViewModel
    private var fabMenuOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val fab = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFab)
        val fabMenu1 = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFabMenu1)
        val fabMenu2 = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFabMenu2)
        val fabMenu3 = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFabMenu3)
        val fabMenu4 = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFabMenu4)
        val fabMenu5 = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFabMenu5)
        fab.setOnClickListener {
            fabMenuOpen = !fabMenuOpen
            if (fabMenuOpen) {
                fabMenu1.show()
                fabMenu2.show()
                fabMenu3.show()
                fabMenu4.show()
                fabMenu5.show()
                fab.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp)
                fab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                fab.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            } else {
                fabMenu1.hide()
                fabMenu2.hide()
                fabMenu3.hide()
                fabMenu4.hide()
                fabMenu5.hide()
                fab.setImageResource(R.drawable.ic_add_white)
                fab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                fab.imageTintList = null
            }
        }
        fabMenu5.setOnClickListener {
            findNavController().navigate(R.id.nav_home)
        }
        // Setup RecyclerView for notes
        notesAdapter = NotesAdapter(
            emptyList(),
            onNoteClick = { note ->
                // Open EditNoteActivity for editing
                val intent = Intent(requireContext(), com.example.diaryapp.ui.home.EditNoteActivity::class.java)
                intent.putExtra("note_id", note.id)
                intent.putExtra("note_title", note.title)
                intent.putExtra("note_content", note.content)
                intent.putExtra("note_type", note.noteType)
                // Add extras for images/audio if needed
                startActivity(intent)
            },
            onNoteDelete = { note ->
                // Show confirmation dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to permanently delete this note?")
                    .setPositiveButton("Delete") { _, _ ->
                        notesViewModel.delete(note)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.notesRecyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.notesRecyclerView.adapter = notesAdapter
        // Setup ViewModel
        notesViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)).get(NotesHomeViewModel::class.java)
        notesViewModel.notes.observe(viewLifecycleOwner) { notes ->
            Log.d("NotesHomeFragment", "Notes updated: count=${notes.size}, titles=${notes.map { it.title }}")
            notesAdapter.updateNotes(notes)
        }
        // N button creates new note (open EditNoteActivity)
        fabMenu1.setOnClickListener {
            val intent = Intent(requireContext(), com.example.diaryapp.ui.home.EditNoteActivity::class.java)
            startActivity(intent)
        }
        fabMenu4.setOnClickListener {
            val intent = Intent(requireContext(), com.example.diaryapp.ui.home.EditNoteActivity::class.java)
            intent.putExtra("note_type", "P")
            startActivity(intent)
        }
        fabMenu3.setOnClickListener {
            val intent = Intent(requireContext(), com.example.diaryapp.ui.home.EditNoteActivity::class.java)
            intent.putExtra("note_type", "A")
            startActivity(intent)
        }
        // Add scroll listener to retract menu on scroll
        binding.notesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (fabMenuOpen && (dx != 0 || dy != 0)) {
                    fabMenuOpen = false
                    fabMenu1.hide()
                    fabMenu2.hide()
                    fabMenu3.hide()
                    fabMenu4.hide()
                    fabMenu5.hide()
                    fab.setImageResource(R.drawable.ic_add_white)
                    fab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                    fab.imageTintList = null
                }
            }
        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 