package com.autosort.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autosort.data.model.AiInsight
import kotlinx.coroutines.flow.Flow

@Dao
interface AiInsightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: AiInsight)

    @Query("SELECT * FROM ai_insights WHERE folderPath = :path LIMIT 1")
    suspend fun getInsight(path: String): AiInsight?

    @Query("SELECT * FROM ai_insights ORDER BY timestamp DESC")
    fun getAllInsights(): Flow<List<AiInsight>>

    @Query("DELETE FROM ai_insights WHERE folderPath = :path")
    suspend fun deleteInsight(path: String)
}
