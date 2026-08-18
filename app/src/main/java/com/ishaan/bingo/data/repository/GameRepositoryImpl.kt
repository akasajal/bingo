package com.ishaan.bingo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.ishaan.bingo.data.remote.FirebaseGameDataSource
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.model.Player
import com.ishaan.bingo.domain.repository.GameRepository
import com.ishaan.bingo.game.BingoGameEngine
import kotlinx.coroutines.*
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

    override suspend fun prepareSession(): Result<Unit> = runCatching {
        ensureAuthenticated()
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

    override fun createRoomDraft(): GameRoom = dataSource.createRoomDraft()

    override suspend fun createRoom(room: GameRoom): Result<GameRoom> = runCatching {
        ensureAuthenticated()
        val creator = Player(id = playerId, name = "Player 1")
        val roomWithCreator = room.copy(players = mapOf(creator.id to creator))
        dataSource.createRoom(roomWithCreator)
        roomWithCreator
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
    override suspend fun callNumber(roomId: String, playerIds: Set<String>, number: Int): Result<Unit> = runCatching {
        ensureAuthenticated()
        
        // Parallelize board fetching to minimize latency
        val boards = kotlinx.coroutines.coroutineScope {
            playerIds.map { id ->
                async {
                    id to (dataSource.getBoardOnce(roomId, id) ?: BingoBoard())
                }
            }.awaitAll().toMap()
        }

        dataSource.callNumberTransactional(
            roomId = roomId,
            callingPlayerId = playerId,
            number = number,
            gameEngine = gameEngine,
            boards = boards
        )
    }

    override suspend fun playAgain(roomId: String): Result<Unit> = runCatching {
        ensureAuthenticated()
        dataSource.playAgain(roomId, playerId)
    }
}
