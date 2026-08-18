package com.ishaan.bingo.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.Transaction
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.model.Player
import com.ishaan.bingo.game.BingoGameEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseGameDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val roomsCollection = firestore.collection("rooms")

    fun getGameRoom(roomId: String): Flow<GameRoom?> {
        return roomsCollection.document(roomId).snapshots().map { snapshot ->
            snapshot.toObject(GameRoom::class.java)
        }
    }

    fun createRoomDraft(): GameRoom {
        val roomId = UUID.randomUUID().toString()
        val code = (1..5).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
        return GameRoom(
            id = roomId,
            code = code,
            status = GameStatus.WAITING_FOR_PLAYER
        )
    }

    suspend fun createRoom(room: GameRoom) {
        roomsCollection.document(room.id).set(room).await()
    }

    suspend fun joinRoom(code: String, player: Player): GameRoom {
        val query = roomsCollection.whereEqualTo("code", code).limit(1).get().await()
        if (query.isEmpty) throw Exception("Room not found")

        val docRef = query.documents[0].reference
        var resultRoom: GameRoom? = null

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val room = snapshot.toObject(GameRoom::class.java)
                ?: throw Exception("Invalid room data")

            if (room.players.size >= 2) throw Exception("Room is full")
            if (room.players.containsKey(player.id)) {
                // Already in the room (e.g. reconnect) — no-op
                resultRoom = room
                return@runTransaction
            }

            val updatedPlayers = room.players + (player.id to player)
            val updatedStatus =
                if (updatedPlayers.size == 2) GameStatus.BOARD_SETUP else room.status

            val updatedRoom = room.copy(
                players = updatedPlayers,
                status = updatedStatus
            )
            transaction.set(docRef, updatedRoom)
            resultRoom = updatedRoom
        }.await()

        return resultRoom ?: throw Exception("Transaction failed")
    }

    suspend fun markPlayerReady(roomId: String, playerId: String): GameRoom {
        val docRef = roomsCollection.document(roomId)
        var resultRoom: GameRoom? = null

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val room = snapshot.toObject(GameRoom::class.java)
                ?: throw Exception("Room not found")

            val currentPlayer = room.players[playerId]
                ?: throw Exception("Player not found in room")

            val updatedPlayers = room.players.toMutableMap()
            updatedPlayers[playerId] = currentPlayer.copy(isReady = true)

            val allReady = updatedPlayers.values.all { it.isReady }
            val newStatus =
                if (allReady && updatedPlayers.size == 2) GameStatus.PLAYING else room.status

            val currentTurn = if (newStatus == GameStatus.PLAYING && room.currentTurnPlayerId.isEmpty()) {
                updatedPlayers.keys.random()
            } else {
                room.currentTurnPlayerId
            }

            val updatedRoom = room.copy(
                players = updatedPlayers,
                status = newStatus,
                currentTurnPlayerId = currentTurn
            )
            transaction.set(docRef, updatedRoom)
            resultRoom = updatedRoom
        }.await()

        return resultRoom ?: throw Exception("Transaction failed")
    }

    suspend fun callNumberTransactional(
        roomId: String,
        callingPlayerId: String,
        number: Int,
        gameEngine: BingoGameEngine,
        boards: Map<String, BingoBoard>
    ): GameRoom {
        val docRef = roomsCollection.document(roomId)
        var resultRoom: GameRoom? = null

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val room = snapshot.toObject(GameRoom::class.java)
                ?: throw Exception("Room not found")

            // Authoritative validation inside the transaction
            if (room.status != GameStatus.PLAYING) {
                throw Exception("Game is not in progress")
            }
            if (room.currentTurnPlayerId != callingPlayerId) {
                throw Exception("Not your turn")
            }
            if (room.calledNumbers.contains(number)) {
                throw Exception("Number already called")
            }

            val updatedRoom = gameEngine.processCall(room, boards, number)
            transaction.set(docRef, updatedRoom)
            resultRoom = updatedRoom
        }.await()

        return resultRoom ?: throw Exception("Transaction failed")
    }

    suspend fun playAgain(roomId: String, requestingPlayerId: String): GameRoom {
        val roomRef = roomsCollection.document(roomId)
        var resultRoom: GameRoom? = null

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val room = snapshot.toObject(GameRoom::class.java)
                ?: throw Exception("Room not found")

            if (!room.players.containsKey(requestingPlayerId)) {
                throw Exception("Player not found in room")
            }
            if (room.status == GameStatus.BOARD_SETUP) {
                resultRoom = room
                return@runTransaction
            }
            if (room.status != GameStatus.FINISHED) {
                throw Exception("Game is not finished yet")
            }

            val resetPlayers = room.players.mapValues { (_, player) ->
                player.copy(
                    isReady = false,
                    bingoProgress = 0,
                    completedLines = emptyList()
                )
            }

            val resetRoom = room.copy(
                status = GameStatus.BOARD_SETUP,
                players = resetPlayers,
                currentTurnPlayerId = "",
                calledNumbers = emptyList(),
                callerMap = emptyMap(),
                winnerPlayerId = null
            )

            room.players.keys.forEach { playerId ->
                transaction.delete(roomRef.collection("boards").document(playerId))
            }
            transaction.set(roomRef, resetRoom)
            resultRoom = resetRoom
        }.await()

        return resultRoom ?: throw Exception("Transaction failed")
    }

    suspend fun updateRoom(room: GameRoom) {
        roomsCollection.document(room.id).set(room).await()
    }

    suspend fun updateBoard(roomId: String, playerId: String, board: BingoBoard) {
        // Boards are stored in a subcollection for privacy
        firestore.collection("rooms").document(roomId)
            .collection("boards").document(playerId)
            .set(board).await()
    }

    suspend fun getBoardOnce(roomId: String, playerId: String): BingoBoard? {
        return firestore.collection("rooms").document(roomId)
            .collection("boards").document(playerId)
            .get().await()
            .toObject(BingoBoard::class.java)
    }

    fun getBoard(roomId: String, playerId: String): Flow<BingoBoard?> {
        return firestore.collection("rooms").document(roomId)
            .collection("boards").document(playerId)
            .snapshots().map { it.toObject(BingoBoard::class.java) }
    }
}