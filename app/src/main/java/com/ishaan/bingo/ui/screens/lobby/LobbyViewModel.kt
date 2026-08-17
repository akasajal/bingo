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
                _uiState.update { it.copy(isLoading = false, joinedRoomId = room.id, gameCode = room.code) }
                observeRoom(room.id)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun joinGame(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.joinRoom(code).onSuccess { room ->
                _uiState.update { it.copy(isLoading = false, joinedRoomId = room.id) }
                observeRoom(room.id)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    private fun observeRoom(roomId: String) {
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

    fun resetLobby() {
        _uiState.value = LobbyUiState()
    }
}
