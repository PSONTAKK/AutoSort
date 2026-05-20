package com.autosort.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * V3: Caches the result of AI analysis to save credits and allow offline viewing.
 */
@Entity(tableName = "ai_insights")
data class AiInsight(
    @PrimaryKey
    val folderPath: String,     // Target folder path serves as the unique ID
    val timestamp: Long,        // When this insight was generated
    val summary: String,        // Raw Gemini API JSON or formatted response
    val fileCountAtScan: Int    // Number of files when scanned
)
