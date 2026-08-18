package com.ishaan.bingo.data.repository

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetBoardDao {
    @Query("SELECT * FROM preset_boards ORDER BY rowid ASC")
    fun getAllPresets(): Flow<List<PresetBoardEntity>>

    @Query("SELECT COUNT(*) FROM preset_boards")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: PresetBoardEntity)

    @Update
    suspend fun update(preset: PresetBoardEntity)

    @Query("DELETE FROM preset_boards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT numbers FROM preset_boards")
    suspend fun getAllNumbers(): List<String>

    @Query("SELECT numbers FROM preset_boards WHERE id != :excludeId")
    suspend fun getAllNumbersExcluding(excludeId: String): List<String>
}