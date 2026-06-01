package com.example.data

import kotlinx.coroutines.flow.Flow

class DictionaryRepository(
    private val dao: DictionaryDao,
    private val assets: android.content.res.AssetManager
) {

    val offlineWordIndex: List<String> by lazy {
        try {
            assets.open("words.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    val allEntries: Flow<List<DictionaryEntry>> = dao.getAllEntries()
    val favorites: Flow<List<DictionaryEntry>> = dao.getFavorites()
    val history: Flow<List<DictionaryEntry>> = dao.getHistory()
    val categories: Flow<List<String>> = dao.getCategories()

    fun searchEntries(query: String): Flow<List<DictionaryEntry>> {
        return dao.searchEntries(query)
    }

    fun getEntriesByCategory(category: String): Flow<List<DictionaryEntry>> {
        return dao.getEntriesByCategory(category)
    }

    fun getEntryById(id: Int): Flow<DictionaryEntry?> {
        return dao.getEntryById(id)
    }

    suspend fun getEntryByNormalizedWord(normalizedWord: String): DictionaryEntry? {
        return dao.getEntryByNormalizedWord(normalizedWord)
    }

    suspend fun insertEntry(entry: DictionaryEntry): Long {
        return dao.insertEntry(entry)
    }

    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean) {
        dao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun updateLastViewedTimestamp(id: Int, timestamp: Long) {
        dao.updateLastViewedTimestamp(id, timestamp)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun updateEntry(entry: DictionaryEntry) {
        dao.updateEntry(entry)
    }
}
