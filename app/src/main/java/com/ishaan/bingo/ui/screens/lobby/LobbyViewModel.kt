package com.ishaan.bingo.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.repository.GameRepository
import com.ishaan.bingo.ui.AppViewModelProvider
import kotlinx.coroutines.Job
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
    val shouldNavigateToSetup: Boolean = false,
    val isBotGame: Boolean = false
)

class LobbyViewModel(
    private val repository: GameRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState = _uiState.asStateFlow()

    // Track the observer job so we can cancel it before starting a new game
    private var roomObserverJob: Job? = null

    fun createGame() {
        // Cancel any leftover observer from a previous session before starting fresh
        roomObserverJob?.cancel()
        roomObserverJob = null

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.createRoom().onSuccess { room ->
                _uiState.update { it.copy(isLoading = false, joinedRoomId = room.id, gameCode = room.code) }
                observeRoomForCreator(room.id)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun joinGame(code: String) {
        roomObserverJob?.cancel()
        roomObserverJob = null

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.joinRoom(code).onSuccess { room ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        joinedRoomId = room.id,
                        shouldNavigateToSetup = room.status == GameStatus.BOARD_SETUP
                    )
                }
                if (room.status != GameStatus.BOARD_SETUP) observeRoomForCreator(room.id)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun playWithBot() {
        roomObserverJob?.cancel()
        roomObserverJob = null

        val localRepo = AppViewModelProvider.freshBotRepository()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            localRepo.createRoom().onSuccess { room ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        joinedRoomId = room.id,
                        shouldNavigateToSetup = true,
                        isBotGame = true
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    private fun observeRoomForCreator(roomId: String) {
        roomObserverJob = viewModelScope.launch {
            repository.getGameRoom(roomId).collectLatest { room ->
                if (room?.status == GameStatus.BOARD_SETUP) {
                    _uiState.update { it.copy(shouldNavigateToSetup = true) }
                }
            }
        }
    }

    fun consumeNavigation() { _uiState.update { it.copy(shouldNavigateToSetup = false) } }

    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun resetLobby() {
        // Cancel any in-flight room observer so it can't update state after reset
        roomObserverJob?.cancel()
        roomObserverJob = null
        _uiState.update { LobbyUiState() }
    }
}