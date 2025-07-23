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
import com.example.diaryapp.ThemeUtils
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest
import android.widget.EditText
import android.widget.Button
import android.view.LayoutInflater as AndroidLayoutInflater

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
        
        // Set theme color only on the app bar (header)
        // val themeColor = ThemeUtils.getCurrentThemeColor(requireActivity())
        // binding.toolbar.setBackgroundColor(themeColor)
        
        val fab = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFab)
        val fabMenu1 = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFabMenu1)
        val fabMenu3 = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFabMenu3)
        val fabMenu4 = binding.root.findViewById<FloatingActionButton>(R.id.addNotesFabMenu4)
        fab.setOnClickListener {
            fabMenuOpen = !fabMenuOpen
            if (fabMenuOpen) {
                fabMenu1.show()
                fabMenu3.show()
                fabMenu4.show()
                fab.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp)
                fab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                fab.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            } else {
                fabMenu1.hide()
                fabMenu3.hide()
                fabMenu4.hide()
                fab.setImageResource(R.drawable.ic_add_white)
                fab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                fab.imageTintList = null
            }
        }
        // Setup RecyclerView for notes
        notesAdapter = NotesAdapter(
            emptyList(),
            onNoteClick = { note ->
                // Check if note requires password protection
                if (note.noteType == "A" || note.noteType == "P") {
                    showPasswordDialog(note)
                } else {
                    // Open EditNoteActivity directly for regular notes
                    openEditNoteActivity(note)
                }
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
                    fabMenu3.hide()
                    fabMenu4.hide()
                    fab.setImageResource(R.drawable.ic_add_white)
                    fab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                    fab.imageTintList = null
                }
            }
        })

        // --- Bottom Notch Navigation Setup ---
        val notchInclude = binding.root.findViewById<View>(R.id.bottomNotchNavInclude)
        val diaryTab = notchInclude.findViewById<View>(R.id.diaryTab)
        val notesTab = notchInclude.findViewById<View>(R.id.notesTab)
        val diaryIcon = notchInclude.findViewById<android.widget.ImageView>(R.id.diaryIcon)
        val notesIcon = notchInclude.findViewById<android.widget.ImageView>(R.id.notesIcon)
        val diaryLabel = notchInclude.findViewById<android.widget.TextView>(R.id.diaryLabel)
        val notesLabel = notchInclude.findViewById<android.widget.TextView>(R.id.notesLabel)

        // Set Notes as selected by default
        diaryTab.isSelected = false
        notesTab.isSelected = true
        diaryIcon.setColorFilter(android.graphics.Color.BLACK)
        diaryLabel.setTextColor(android.graphics.Color.BLACK)
        notesIcon.setColorFilter(android.graphics.Color.WHITE)
        notesLabel.setTextColor(android.graphics.Color.WHITE)

        diaryTab.setOnClickListener {
            // Switch to Diary, update UI and navigate
            diaryTab.isSelected = true
            notesTab.isSelected = false
            diaryIcon.setColorFilter(android.graphics.Color.WHITE)
            diaryLabel.setTextColor(android.graphics.Color.WHITE)
            notesIcon.setColorFilter(android.graphics.Color.BLACK)
            notesLabel.setTextColor(android.graphics.Color.BLACK)
            findNavController().navigate(R.id.nav_home)
        }
        notesTab.setOnClickListener {
            // Already on Notes, just update UI
            diaryTab.isSelected = false
            notesTab.isSelected = true
            diaryIcon.setColorFilter(android.graphics.Color.BLACK)
            diaryLabel.setTextColor(android.graphics.Color.BLACK)
            notesIcon.setColorFilter(android.graphics.Color.WHITE)
            notesLabel.setTextColor(android.graphics.Color.WHITE)
        }
        return root
    }

    private fun showPasswordDialog(note: com.example.diaryapp.data.Note) {
        val dialogView = AndroidLayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_protection, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        submitButton.setOnClickListener {
            val password = passwordInput.text.toString()
            if (validatePassword(password)) {
                dialog.dismiss()
                openEditNoteActivity(note)
            } else {
                Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
                passwordInput.text.clear()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun validatePassword(password: String): Boolean {
        val prefs = getEncryptedPrefs()
        val savedPasswordHash = prefs.getString("password_hash", null)
        return savedPasswordHash != null && hash(password) == savedPasswordHash
    }

    private fun openEditNoteActivity(note: com.example.diaryapp.data.Note) {
        val intent = Intent(requireContext(), com.example.diaryapp.ui.home.EditNoteActivity::class.java)
        intent.putExtra("note_id", note.id)
        intent.putExtra("note_title", note.title)
        intent.putExtra("note_content", note.content)
        intent.putExtra("note_type", note.noteType)
        startActivity(intent)
    }

    private fun getEncryptedPrefs() = EncryptedSharedPreferences.create(
        "diary_auth_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        requireContext(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 