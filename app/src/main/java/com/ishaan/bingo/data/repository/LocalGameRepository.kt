package com.ishaan.bingo.data.repository

import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.model.Player
import com.ishaan.bingo.domain.model.BotDifficulty
import com.ishaan.bingo.domain.repository.GameRepository
import com.ishaan.bingo.game.BotMoveStrategy
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
    private val difficulty: BotDifficulty = BotDifficulty.EASY,
    private val gameEngine: BingoGameEngine = BingoGameEngine(),
    private val botMoveStrategy: BotMoveStrategy = BotMoveStrategy(),
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

    override suspend fun prepareSession(): Result<Unit> = Result.success(Unit)

    override fun createRoomDraft(): GameRoom = GameRoom(
        id = "mock-room",
        code = "DEBUG",
        status = GameStatus.WAITING_FOR_PLAYER
    )

    override suspend fun createRoom(room: GameRoom): Result<GameRoom> {
        val roomWithCreator = room.copy(
            id = "mock-room",
            code = "DEBUG",
            players = mapOf(playerId to Player(id = playerId, name = "You"))
        )
        _room.value = roomWithCreator

        // Auto-join a second player after a short delay to simulate "waiting"
        kotlinx.coroutines.delay(1000)
        joinRoom("DEBUG")

        // Fix: return the current (updated) room state, not the stale initial snapshot
        return Result.success(_room.value ?: roomWithCreator)
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

        // Auto-submit board for P2. In TEE-HEE mode, start with an empty board.
        val p2Board = if (difficulty == BotDifficulty.TEE_HEE) {
            BingoBoard(List(25) { null })
        } else {
            BingoBoard((1..25).toList().shuffled())
        }
        _boards.update { it + (player2Id to p2Board) }

        return Result.success(updatedRoom)
    }

    override suspend fun submitBoard(roomId: String, board: BingoBoard): Result<Unit> {
        _boards.update { boards ->
            val p2Current = boards[player2Id] ?: BingoBoard((1..25).toList().shuffled())
            boards + (playerId to board) + (player2Id to p2Current)
        }

        var firstTurnId = ""
        _room.update { room ->
            val r = room ?: return@update null
            val updatedPlayers = r.players.toMutableMap()
            updatedPlayers[playerId] = updatedPlayers[playerId]?.copy(isReady = true) ?: return@update r
            updatedPlayers[player2Id] = updatedPlayers[player2Id]?.copy(isReady = true) ?: return@update r

            firstTurnId = listOf(playerId, player2Id).random()

            r.copy(
                players = updatedPlayers,
                status = GameStatus.PLAYING,
                currentTurnPlayerId = firstTurnId
            )
        }

        if (firstTurnId == player2Id) {
            val room = _room.value
            if (room != null) {
                simulateOpponentMove(room, _boards.value)
            }
        }
        
        return Result.success(Unit)
    }

    override suspend fun callNumber(roomId: String, number: Int): Result<Unit> {
        val room = _room.value ?: return Result.failure(Exception("Room not found"))
        
        // Fix for TEE-HEE: Bot must place the user's called number first
        if (difficulty == BotDifficulty.TEE_HEE) {
            _boards.update { boards ->
                val botBoard = boards[player2Id] ?: BingoBoard(List(25) { null })
                val updatedBotBoard = botMoveStrategy.placeNumber(
                    botBoard = botBoard,
                    number = number,
                    calledNumbers = room.calledNumbers.toSet(),
                    difficulty = difficulty
                )
                boards + (player2Id to updatedBotBoard)
            }
        }

        val boards = _boards.value
        // Process your call using the engine locally
        val updatedRoom = gameEngine.processCall(room, boards, number)
        _room.value = updatedRoom

        // Simulate opponent move
        if (updatedRoom.status == GameStatus.PLAYING && updatedRoom.currentTurnPlayerId == player2Id) {
            simulateOpponentMove(updatedRoom, boards)
        }

        return Result.success(Unit)
    }

    override suspend fun syncMyProgress(
        roomId: String,
        progress: Int,
        completedLines: List<String>,
        claimWin: Boolean
    ): Result<Unit> {
        // In local mode, progression is already calculated by the engine in callNumber
        return Result.success(Unit)
    }

    override suspend fun playAgain(roomId: String): Result<Unit> {
        _room.update { room ->
            val r = room ?: return Result.failure(Exception("Room not found"))
            if (r.status == GameStatus.BOARD_SETUP) return Result.success(Unit)
            if (r.status != GameStatus.FINISHED) return Result.failure(Exception("Game is not finished yet"))

            val resetPlayers = r.players.mapValues { (_, player) ->
                player.copy(
                    isReady = false,
                    bingoProgress = 0,
                    completedLines = emptyList()
                )
            }

            r.copy(
                status = GameStatus.BOARD_SETUP,
                players = resetPlayers,
                currentTurnPlayerId = "",
                calledNumbers = emptyList(),
                callerMap = emptyMap(),
                winnerPlayerId = null
            )
        }
        _boards.value = emptyMap()
        return Result.success(Unit)
    }

    private suspend fun simulateOpponentMove(room: GameRoom, boards: Map<String, BingoBoard>) {
        kotlinx.coroutines.delay(800) // Thinking time
        
        // Fix #5: Re-read fresh state after delay to avoid using stale room/board data
        val freshRoom = _room.value ?: return
        val freshBoards = _boards.value
        val p2Board = freshBoards[player2Id] ?: return
        val userBoard = freshBoards[playerId] ?: return
        
        val chosen = botMoveStrategy.chooseNumber(
            botBoard = p2Board,
            userBoard = userBoard,
            calledNumbers = freshRoom.calledNumbers.toSet(),
            difficulty = difficulty
        )

        if (chosen != null) {
            // Fix for TEE-HEE: Bot must place its own chosen number before calling
            if (difficulty == BotDifficulty.TEE_HEE) {
                _boards.update { b ->
                    val currentBotBoard = b[player2Id] ?: BingoBoard(List(25) { null })
                    val updatedBotBoard = botMoveStrategy.placeNumber(
                        botBoard = currentBotBoard,
                        number = chosen,
                        calledNumbers = freshRoom.calledNumbers.toSet(),
                        difficulty = difficulty
                    )
                    b + (player2Id to updatedBotBoard)
                }
            }
            
            // Re-read boards after dynamic placement
            val finalBoards = _boards.value
            _room.value = gameEngine.processCall(freshRoom, finalBoards, chosen)
        }
    }
}
