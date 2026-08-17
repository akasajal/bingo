package com.ishaan.bingo.data.repository

import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.model.Player
import com.ishaan.bingo.domain.repository.GameRepository
import com.ishaan.bingo.game.BingoGameEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * A mock repository that simulates a 2-player game locally for testing/debugging.
 * It auto-joins a second player when a room is created.
 */
class LocalGameRepository(
    private val gameEngine: BingoGameEngine = BingoGameEngine(),
    override val playerId: String = "local-player-1"
) : GameRepository {

    private val player2Id = "local-player-2"
    private val _room = MutableStateFlow<GameRoom?>(null)
    private val _boards = MutableStateFlow<Map<String, BingoBoard>>(emptyMap())

    override fun getGameRoom(roomId: String): Flow<GameRoom?> = _room.asStateFlow()

    override fun getPlayerBoard(roomId: String): Flow<BingoBoard?> = _boards.asStateFlow().map { it[playerId] }

    override fun getOpponentBoard(roomId: String): Flow<BingoBoard?> = _boards.asStateFlow().map { boards ->
        boards.keys.firstOrNull { it != playerId }?.let { boards[it] }
    }

    override suspend fun createRoom(): Result<GameRoom> {
        val room = GameRoom(
            id = "mock-room",
            code = "DEBUG",
            status = GameStatus.WAITING_FOR_PLAYER,
            players = mapOf(playerId to Player(id = playerId, name = "You"))
        )
        _room.value = room
        
        // Auto-join a second player after a short delay to simulate "waiting"
        kotlinx.coroutines.delay(1000)
        joinRoom("DEBUG")
        
        return Result.success(room)
    }

    override suspend fun joinRoom(code: String): Result<GameRoom> {
        val currentRoom = _room.value ?: return Result.failure(Exception("No room to join"))
        if (currentRoom.players.size >= 2) return Result.success(currentRoom)

        val p2 = Player(id = player2Id, name = "Opponent (Bot)")
        val updatedRoom = currentRoom.copy(
            players = currentRoom.players + (player2Id to p2),
            status = GameStatus.BOARD_SETUP
        )
        _room.value = updatedRoom
        
        // Auto-submit board for P2
        val p2Board = BingoBoard((1..25).toList().shuffled())
        _boards.update { it + (player2Id to p2Board) }
        
        return Result.success(updatedRoom)
    }

    override suspend fun submitBoard(roomId: String, board: BingoBoard): Result<Unit> {
        _boards.update { it + (playerId to board) }
        
        _room.update { room ->
            val r = room ?: return@update null
            val updatedPlayers = r.players.toMutableMap()
            updatedPlayers[playerId] = updatedPlayers[playerId]?.copy(isReady = true) ?: return@update r
            
            // In mock mode, P2 is always ready
            updatedPlayers[player2Id] = updatedPlayers[player2Id]?.copy(isReady = true) ?: return@update r

            r.copy(
                players = updatedPlayers,
                status = GameStatus.PLAYING,
                currentTurnPlayerId = playerId // You start first
            )
        }
        return Result.success(Unit)
    }

    override suspend fun callNumber(roomId: String, number: Int): Result<Unit> {
        val room = _room.value ?: return Result.failure(Exception("Room not found"))
        val boards = _boards.value
        
        // Process your call
        val updatedRoom = gameEngine.processCall(room, boards, number)
        _room.value = updatedRoom
        
        // If it's now Opponent's turn, simulate their move
        if (updatedRoom.status == GameStatus.PLAYING && updatedRoom.currentTurnPlayerId == player2Id) {
            simulateOpponentMove(updatedRoom, boards)
        }
        
        return Result.success(Unit)
    }

    private suspend fun simulateOpponentMove(room: GameRoom, boards: Map<String, BingoBoard>) {
        kotlinx.coroutines.delay(1500) // Thinking time
        val p2Board = boards[player2Id] ?: return
        val availableNumbers = p2Board.numbers.filterNotNull().filter { !room.calledNumbers.contains(it) }
        
        if (availableNumbers.isNotEmpty()) {
            val chosen = availableNumbers.random()
            val nextRoom = _room.value ?: return
            _room.value = gameEngine.processCall(nextRoom, boards, chosen)
        }
    }
}
