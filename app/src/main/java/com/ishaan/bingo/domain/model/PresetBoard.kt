package com.ishaan.bingo.domain.model

import java.util.UUID

data class PresetBoard(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "My Board",
    val board: BingoBoard
)
