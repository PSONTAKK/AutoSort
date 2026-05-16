package com.autosort.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autosort.data.model.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: Rule)

    @Update
    suspend fun update(rule: Rule)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM rules ORDER BY name ASC")
    fun getAllRules(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE active = 1")
    suspend fun getActiveRules(): List<Rule>

    @Query("SELECT * FROM rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Rule?
}
