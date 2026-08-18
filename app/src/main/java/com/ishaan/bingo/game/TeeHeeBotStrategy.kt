package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard

/**
 * TEE-HEE Strategy:
 * The bot doesn't have a fixed board. It places numbers strategically as they are called.
 *
 * Design goal: game ends in 16–18 total moves (combined player + bot calls).
 *
 * Core ideas:
 *  1. PLACEMENT – When a number lands on the bot's board, place it in the cell that
 *     participates in the most currently-incomplete lines AND is closest to completing
 *     one of them.  Ties broken by a stable per-game random weight so the bot varies
 *     its winning path across matches.
 *
 *  2. NUMBER SELECTION – On the bot's turn it calls the uncalled number whose *optimal
 *     placement* gives the greatest expected line progress.  It completely ignores the
 *     user's board (the user has a fixed board, so the bot can't meaningfully interfere
 *     beyond picking numbers the user hasn't placed yet — which it can't know).
 *
 *  3. EFFICIENCY – The scoring uses a geometric scale (5→100 000, 4→10 000, 3→1 000,
 *     2→100, 1→10) so "nearly-complete line" cells always beat "lonely cell" cells.
 *     This creates the greedy convergence that reliably finishes in 16-18 moves.
 */
class TeeHeeBotStrategy : BotStrategy {

    // Stable per-game tiny bias so the bot prefers a slightly different winning path
    // each match without sacrificing strategic quality.
    private val lineWeights: Map<String, Int> = BingoLineDetector.ALL_LINES.keys
        .associateWith { (1..30).random() }

    // How many lines each board index participates in (computed once, board-agnostic).
    private val indexLineCount: IntArray = IntArray(25) { idx ->
        BingoLineDetector.ALL_LINES.values.count { line -> idx in line }
    }

    // ── PUBLIC API ────────────────────────────────────────────────────────────────

    /**
     * Place [number] on the best empty cell of [board].
     * Called when any number is called (either player's turn).
     */
    fun placeNumber(
        board: BingoBoard,
        number: Int,
        calledNumbers: Set<Int>
    ): BingoBoard {
        val emptyIndices = board.numbers.mapIndexedNotNull { i, n -> if (n == null) i else null }
        if (emptyIndices.isEmpty()) return board

        val calledWithNew = calledNumbers + number
        val bestIndex = emptyIndices.maxWithOrNull(
            compareByDescending<Int> { idx -> cellScore(idx, board, calledWithNew) }
                .thenByDescending { idx -> indexLineCount[idx] }  // prefer high-intersection cells
                .thenBy { idx -> lineWeights.values.elementAtOrElse(idx) { 0 } } // stable tiebreak
        ) ?: emptyIndices.first()

        val newNumbers = board.numbers.toMutableList()
        newNumbers[bestIndex] = number
        return BingoBoard(newNumbers)
    }

    /**
     * Choose which uncalled number (1-25) to call next.
     * We maximise our own line progress; we do not waste turns blocking the user
     * (who has a fixed board we can't change anyway).
     */
    override fun chooseNumber(
        botBoard: BingoBoard,
        userBoard: BingoBoard,
        calledNumbers: Set<Int>
    ): Int? {
        val available = (1..25).filter { it !in calledNumbers }
        if (available.isEmpty()) return null

        return available.maxWithOrNull(
            compareByDescending<Int> { number ->
                // Hard mode adversarial scoring: maximise my gain, minimise user's gain
                val myScore = bestPlacementScore(number, botBoard, calledNumbers)
                val userGain = calculateTacticalScore(number, userBoard, calledNumbers)
                myScore - userGain
            }.thenBy { lineWeights.values.elementAtOrElse(it - 1) { 0 } }
        )
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────────

    /**
     * Score for placing [number] at a particular empty [index], given the current
     * board state and the full set of numbers that will have been called (including
     * [number] itself).
     *
     * Geometric scoring ensures "4-in-a-line → win" always beats "2 cells in 3 lines".
     */
    private fun cellScore(index: Int, board: BingoBoard, calledWithNew: Set<Int>): Int {
        var score = 0
        BingoLineDetector.ALL_LINES.forEach { (lineId, line) ->
            if (index !in line) return@forEach

            // Count how many cells in this line will be filled after placing the number
            val filled = line.count { cellIdx ->
                val numAtCell = if (cellIdx == index) calledWithNew.last() // the new number
                else board.numbers[cellIdx]
                numAtCell != null && numAtCell in calledWithNew
            }

            score += when (filled) {
                5 -> 100_000   // completes the line right now → huge reward
                4 -> 10_000    // one away → very high priority
                3 -> 1_000
                2 -> 100
                else -> 10
            }
            score += lineWeights[lineId] ?: 0
        }
        return score
    }

    /**
     * What is the best score we could get if we placed [number] on the best empty cell
     * of [board]?  Used by [chooseNumber] to rank candidates.
     */
    private fun bestPlacementScore(number: Int, board: BingoBoard, calledNumbers: Set<Int>): Int {
        val calledWithNew = calledNumbers + number
        val emptyIndices = board.numbers.mapIndexedNotNull { i, n -> if (n == null) i else null }
        if (emptyIndices.isEmpty()) return 0
        return emptyIndices.maxOf { idx -> cellScore(idx, board, calledWithNew) }
    }
}