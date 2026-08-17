package com.ishaan.bingo.domain.model

enum class GameStatus {
    LOBBY,
    WAITING_FOR_PLAYER,
    BOARD_SETUP,
    WAITING_FOR_READY,
    PLAYING,
    FINISHED
}
