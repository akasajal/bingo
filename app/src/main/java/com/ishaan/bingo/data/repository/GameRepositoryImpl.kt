package com.ishaan.bingo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.ishaan.bingo.data.remote.FirebaseGameDataSource
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.model.Player
import com.ishaan.bingo.domain.repository.GameRepository
import com.ishaan.bingo.game.BingoGameEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GameRepositoryImpl(
    private val dataSource: FirebaseGameDataSource,
    private val gameEngine: BingoGameEngine = BingoGameEngine(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : GameRepository {

    override val playerId: String
        get() = auth.currentUser?.uid ?: ""

    private suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    override fun getGameRoom(roomId: String): Flow<GameRoom?> = dataSource.getGameRoom(roomId)

    override fun getPlayerBoard(roomId: String): Flow<BingoBoard?> = dataSource.getBoard(roomId, playerId)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getOpponentBoard(roomId: String): Flow<BingoBoard?> {
        return dataSource.getGameRoom(roomId).flatMapLatest { room ->
            val opponentId = room?.players?.keys?.firstOrNull { it != playerId }
            if (opponentId != null) {
                dataSource.getBoard(roomId, opponentId)
            } else {
                flowOf(null)
            }
        }
    }

    override suspend fun createRoom(): Result<GameRoom> = runCatching {
        ensureAuthenticated()
        val room = dataSource.createRoom()
        val creator = Player(id = playerId, name = "Player 1")
        val updatedRoom = room.copy(players = mapOf(playerId to creator))
        dataSource.updateRoom(updatedRoom)
        updatedRoom
    }

    override suspend fun joinRoom(code: String): Result<GameRoom> = runCatching {
        ensureAuthenticated()
        val player = Player(id = playerId, name = "Player 2")
        dataSource.joinRoom(code, player)
    }

    /**
     * Fix #2: Board upload is still a plain write (boards are per-player, no race there).
     * The ready flag is now set inside a Firestore transaction via markPlayerReady,
     * so concurrent submitBoard calls from both players can't overwrite each other.
     */
    override suspend fun submitBoard(roomId: String, board: BingoBoard): Result<Unit> = runCatching {
        ensureAuthenticated()
        dataSource.updateBoard(roomId, playerId, board)
        dataSource.markPlayerReady(roomId, playerId)
    }

    /**
     * Fix #1: Turn ownership and duplicate-call checks are now enforced inside a
     * Firestore transaction in the data source, so a player cannot write a move
     * on the opponent's turn even by calling the function directly.
     */
    override suspend fun callNumber(roomId: String, number: Int): Result<Unit> = runCatching {
        ensureAuthenticated()
        dataSource.callNumberTransactional(
            roomId = roomId,
            callingPlayerId = playerId,
            number = number,
            gameEngine = gameEngine,
            getBoards = { playerIds ->
                playerIds.associateWith { id ->
                    dataSource.getBoard(roomId, id).first() ?: BingoBoard()
                }
            }
        )
    }
}