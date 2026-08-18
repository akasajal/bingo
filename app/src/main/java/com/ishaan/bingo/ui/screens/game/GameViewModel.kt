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

                val myPlayer = uiState.value.gameRoom?.players?.get(repository.playerId) ?: return@combine
                
                // Calculate actual progress based on current board and called numbers
                val detectedLines = lineDetector.detectCompletedLines(board.numbers, calledNumbers.toSet())
                val currentCompletedLines = myPlayer.completedLines.toSet()
                
                // Only sync if we have actually found new lines not yet on the server
                val newlyCompletedLines = detectedLines - currentCompletedLines
                
                if (newlyCompletedLines.isNotEmpty()) {
                    val newProgress = (myPlayer.bingoProgress + newlyCompletedLines.size).coerceAtMost(5)
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
        _uiState.update { it.copy(isCallingNumber = true) }
        
        val currentRoom = _uiState.value.gameRoom ?: return
        if (currentRoom.currentTurnPlayerId != repository.playerId) return

        // Optimistic update — reflect the call instantly in the UI
        // Fix #3: Removed optimistic turn flip to avoid race conditions with server response
        val optimisticRoom = currentRoom.copy(
            calledNumbers = currentRoom.calledNumbers + number,
            callerMap = currentRoom.callerMap + (number.toString() to repository.playerId)
        )
        _uiState.update { it.copy(gameRoom = optimisticRoom, error = null) }

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
        return when (error) {
            is java.net.ConnectException -> "No internet connection"
            is java.util.concurrent.TimeoutException -> "Request timed out"
            else -> "Something went wrong. Please try again."
        }
    }
}
