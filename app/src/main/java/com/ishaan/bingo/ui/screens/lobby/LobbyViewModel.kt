package com.ishaan.bingo.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishaan.bingo.domain.repository.GameRepository
import com.ishaan.bingo.domain.model.GameStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LobbyUiState(
    val gameCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val joinedRoomId: String? = null,
    val shouldNavigateToSetup: Boolean = false
)

class LobbyViewModel(
    private val repository: GameRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState = _uiState.asStateFlow()

    fun createGame() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.createRoom().onSuccess { room ->
                // Store the room ID and show the waiting dialog, then watch for
                // the opponent to join (which flips status to BOARD_SETUP).
                _uiState.update { it.copy(isLoading = false, joinedRoomId = room.id, gameCode = room.code) }
                observeRoomForCreator(room.id)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun joinGame(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.joinRoom(code).onSuccess { room ->
                // The joiner's room is already BOARD_SETUP at this point (joinRoom sets it).
                // Set both roomId and shouldNavigateToSetup in one atomic update so the
                // LaunchedEffect in LobbyScreen always sees a non-null joinedRoomId when
                // it reads shouldNavigateToSetup = true.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        joinedRoomId = room.id,
                        shouldNavigateToSetup = room.status == GameStatus.BOARD_SETUP
                    )
                }
                // If for some reason the room isn't BOARD_SETUP yet, fall back to observing.
                if (room.status != GameStatus.BOARD_SETUP) {
                    observeRoomForCreator(room.id)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    /**
     * Used by the creator (and as a fallback for the joiner) to watch for the room
     * transitioning to BOARD_SETUP, which signals both players are present.
     */
    private fun observeRoomForCreator(roomId: String) {
        viewModelScope.launch {
            repository.getGameRoom(roomId).collectLatest { room ->
                if (room?.status == GameStatus.BOARD_SETUP) {
                    _uiState.update { it.copy(shouldNavigateToSetup = true) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Resets all navigation and game state so the screen is ready for a new session. */
    fun resetLobby() {
        _uiState.update { LobbyUiState() }
    }
}