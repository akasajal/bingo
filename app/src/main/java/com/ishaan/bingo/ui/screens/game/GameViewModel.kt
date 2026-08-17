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
        viewModelScope.launch {
            repository.callNumber(roomId, number).onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }
}
