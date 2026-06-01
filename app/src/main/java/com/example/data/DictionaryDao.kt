package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary_entries ORDER BY word ASC")
    fun getAllEntries(): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM dictionary_entries WHERE normalizedWord LIKE :searchQuery || '%' OR normalizedWord LIKE '% ' || :searchQuery || '%' ORDER BY word ASC")
    fun searchEntries(searchQuery: String): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM dictionary_entries WHERE normalizedWord = :normalizedWord LIMIT 1")
    suspend fun getEntryByNormalizedWord(normalizedWord: String): DictionaryEntry?

    @Query("SELECT * FROM dictionary_entries WHERE id = :id LIMIT 1")
    fun getEntryById(id: Int): Flow<DictionaryEntry?>

    @Query("SELECT * FROM dictionary_entries WHERE isFavorite = 1 ORDER BY word ASC")
    fun getFavorites(): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM dictionary_entries WHERE lastViewedTimestamp > 0 ORDER BY lastViewedTimestamp DESC")
    fun getHistory(): Flow<List<DictionaryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DictionaryEntry): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<DictionaryEntry>)

    @Update
    suspend fun updateEntry(entry: DictionaryEntry)

    @Query("UPDATE dictionary_entries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("UPDATE dictionary_entries SET lastViewedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastViewedTimestamp(id: Int, timestamp: Long)

    @Query("UPDATE dictionary_entries SET lastViewedTimestamp = 0")
    suspend fun clearHistory()

    @Query("SELECT DISTINCT category FROM dictionary_entries ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT * FROM dictionary_entries WHERE category = :category ORDER BY word ASC")
    fun getEntriesByCategory(category: String): Flow<List<DictionaryEntry>>

    @Query("DELETE FROM dictionary_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Int)

    @Query("DELETE FROM dictionary_entries WHERE isUserAdded = 1")
    suspend fun clearUserAddedEntries()
}
