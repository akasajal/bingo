package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.model.Player

class BingoGameEngine(
    private val lineDetector: BingoLineDetector = BingoLineDetector()
) {
    /**
     * Processes a number call and returns the updated GameRoom.
     * This assumes validation (turn, called numbers) has already been performed by the caller/server.
     */
    fun processCall(
        gameRoom: GameRoom,
        playerBoards: Map<String, BingoBoard>,
        calledNumber: Int
    ): GameRoom {
        if (gameRoom.status != GameStatus.PLAYING) return gameRoom
        if (gameRoom.calledNumbers.contains(calledNumber)) return gameRoom

        val newCalledNumbers = gameRoom.calledNumbers + calledNumber
        val newCallerMap = gameRoom.callerMap + (calledNumber.toString() to gameRoom.currentTurnPlayerId)

        val updatedPlayers = gameRoom.players.mapValues { (playerId, player) ->
            val board = playerBoards[playerId] ?: return@mapValues player
            val currentCompletedLines = player.completedLines.toSet()
            val newlyCompletedLines = lineDetector.detectCompletedLines(board.numbers, newCalledNumbers.toSet())

            val freshLines = newlyCompletedLines - currentCompletedLines

            // Rule: Award ONE letter for EACH newly completed line
            val newProgress = (player.bingoProgress + freshLines.size).coerceAtMost(5)

            player.copy(
                bingoProgress = newProgress,
                completedLines = newlyCompletedLines.toList()
            )
        }

        val winnerId = updatedPlayers.entries.find { it.value.bingoProgress >= 5 }?.key
        val newStatus = if (winnerId != null) GameStatus.FINISHED else GameStatus.PLAYING

        // Switch turn
        val playerIds = updatedPlayers.keys.toList()
        val nextTurnPlayerId = if (winnerId != null) "" else {
            val currentIndex = playerIds.indexOf(gameRoom.currentTurnPlayerId)
            playerIds[(currentIndex + 1) % playerIds.size]
        }

        return gameRoom.copy(
            players = updatedPlayers,
            calledNumbers = newCalledNumbers,
            callerMap = newCallerMap,
            status = newStatus,
            winnerPlayerId = winnerId,
            currentTurnPlayerId = nextTurnPlayerId
        )
    }
}