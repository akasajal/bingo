package com.ishaan.bingo.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishaan.bingo.domain.model.BotDifficulty
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
    private var roomCreationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.prepareSession()
        }
    }

    fun createGame() {
        // Cancel any leftover observer from a previous session before starting fresh
        roomObserverJob?.cancel()
        roomObserverJob = null
        roomCreationJob?.cancel()

        // Generating a room ID and share code is local and should never wait on Firebase.
        val roomDraft = repository.createRoomDraft()
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                joinedRoomId = roomDraft.id,
                gameCode = roomDraft.code
            )
        }

        roomCreationJob = viewModelScope.launch {
            repository.createRoom(roomDraft).onSuccess { room ->
                if (_uiState.value.joinedRoomId != roomDraft.id) return@onSuccess
                _uiState.update { it.copy(isLoading = false) }
                observeRoomForCreator(room.id)
            }.onFailure {
                if (_uiState.value.joinedRoomId != roomDraft.id) return@onFailure
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameCode = "",
                        joinedRoomId = null,
                        error = "We couldn't create your game. Check your internet connection and try again."
                    )
                }
            }
        }
    }

    fun joinGame(code: String) {
        if (_uiState.value.isLoading) return
        roomObserverJob?.cancel()
        roomObserverJob = null

        viewModelScope.launch {
            // Optimistic navigation: If we've pre-warmed, assume the join will work.
            // This masks the Firestore transaction time.
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            repository.joinRoom(code).onSuccess { room ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        joinedRoomId = room.id,
                        shouldNavigateToSetup = true
                    )
                }
                if (room.status != GameStatus.BOARD_SETUP) observeRoomForCreator(room.id)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = mapError(error)) }
            }
        }
    }

    fun onCodeInputChanged(code: String) {
        if (code.isNotEmpty()) {
            // Pre-warm the Firebase connection as soon as typing starts
            viewModelScope.launch {
                repository.prepareSession()
            }
        }
    }

    fun playWithBot(difficulty: BotDifficulty) {
        roomObserverJob?.cancel()
        roomObserverJob = null

        val localRepo = AppViewModelProvider.freshBotRepository(difficulty)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            localRepo.createRoom(localRepo.createRoomDraft()).onSuccess { room ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        joinedRoomId = room.id,
                        shouldNavigateToSetup = true,
                        isBotGame = true
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = mapError(error)) }
            }
        }
    }

    private fun observeRoomForCreator(roomId: String) {
        roomObserverJob = viewModelScope.launch {
            repository.getGameRoom(roomId).collectLatest { room ->
                if (room?.status == GameStatus.BOARD_SETUP) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            joinedRoomId = room.id,
                            shouldNavigateToSetup = true
                        )
                    }
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
        roomCreationJob?.cancel()
        roomCreationJob = null
        _uiState.update { LobbyUiState() }
    }

    private fun mapError(error: Throwable): String {
        val message = error.message ?: ""
        return when {
            message.contains("Room is full", ignoreCase = true) -> "This game already has two players."
            message.contains("Room not found", ignoreCase = true) -> "We couldn't find a game with that code."
            message.contains("PERMISSION_DENIED", ignoreCase = true) -> "Access denied. Please try again."
            else -> "Something went wrong. Please check your connection and try again."
        }
    }
}
