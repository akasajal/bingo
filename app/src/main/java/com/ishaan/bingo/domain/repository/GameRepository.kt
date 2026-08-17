package com.ishaan.bingo.domain.repository

import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    val playerId: String
    fun getGameRoom(roomId: String): Flow<GameRoom?>
    fun getPlayerBoard(roomId: String): Flow<BingoBoard?>
    fun getOpponentBoard(roomId: String): Flow<BingoBoard?>
    suspend fun createRoom(): Result<GameRoom>
    suspend fun joinRoom(code: String): Result<GameRoom>
    suspend fun submitBoard(roomId: String, board: BingoBoard): Result<Unit>
    suspend fun callNumber(roomId: String, number: Int): Result<Unit>
}
