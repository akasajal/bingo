package com.ishaan.bingo.data.repository

import com.ishaan.bingo.domain.model.PresetBoard
import com.ishaan.bingo.domain.repository.PresetBoardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocalPresetRepository : PresetBoardRepository {
    private val _presets = MutableStateFlow<List<PresetBoard>>(emptyList())
    val presets = _presets.asStateFlow()

    override fun getPresetBoards(): Flow<List<PresetBoard>> = presets

    override suspend fun addPresetBoard(preset: PresetBoard): Result<Unit> = runCatching {
        if (_presets.value.size >= 6) {
            throw Exception("Maximum 6 presets allowed")
        }
        _presets.update { it + preset }
    }

    override suspend fun updatePresetBoard(preset: PresetBoard): Result<Unit> = runCatching {
        _presets.update { list ->
            list.map { if (it.id == preset.id) preset else it }
        }
    }

    override suspend fun deletePresetBoard(id: String): Result<Unit> = runCatching {
        _presets.update { list -> list.filter { it.id != id } }
    }
}
