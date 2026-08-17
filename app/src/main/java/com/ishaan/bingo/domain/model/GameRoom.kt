package com.ishaan.bingo.domain.model

data class GameRoom(
    val id: String = "",
    val code: String = "",
    val status: GameStatus = GameStatus.WAITING_FOR_PLAYER,
    val players: Map<String, Player> = emptyMap(),
    val currentTurnPlayerId: String = "",
    val calledNumbers: List<Int> = emptyList(),
    val winnerPlayerId: String? = null
)
