package com.ishaan.bingo.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.model.Player
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

    suspend fun createRoom(): GameRoom {
        val roomId = UUID.randomUUID().toString()
        val code = (1..5).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
        val room = GameRoom(
            id = roomId,
            code = code,
            status = GameStatus.WAITING_FOR_PLAYER
        )
        roomsCollection.document(roomId).set(room).await()
        return room
    }

    suspend fun joinRoom(code: String, player: Player): GameRoom {
        val query = roomsCollection.whereEqualTo("code", code).limit(1).get().await()
        if (query.isEmpty) throw Exception("Room not found")
        
        val document = query.documents[0]
        val room = document.toObject(GameRoom::class.java) ?: throw Exception("Invalid room data")
        
        if (room.players.size >= 2) throw Exception("Room is full")
        
        val updatedPlayers = room.players + (player.id to player)
        val updatedStatus = if (updatedPlayers.size == 2) GameStatus.BOARD_SETUP else room.status
        
        val updatedRoom = room.copy(
            players = updatedPlayers,
            status = updatedStatus
        )
        
        document.reference.set(updatedRoom).await()
        return updatedRoom
    }

    suspend fun updateRoom(room: GameRoom) {
        roomsCollection.document(room.id).set(room).await()
    }
    
    suspend fun updateBoard(roomId: String, playerId: String, board: BingoBoard) {
        // We store boards separately for privacy
        firestore.collection("rooms").document(roomId)
            .collection("boards").document(playerId)
            .set(board).await()
    }

    fun getBoard(roomId: String, playerId: String): Flow<BingoBoard?> {
        return firestore.collection("rooms").document(roomId)
            .collection("boards").document(playerId)
            .snapshots().map { it.toObject(BingoBoard::class.java) }
    }
}
