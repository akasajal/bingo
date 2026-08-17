package com.ishaan.bingo.game

class BingoLineDetector {
    companion object {
        val ALL_LINES = listOf(
            // Rows
            listOf(0, 1, 2, 3, 4),
            listOf(5, 6, 7, 8, 9),
            listOf(10, 11, 12, 13, 14),
            listOf(15, 16, 17, 18, 19),
            listOf(20, 21, 22, 23, 24),
            // Columns
            listOf(0, 5, 10, 15, 20),
            listOf(1, 6, 11, 16, 21),
            listOf(2, 7, 12, 17, 22),
            listOf(3, 8, 13, 18, 23),
            listOf(4, 9, 14, 19, 24),
            // Diagonals
            listOf(0, 6, 12, 18, 24),
            listOf(4, 8, 12, 16, 20)
        ).mapIndexed { index, line -> "LINE_$index" to line }.toMap()

        fun getLineId(line: List<Int>): String {
            return ALL_LINES.entries.find { it.value == line }?.key ?: "UNKNOWN"
        }
    }

    /**
     * Returns the IDs of all completed lines.
     * @param board The player's board (positions of numbers 1-25)
     * @param calledNumbers The numbers that have been called in the game
     */
    fun detectCompletedLines(boardNumbers: List<Int?>, calledNumbers: Set<Int>): Set<String> {
        val completedLineIds = mutableSetOf<String>()

        ALL_LINES.forEach { (id, indices) ->
            val isLineComplete = indices.all { index ->
                val numberAtCell = boardNumbers[index]
                numberAtCell != null && calledNumbers.contains(numberAtCell)
            }
            if (isLineComplete) {
                completedLineIds.add(id)
            }
        }

        return completedLineIds
    }
}
