package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard

/**
 * TEE-HEE Strategy (Manifest Destiny):
 * 1. Shadow Phase (1-15 calls): Bot sabotages user, avoids calling numbers user needs.
 * 2. Manifest Phase (Call 16): Bot instantly builds a board with 5 completed lines.
 */
class TeeHeeBotStrategy : BotStrategy {

    /**
     * Absolute Sabotage: Never call a number that would help the user complete a line of 3+.
     */
    override fun chooseNumber(
        botBoard: BingoBoard,
        userBoard: BingoBoard,
        calledNumbers: Set<Int>
    ): Int? {
        val availableNumbers = (1..25).filter { it !in calledNumbers }
        if (availableNumbers.isEmpty()) return null

        // Filter out "Dangerous" numbers for the bot (anything that helps user finish a 3+ line)
        val safeNumbers = availableNumbers.filter { number ->
            val userIdx = userBoard.numbers.indexOf(number)
            if (userIdx == -1) return@filter true
            
            // Avoid if any user line containing this number has 2 or more already called
            // (Calling it would make it 3+, which is the user's limit for 'safe' calls)
            !BingoLineDetector.ALL_LINES.values.any { line ->
                userIdx in line && line.count { userBoard.numbers[it] in calledNumbers } >= 2
            }
        }

        val candidates = if (safeNumbers.isNotEmpty()) safeNumbers else availableNumbers
        
        // Pick the least helpful number for the user among candidates
        return candidates.minByOrNull { calculateTacticalScore(it, userBoard, calledNumbers) }
            ?: candidates.random()
    }

    /**
     * Maps the 16 called numbers to a layout that completes exactly 5 lines.
     */
    fun manifestBoard(calledNumbers: List<Int>, uncalledNumbers: List<Int>): BingoBoard {
        val numbers = MutableList<Int?>(25) { null }
        val calledPool = calledNumbers.toMutableList()
        val uncalledPool = uncalledNumbers.toMutableList()

        // Choose one of given 16-cell / 5-line patterns
        val layout = when (java.util.Random().nextInt(4)) {
            0 -> listOf(
                0, 4, 6, 8, 12, 16, 18, 20, 24,
                1, 2, 3, 21, 22, 23, 11
            )

            1 -> listOf(
                0, 4, 6, 8, 12, 16, 18, 20, 24,
                1, 2, 3, 21, 22, 23, 13
            )

            2 -> listOf(
                0, 4, 6, 8, 12, 16, 18, 20, 24,
                5, 10, 15, 9, 14, 19, 7
            )

            else -> listOf(
                0, 4, 6, 8, 12, 16, 18, 20, 24,
                5, 10, 15, 9, 14, 19, 17
            )
        }

        // Place called numbers into the strategic layout spots
        layout.forEachIndexed { i, boardIndex ->
            if (i < calledPool.size) {
                numbers[boardIndex] = calledPool[i]
            }
        }

        // Fill remaining 9 spots with uncalled numbers
        for (i in 0 until 25) {
            if (numbers[i] == null && uncalledPool.isNotEmpty()) {
                numbers[i] = uncalledPool.removeAt(0)
            }
        }

        return BingoBoard(numbers)
    }
}
