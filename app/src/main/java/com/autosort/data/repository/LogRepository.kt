package com.autosort.data.repository

import com.autosort.data.db.LogDao
import com.autosort.data.model.SortLog
import kotlinx.coroutines.flow.Flow

class LogRepository(private val logDao: LogDao) {

    val recentLogs: Flow<List<SortLog>> = logDao.getRecentLogs()

    suspend fun insert(log: SortLog) = logDao.insert(log)

    suspend fun clearAll() = logDao.clearAll()
}
