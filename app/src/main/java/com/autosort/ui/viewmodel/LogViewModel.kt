package com.autosort.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autosort.data.db.AppDatabase
import com.autosort.data.model.SortLog
import com.autosort.data.repository.LogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LogRepository = LogRepository(
        AppDatabase.getInstance(application).logDao()
    )

    val logs: StateFlow<List<SortLog>> = repository.recentLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
