package com.ishaan.bingo.domain.model

data class Player(
    val id: String = "",
    val name: String = "",
    val isReady: Boolean = false,
    val bingoProgress: Int = 0, // Number of BINGO letters earned (0-5)
    val completedLines: Set<String> = emptySet()
)
