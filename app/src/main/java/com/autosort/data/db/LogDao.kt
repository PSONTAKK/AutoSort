package com.autosort.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autosort.data.model.SortLog
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SortLog)

    @Query("SELECT * FROM logs ORDER BY time DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<SortLog>>

    @Query("DELETE FROM logs")
    suspend fun clearAll()

    /** V-04: Keep only the most recent 500 log entries */
    @Query("DELETE FROM logs WHERE id NOT IN (SELECT id FROM logs ORDER BY time DESC LIMIT 500)")
    suspend fun pruneOldLogs()

    /** V3: Get logs since a specific timestamp for monthly reports */
    @Query("SELECT * FROM logs WHERE time >= :since ORDER BY time DESC")
    suspend fun getLogsSince(since: Long): List<SortLog>
}
