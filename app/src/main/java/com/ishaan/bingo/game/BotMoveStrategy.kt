package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard

/** Chooses moves that build and finish the bot's BINGO lines. */
class BotMoveStrategy {
    fun chooseNumber(board: BingoBoard, calledNumbers: Set<Int>): Int? {
        val availableCells = board.numbers.mapIndexedNotNull { index, number ->
            number?.takeUnless(calledNumbers::contains)?.let { index to it }
        }
        if (availableCells.isEmpty()) return null

        val lineProgress = BingoLineDetector.ALL_LINES.values.map { line ->
            line to line.count { index -> board.numbers[index] in calledNumbers }
        }
        val strongestLaneProgress = lineProgress.maxOf { it.second }

        // In the opening, control the centre: it belongs to its row, column, and both diagonals.
        if (strongestLaneProgress < 2) {
            availableCells.firstOrNull { (index, _) -> index == CENTER_CELL }?.let { return it.second }
        }

        return availableCells.maxWithOrNull(
            compareBy<Pair<Int, Int>> { (index, _) -> tacticalScore(index, lineProgress) }
                .thenBy { (index, _) -> centrality(index) }
                // Keep equal choices predictable for a testable, non-random bot.
                .thenBy { (_, number) -> -number }
        )?.second
    }

    private fun tacticalScore(index: Int, lineProgress: List<Pair<List<Int>, Int>>): Int {
        return lineProgress.sumOf { (line, calledInLine) ->
            if (index !in line || calledInLine < 2) 0 else when (calledInLine) {
                4 -> 10_000 // Finish a line immediately.
                3 -> 1_000
                else -> 100 // Continue a lane with two called cells.
            }
        }
    }

    private fun centrality(index: Int): Int =
        BingoLineDetector.ALL_LINES.values.count { index in it }

    private companion object {
        const val CENTER_CELL = 12
    }
}
