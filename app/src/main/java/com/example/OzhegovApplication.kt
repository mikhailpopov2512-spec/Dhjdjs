package com.example

import android.app.Application
import com.example.data.DictionaryDatabase
import com.example.data.DictionaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class OzhegovApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { DictionaryDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { DictionaryRepository(database.dictionaryDao()) }
}
