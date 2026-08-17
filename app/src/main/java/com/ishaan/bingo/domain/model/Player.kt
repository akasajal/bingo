package com.ishaan.bingo.domain.model

data class Player(
    val id: String = "",
    val name: String = "",
    @get:JvmName("getIsReady")
    val isReady: Boolean = false,
    val bingoProgress: Int = 0, // Number of BINGO letters earned (0-5)
    val completedLines: List<String> = emptyList()
)