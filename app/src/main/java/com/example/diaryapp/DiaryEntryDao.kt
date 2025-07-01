package com.example.diaryapp

import androidx.room.*

@Dao
interface DiaryEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries WHERE date = :date")
    suspend fun getEntriesByDate(date: Long): List<DiaryEntry>

    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    suspend fun getAllEntries(): List<DiaryEntry>

    @Query("DELETE FROM diary_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM diary_entries WHERE date = :date")
    suspend fun deleteEntryByDate(date: Long)
} 