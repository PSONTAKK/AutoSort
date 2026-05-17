package com.autosort.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogStatus {
    SUCCESS,
    FAIL_IO,
    SKIPPED,
    WARNING
}

@Entity(tableName = "logs")
data class SortLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val time: Long = System.currentTimeMillis(),
    val name: String,
    val target: String,
    val status: LogStatus
)
