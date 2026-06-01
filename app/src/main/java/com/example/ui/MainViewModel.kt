package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GeminiClient
import com.example.api.GeneratedContentRequest
import com.example.api.Part
import com.example.api.GenerationConfig
import com.example.data.DictionaryEntry
import com.example.data.DictionaryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(private val repository: DictionaryRepository) : ViewModel() {

    // --- Search & Filtering State ---
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    
    // --- Currently Opened/Details State ---
    val selectedWord = MutableStateFlow<DictionaryEntry?>(null)
    
    // --- Word of the Day State ---
    val wordOfTheDay = MutableStateFlow<DictionaryEntry?>(null)

    // --- active UI Tab: "dashboard", "browse", "favorites", "history" ---
    val currentTab = MutableStateFlow("dashboard")

    // --- AI Definition Resolution States ---
    val isAiLoading = MutableStateFlow(false)
    val aiError = MutableStateFlow<String?>(null)

    // --- Database Data Sources (Reactive Flows as mandated) ---
    val allEntries: StateFlow<List<DictionaryEntry>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<DictionaryEntry>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<DictionaryEntry>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyUrlList())

    val categories: StateFlow<List<String>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper for initial empty history list
    private fun emptyUrlList() = emptyList<DictionaryEntry>()

    // Reactive search stream with debounce for efficient querying
    val searchResults: StateFlow<List<DictionaryEntry>> = searchQuery
        .debounce(250)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchEntries(normalizeRussianWord(query))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Hydrate a "Word of the Day" on launch from our preseeded database words
        viewModelScope.launch {
            repository.allEntries.firstOrNull()?.let { list ->
                if (list.isNotEmpty()) {
                    // Pick a word based on current calendar day to keep it consistent for the day
                    val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                    val index = dayOfYear % list.size
                    wordOfTheDay.value = list[index]
                }
            }
            
            // If the database is initially empty (waiting for creation callback to finalize), 
            // listen until words are inserted and set the Word of the Day dynamically.
            repository.allEntries.collect { list ->
                if (list.isNotEmpty() && wordOfTheDay.value == null) {
                    val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                    val index = dayOfYear % list.size
                    wordOfTheDay.value = list[index]
                }
            }
        }
    }

    // --- Actions ---

    fun setQuery(query: String) {
        searchQuery.value = query
        if (query.isNotBlank() && currentTab.value != "search") {
            // Auto open search tab if user starts typing from elsewhere
            currentTab.value = "search"
        }
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun selectTab(tab: String) {
        currentTab.value = tab
    }

    fun selectWord(entry: DictionaryEntry?) {
        selectedWord.value = entry
        if (entry != null) {
            viewModelScope.launch {
                repository.updateLastViewedTimestamp(entry.id, System.currentTimeMillis())
            }
        }
    }

    fun toggleFavorite(entry: DictionaryEntry) {
        viewModelScope.launch {
            val updatedStatus = !entry.isFavorite
            repository.updateFavoriteStatus(entry.id, updatedStatus)
            // If the currently viewed details word is updated, sync its state
            if (selectedWord.value?.id == entry.id) {
                selectedWord.value = selectedWord.value?.copy(isFavorite = updatedStatus)
            }
            // Keep Word of the day in sync
            if (wordOfTheDay.value?.id == entry.id) {
                wordOfTheDay.value = wordOfTheDay.value?.copy(isFavorite = updatedStatus)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun resolveWordDetailsOffline(wordString: String) {
        viewModelScope.launch {
            val normalizedQuery = normalizeRussianWord(wordString)
            val existing = repository.getEntryByNormalizedWord(normalizedQuery)
            if (existing != null) {
                selectWord(existing)
            } else {
                // If it is entirely missing offline, trigger AI definition directly!
                fetchAiDefinition(wordString)
            }
        }
    }

    fun fetchAiDefinition(wordStr: String) {
        if (wordStr.isBlank()) return
        
        isAiLoading.value = true
        aiError.value = null
        
        viewModelScope.launch {
            try {
                // 1. Check if we already have it locally
                val normalized = normalizeRussianWord(wordStr)
                val existing = repository.getEntryByNormalizedWord(normalized)
                if (existing != null) {
                    selectWord(existing)
                    isAiLoading.value = false
                    return@launch
                }

                // 2. Fetch via Gemini API REST Client
                val cleanedWord = wordStr.trim().uppercase(Locale.getDefault())
                val prompt = "Объясни значение русского слова \"$cleanedWord\". Напиши только словарное толкование в строгом ожеговском стиле."
                
                val sysInstruction = "Ты — выдающийся лингвист, эксперт по русскому языку и преемником С. И. Ожегова. Твоя единственная цель — составить подробное толкование предложенного пользователем слова строго в лаконичном, академическом, толковом стиле классического Словаря Ожегова.\n\n" +
                        "Формат ответа должен быть безупречно структурирован:\n" +
                        "1. Заголовок слова заглавными буквами с указанием знака ударения (используй символ ударения \\u0301 сразу после ударной гласной, например: АВО́СЬ, ВЕЛИКОДУ́ШИЕ, СЧА́СТЬЕ).\n" +
                        "2. Часть речи, изменения (например, -я, м. или -и, ж.) и стилистические пометы (например: существительное, женский род; глагол, совершенный вид, переходный; разговорное, книжное, устарелое) на новой строке.\n" +
                        "3. Точное толкование одного или нескольких пронумерованных значений слова.\n" +
                        "4. Несколько примеров употребления из живого русского языка или классической литературы, оформленные курсивом (используй префикс '— ' или разметку Markdown для курсива).\n\n" +
                        "Крайне важно: не пиши никакого лишнего текста вокруг определения (не здоровайся, не пиши 'Вот определение:', 'Слово означает:' или вежливые вступления). Возвращай исключительно готовое словарное определение."

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("API ключ Gemini не настроен. Пожалуйста, укажите действующий ключ GEMINI_API_KEY в панели Secrets во вкладке настроек Google AI Studio.")
                }

                val request = GeneratedContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = sysInstruction))),
                    generationConfig = GenerationConfig(temperature = 0.3f, topP = 0.95f)
                )

                val response = GeminiClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!responseText.isNullOrBlank()) {
                    // Create entry and insert to expand the dynamic dictionary offline catalog
                    val firstLetter = cleanedWord.firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "А"
                    val newEntry = DictionaryEntry(
                        word = cleanedWord,
                        normalizedWord = normalized,
                        category = firstLetter,
                        definition = responseText.trim(),
                        isUserAdded = true,
                        lastViewedTimestamp = System.currentTimeMillis()
                    )
                    
                    val insertedId = repository.insertEntry(newEntry)
                    val insertedEntry = newEntry.copy(id = insertedId.toInt())
                    
                    selectWord(insertedEntry)
                } else {
                    throw Exception("Не удалось получить текстовый ответ от искусственного интеллекта. Повторите запрос.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                aiError.value = e.localizedMessage ?: "Произошла неизвестная ошибка при обращении к AI-Переводчику"
            } finally {
                isAiLoading.value = false
            }
        }
    }

    // --- Helpers ---
    
    fun normalizeRussianWord(word: String): String {
        return word
            .lowercase()
            .replace("\u0301", "")
            .replace("\u0300", "")
            .replace("ё", "е")
            .trim()
    }
}

// --- ViewModel Factory ---

class MainViewModelFactory(private val repository: DictionaryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
