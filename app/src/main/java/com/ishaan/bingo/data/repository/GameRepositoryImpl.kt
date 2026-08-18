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
     * Fix: Board upload and ready-flag are independent.
     * We run them concurrently to cut setup latency by ~50%.
     */
    override suspend fun submitBoard(roomId: String, board: BingoBoard): Result<Unit> = runCatching {
        ensureAuthenticated()
        coroutineScope {
            val boardUpdate = async { dataSource.updateBoard(roomId, playerId, board) }
            val readyUpdate = async { dataSource.markPlayerReady(roomId, playerId) }
            boardUpdate.await()
            readyUpdate.await()
        }
    }

    /**
     * Optimized Move Flow:
     * We only update calledNumbers and flip turn. 
     * Local progress calculation happens in the ViewModel.
     */
    override suspend fun callNumber(roomId: String, number: Int): Result<Unit> = runCatching {
        ensureAuthenticated()
        dataSource.callNumberSimple(
            roomId = roomId,
            callingPlayerId = playerId,
            number = number
        )
    }

    override suspend fun syncMyProgress(
        roomId: String, 
        progress: Int, 
        completedLines: List<String>, 
        claimWin: Boolean
    ): Result<Unit> = runCatching {
        ensureAuthenticated()
        dataSource.updatePlayerProgress(roomId, playerId, progress, completedLines, claimWin)
    }

    override suspend fun playAgain(roomId: String): Result<Unit> = runCatching {
        ensureAuthenticated()
        dataSource.playAgain(roomId, playerId)
    }
}
