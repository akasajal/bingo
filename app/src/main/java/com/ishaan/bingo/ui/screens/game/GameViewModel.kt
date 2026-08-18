package com.ishaan.bingo.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.repository.GameRepository
import com.ishaan.bingo.game.BingoLineDetector
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GameUiState(
    val gameRoom: GameRoom? = null,
    val playerBoard: BingoBoard? = null,
    val localBingoProgress: Int = 0,
    val isLoading: Boolean = false,
    val isCallingNumber: Boolean = false,
    val error: String? = null
)

class GameViewModel(
    val repository: GameRepository,
    private val roomId: String,
    private val lineDetector: BingoLineDetector = BingoLineDetector()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        observeGame()
        setupLocalProgressionSync()
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

    /**
     * Decentralized Sync Logic:
     * We watch for changes in calledNumbers, calculate our own progress locally,
     * and push it to the server if it has changed.
     */
    private fun setupLocalProgressionSync() {
        viewModelScope.launch {
            combine(
                uiState.map { it.gameRoom?.calledNumbers }.distinctUntilChanged(),
                uiState.map { it.playerBoard }.distinctUntilChanged()
            ) { calledNumbers, board ->
                if (calledNumbers == null || board == null) return@combine

                // Calculate actual progress based on current board and called numbers
                val detectedLines = lineDetector.detectCompletedLines(board.numbers, calledNumbers.toSet())
                val newProgress = detectedLines.size.coerceAtMost(5)

                // Update local state immediately for zero-latency UI feedback
                _uiState.update { it.copy(localBingoProgress = newProgress) }

                val myPlayer = uiState.value.gameRoom?.players?.get(repository.playerId) ?: return@combine
                val currentCompletedLines = myPlayer.completedLines.toSet()
                
                // Only sync if we have actually found new lines not yet on the server
                val newlyCompletedLines = detectedLines - currentCompletedLines
                
                if (newlyCompletedLines.isNotEmpty()) {
                    // Double check we aren't re-syncing the same progress
                    if (newProgress > myPlayer.bingoProgress) {
                        val claimWin = newProgress >= 5
                        
                        repository.syncMyProgress(
                            roomId = roomId,
                            progress = newProgress,
                            completedLines = detectedLines.toList(),
                            claimWin = claimWin
                        )
                    }
                }
            }.collect()
        }
    }

    fun callNumber(number: Int) {
        if (_uiState.value.isCallingNumber) return
        val currentRoom = _uiState.value.gameRoom ?: return
        if (currentRoom.currentTurnPlayerId != repository.playerId) return

        // Atomic Optimistic Update — reflects call instantly and sets loading guard
        _uiState.update { 
            val optimisticRoom = currentRoom.copy(
                calledNumbers = currentRoom.calledNumbers + number,
                callerMap = currentRoom.callerMap + (number.toString() to repository.playerId),
                // Restore optimistic turn flip for snappier UI
                currentTurnPlayerId = currentRoom.players.keys
                    .firstOrNull { it != repository.playerId } ?: currentRoom.currentTurnPlayerId
            )
            it.copy(gameRoom = optimisticRoom, isCallingNumber = true, error = null) 
        }

        viewModelScope.launch {
            repository.callNumber(roomId, number)
                .onSuccess {
                    _uiState.update { it.copy(isCallingNumber = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            gameRoom = currentRoom,
                            isCallingNumber = false,
                            error = mapError(error)
                        )
                    }
                }
        }
    }

    private fun mapError(error: Throwable): String {
        val message = error.message ?: ""
        return when {
            error is java.net.ConnectException || message.contains("offline", true) -> "No internet connection"
            error is java.util.concurrent.TimeoutException -> "Request timed out"
            message.contains("PERMISSION_DENIED", ignoreCase = true) -> "Access denied. Please try again."
            else -> "Something went wrong. Please try again."
        }
    }
}
