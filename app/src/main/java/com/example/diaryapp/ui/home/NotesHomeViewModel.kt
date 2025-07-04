package com.example.diaryapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.diaryapp.DiaryDatabase
import com.example.diaryapp.data.Note
import kotlinx.coroutines.launch

class NotesHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val noteDao = DiaryDatabase.getDatabase(application).noteDao()
    val notes: LiveData<List<Note>> = noteDao.getAllNotes()

    fun insert(note: Note) {
        viewModelScope.launch {
            noteDao.insert(note)
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch {
            noteDao.delete(note)
        }
    }

    fun update(note: Note) {
        viewModelScope.launch {
            noteDao.update(note)
        }
    }
} 