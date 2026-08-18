package com.ishaan.bingo.ui.screens.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.repository.GameRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ResultUiState(
    val gameRoom: GameRoom? = null,
    val myBoard: BingoBoard? = null,
    val opponentBoard: BingoBoard? = null,
    val showingOpponentBoard: Boolean = false,
    val isLoading: Boolean = false,
    val isPlayingAgain: Boolean = false,
    val error: String? = null
)

class ResultViewModel(
    private val repository: GameRepository,
    private val roomId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    val myPlayerId = repository.playerId

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getGameRoom(roomId),
                repository.getPlayerBoard(roomId),
                repository.getOpponentBoard(roomId)
            ) { room, playerBoard, opponentBoard ->
                val current = _uiState.value
                current.copy(
                    gameRoom = room,
                    myBoard = playerBoard,
                    opponentBoard = opponentBoard,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleBoardReveal() {
        _uiState.update { it.copy(showingOpponentBoard = !it.showingOpponentBoard) }
    }

    fun playAgain() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlayingAgain = true, error = null) }
            repository.playAgain(roomId).onFailure {
                _uiState.update {
                    it.copy(
                        isPlayingAgain = false,
                        error = "We couldn't start a new game. Check your connection and try again."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
