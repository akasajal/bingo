package com.ishaan.bingo.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardSetupUiState(
    val board: BingoBoard = BingoBoard(),
    val nextNumber: Int = 1,
    val history: List<Int> = emptyList(), // Indices of numbers placed in order
    val isReady: Boolean = false,
    val error: String? = null
)

class BoardSetupViewModel(
    private val repository: GameRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BoardSetupUiState())
    val uiState = _uiState.asStateFlow()

    fun onCellClick(index: Int) {
        _uiState.update { state ->
            if (state.board.numbers[index] != null || state.nextNumber > 25) {
                return@update state
            }

            val newNumbers = state.board.numbers.toMutableList()
            newNumbers[index] = state.nextNumber
            
            state.copy(
                board = BingoBoard(newNumbers),
                nextNumber = state.nextNumber + 1,
                history = state.history + index,
                isReady = (state.nextNumber == 25),
                error = null // Clear error on interaction
            )
        }
    }

    fun submitBoard(roomId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.submitBoard(roomId, _uiState.value.board).onSuccess {
                onComplete()
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }

    fun undo() {
        _uiState.update { state ->
            if (state.history.isEmpty()) return@update state

            val lastIndex = state.history.last()
            val newNumbers = state.board.numbers.toMutableList()
            newNumbers[lastIndex] = null
            
            state.copy(
                board = BingoBoard(newNumbers),
                nextNumber = state.nextNumber - 1,
                history = state.history.dropLast(1),
                isReady = false
            )
        }
    }

    fun delete() {
        _uiState.update { 
            BoardSetupUiState()
        }
    }

    fun loadBoard(board: BingoBoard) {
        _uiState.update { state ->
            // Reconstruct history to enable UNDO on loaded board
            val history = mutableListOf<Int>()
            // Find indices of 1, then 2, etc.
            for (num in 1..25) {
                val index = board.numbers.indexOf(num)
                if (index != -1) history.add(index) else break
            }
            
            state.copy(
                board = board,
                nextNumber = history.size + 1,
                history = history,
                isReady = history.size == 25
            )
        }
    }

    fun saveAsPreset(
        name: String, 
        repository: com.ishaan.bingo.domain.repository.PresetBoardRepository, 
        existingId: String? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val preset = com.ishaan.bingo.domain.model.PresetBoard(
                id = existingId ?: java.util.UUID.randomUUID().toString(),
                name = name,
                board = _uiState.value.board
            )
            
            val result = if (existingId != null) {
                repository.updatePresetBoard(preset)
            } else {
                repository.addPresetBoard(preset)
            }
            
            result.onSuccess {
                onComplete()
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }
}
