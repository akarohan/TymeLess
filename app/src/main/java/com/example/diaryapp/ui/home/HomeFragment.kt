package com.example.diaryapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diaryapp.DiaryEntry
import com.example.diaryapp.EditEntryActivity
import com.example.diaryapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import de.hdodenhof.circleimageview.CircleImageView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.diaryapp.R
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DiaryEntryAdapter
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val swipeRefreshLayout = binding.swipeRefreshLayout
        recyclerView = binding.diaryRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DiaryEntryAdapter(emptyList(), { entry ->
            val intent = Intent(requireContext(), EditEntryActivity::class.java)
            intent.putExtra("entry_date", entry.date)
            startActivity(intent)
        }, { entry ->
            // Show confirmation dialog before deleting
            val context = requireContext()
            val builder = androidx.appcompat.app.AlertDialog.Builder(context)
            val dialog = builder.setMessage("Do you want to delete this Page ?")
                .setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    // Delete only the selected entry from database and refresh
                    lifecycleScope.launch {
                        val db = com.example.diaryapp.DiaryDatabase.getDatabase(context)
                        db.diaryEntryDao().deleteEntryByDate(entry.date)
                        homeViewModel.loadEntries()
                        // Show Snackbar with Undo
                        val rootView = requireActivity().findViewById<View>(android.R.id.content)
                        Snackbar.make(rootView, "Page deleted", 10000)
                            .setAction("Undo") {
                                lifecycleScope.launch {
                                    db.diaryEntryDao().insertOrUpdate(entry)
                                    homeViewModel.loadEntries()
                                }
                            }.show()
                    }
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .create()
            dialog.show()
            dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(android.graphics.Color.BLACK)
        })
        recyclerView.adapter = adapter

        homeViewModel.entries.observe(viewLifecycleOwner) {
            adapter.updateEntries(it)
        }
        homeViewModel.loadEntries()

        swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.loadEntries()
            swipeRefreshLayout.isRefreshing = false
        }

        val fab = binding.addEntryFab
        val fabMenu1 = binding.root.findViewById<FloatingActionButton>(R.id.addEntryFabMenu1)
        val fabMenu2 = binding.root.findViewById<FloatingActionButton>(R.id.addEntryFabMenu2)
        var fabMenuOpen = false
        fab.setOnClickListener {
            fabMenuOpen = !fabMenuOpen
            if (fabMenuOpen) {
                fabMenu1.show()
                fabMenu2.show()
                fab.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp)
                fab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                fab.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            } else {
                fabMenu1.hide()
                fabMenu2.hide()
                fab.setImageResource(R.drawable.ic_add_white)
                fab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                fab.imageTintList = null
            }
        }

        fabMenu1.setOnClickListener {
            findNavController().navigate(R.id.nav_notes_home)
        }

        fabMenu2.setOnClickListener {
            val intent = Intent(requireContext(), EditEntryActivity::class.java)
            // No extras: new blank entry
            startActivity(intent)
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (fabMenuOpen && (dx != 0 || dy != 0)) {
                    fabMenuOpen = false
                    fabMenu1.hide()
                    fabMenu2.hide()
                    fab.setImageResource(R.drawable.ic_add_white)
                    fab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                    fab.imageTintList = null
                }
            }
        })

        return root
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.loadEntries()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}