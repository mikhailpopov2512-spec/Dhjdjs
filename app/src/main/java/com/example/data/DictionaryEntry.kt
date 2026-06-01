package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary_entries",
    indices = [
        Index(value = ["normalizedWord"], unique = true),
        Index(value = ["word"]),
        Index(value = ["lastViewedTimestamp"])
    ]
)
data class DictionaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String, // E.g. "АВО́СЬ" (upper with stress)
    val normalizedWord: String, // E.g. "авось" (lower, stripped)
    val definition: String, // Ozhegov definition
    val category: String, // Start letter "А"
    val isUserAdded: Boolean = false, // Added through AI Gemini search
    val isFavorite: Boolean = false, // Is starred
    val lastViewedTimestamp: Long = 0L // Timestamp of when it was viewed, 0 if never
)
