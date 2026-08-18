package com.ishaan.bingo.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.repository.GameRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GameUiState(
    val gameRoom: GameRoom? = null,
    val playerBoard: BingoBoard? = null,
    val isLoading: Boolean = false,
    val isCallingNumber: Boolean = false,
    val error: String? = null
)

class GameViewModel(
    val repository: GameRepository,
    private val roomId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        observeGame()
    }

    private fun observeGame() {
        viewModelScope.launch {
            repository.getGameRoom(roomId).collect { room ->
                _uiState.update { it.copy(gameRoom = room, isLoading = false) }
            }
        }

        viewModelScope.launch {
            repository.getPlayerBoard(roomId).collect { board ->
                _uiState.update { it.copy(playerBoard = board) }
            }
        }
    }

    fun callNumber(number: Int) {
        if (_uiState.value.isCallingNumber) return
        val currentRoom = _uiState.value.gameRoom ?: return

        // Optimistic update — reflect the call instantly in the UI
        val optimisticRoom = currentRoom.copy(
            calledNumbers = currentRoom.calledNumbers + number,
            callerMap = currentRoom.callerMap + (number.toString() to repository.playerId),
            // Flip turn optimistically so the UI stops showing "YOUR TURN" immediately
            currentTurnPlayerId = currentRoom.players.keys
                .firstOrNull { it != repository.playerId } ?: currentRoom.currentTurnPlayerId
        )
        _uiState.update { it.copy(gameRoom = optimisticRoom, isCallingNumber = true, error = null) }

        viewModelScope.launch {
            repository.callNumber(roomId, currentRoom.players.keys, number)
                .onSuccess {
                    _uiState.update { it.copy(isCallingNumber = false) }
                }
                .onFailure { error ->
                    // Roll back to the real server state on failure
                    _uiState.update {
                        it.copy(
                            gameRoom = currentRoom,
                            isCallingNumber = false,
                            error = error.message
                        )
                    }
                }
        }
    }
}
