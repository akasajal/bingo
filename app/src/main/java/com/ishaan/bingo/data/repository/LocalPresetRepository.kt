package com.ishaan.bingo.data.repository

import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.PresetBoard
import com.ishaan.bingo.domain.repository.PresetBoardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalPresetRepository(private val db: BingoDatabase) : PresetBoardRepository {

    private val dao = db.presetBoardDao()

    override fun getPresetBoards(): Flow<List<PresetBoard>> =
        dao.getAllPresets().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addPresetBoard(preset: PresetBoard): Result<Unit> = runCatching {
        if (dao.getCount() >= 6) throw Exception("Maximum 6 presets allowed")
        val newNumbers = preset.board.numbers.joinToString(",")
        if (dao.getAllNumbers().any { it == newNumbers })
            throw Exception("This board arrangement already exists as a preset")
        dao.insert(preset.toEntity())
    }

    override suspend fun updatePresetBoard(preset: PresetBoard): Result<Unit> = runCatching {
        val updatedNumbers = preset.board.numbers.joinToString(",")
        if (dao.getAllNumbersExcluding(preset.id).any { it == updatedNumbers })
            throw Exception("This arrangement already exists in another preset")
        dao.update(preset.toEntity())
    }

    override suspend fun deletePresetBoard(id: String): Result<Unit> = runCatching {
        dao.deleteById(id)
    }

    private fun PresetBoardEntity.toDomain() = PresetBoard(
        id = id,
        name = name,
        board = BingoBoard(numbers.split(",").map { it.toInt() })
    )

    private fun PresetBoard.toEntity() = PresetBoardEntity(
        id = id,
        name = name,
        numbers = board.numbers.joinToString(",")
    )
}