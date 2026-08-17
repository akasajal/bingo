package com.ishaan.bingo.data.repository

import com.ishaan.bingo.data.remote.FirebaseGameDataSource
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.model.Player
import com.ishaan.bingo.domain.repository.GameRepository
import com.ishaan.bingo.game.BingoGameEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class GameRepositoryImpl(
    private val dataSource: FirebaseGameDataSource,
    private val gameEngine: BingoGameEngine = BingoGameEngine(),
    override val playerId: String = UUID.randomUUID().toString() // Simple persistent-ish ID for session
) : GameRepository {

    override fun getGameRoom(roomId: String): Flow<GameRoom?> = dataSource.getGameRoom(roomId)

    override fun getPlayerBoard(roomId: String): Flow<BingoBoard?> = dataSource.getBoard(roomId, playerId)

    override suspend fun createRoom(): Result<GameRoom> = runCatching {
        dataSource.createRoom()
    }

    override suspend fun joinRoom(code: String): Result<GameRoom> = runCatching {
        val player = Player(id = playerId, name = "Player 2") // Simplified
        dataSource.joinRoom(code, player)
    }

    override suspend fun submitBoard(roomId: String, board: BingoBoard): Result<Unit> = runCatching {
        dataSource.updateBoard(roomId, playerId, board)
        val room = dataSource.getGameRoom(roomId).first() ?: throw Exception("Room not found")
        val updatedPlayers = room.players.toMutableMap()
        val currentPlayer = updatedPlayers[playerId] ?: throw Exception("Player not found")
        updatedPlayers[playerId] = currentPlayer.copy(isReady = true)
        
        val allReady = updatedPlayers.values.all { it.isReady }
        val newStatus = if (allReady && updatedPlayers.size == 2) GameStatus.PLAYING else room.status
        
        // Randomly pick first player if game is starting
        val currentTurn = if (newStatus == GameStatus.PLAYING && room.currentTurnPlayerId.isEmpty()) {
            updatedPlayers.keys.random()
        } else {
            room.currentTurnPlayerId
        }

        dataSource.updateRoom(room.copy(
            players = updatedPlayers,
            status = newStatus,
            currentTurnPlayerId = currentTurn
        ))
    }

    override suspend fun callNumber(roomId: String, number: Int): Result<Unit> = runCatching {
        val room = dataSource.getGameRoom(roomId).first() ?: throw Exception("Room not found")
        
        // We need all boards to process the call (authoritative)
        // In a real app, this should happen on a server, but for this "Server-less" approach,
        // the client who makes the call processes it.
        val playerIds = room.players.keys
        val boards = playerIds.associateWith { id ->
            dataSource.getBoard(roomId, id).first() ?: BingoBoard()
        }
        
        val updatedRoom = gameEngine.processCall(room, boards, number)
        dataSource.updateRoom(updatedRoom)
    }
}
