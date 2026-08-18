package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard

/** Interface for different bot difficulty levels. */
interface BotStrategy {
    fun chooseNumber(
        botBoard: BingoBoard,
        userBoard: BingoBoard,
        calledNumbers: Set<Int>
    ): Int?

    /** Common tactical scoring logic used by both easy and hard bots. */
    fun calculateTacticalScore(number: Int, board: BingoBoard, calledNumbers: Set<Int>): Int {
        val index = board.numbers.indexOf(number)
        if (index == -1) return 0
        
        val lineProgress = BingoLineDetector.ALL_LINES.values.map { line ->
            line.count { board.numbers[it] in calledNumbers }
        }

        return BingoLineDetector.ALL_LINES.values.zip(lineProgress).sumOf { (line, calledInLine) ->
            if (index !in line) 0 else when (calledInLine) {
                4 -> 10_000 // Finish a line immediately
                3 -> 1_000
                2 -> 100
                else -> 10
            }
        }
    }

    /** Center cells are tactically more valuable. */
    fun calculateCentrality(number: Int, board: BingoBoard): Int {
        val index = board.numbers.indexOf(number)
        if (index == -1) return 0
        return BingoLineDetector.ALL_LINES.values.count { index in it }
    }
}
