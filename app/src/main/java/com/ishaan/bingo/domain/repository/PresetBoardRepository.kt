package com.ishaan.bingo.domain.repository

import com.ishaan.bingo.domain.model.PresetBoard
import kotlinx.coroutines.flow.Flow

interface PresetBoardRepository {
    fun getPresetBoards(): Flow<List<PresetBoard>>
    suspend fun addPresetBoard(preset: PresetBoard): Result<Unit>
    suspend fun updatePresetBoard(preset: PresetBoard): Result<Unit>
    suspend fun deletePresetBoard(id: String): Result<Unit>
}
