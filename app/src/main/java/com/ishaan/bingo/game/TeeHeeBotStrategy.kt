package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard

/**
 * TEE-HEE Strategy:
 * The bot doesn't have a fixed board. It places numbers strategically as they are called.
 */
class TeeHeeBotStrategy : BotStrategy {
    
    /**
     * Finds the best cell to place a newly called number on an incomplete bot board.
     */
    fun placeNumber(
        board: BingoBoard,
        number: Int,
        calledNumbers: Set<Int>
    ): BingoBoard {
        val emptyIndices = board.numbers.mapIndexedNotNull { index, n -> if (n == null) index else null }
        if (emptyIndices.isEmpty()) return board

        // Score each empty cell for the specific number being placed
        val bestIndex = emptyIndices.maxByOrNull { index ->
            calculateCellScore(index, number, board, calledNumbers)
        } ?: emptyIndices.first()

        val newNumbers = board.numbers.toMutableList()
        newNumbers[bestIndex] = number
        return BingoBoard(newNumbers)
    }

    override fun chooseNumber(
        botBoard: BingoBoard,
        userBoard: BingoBoard,
        calledNumbers: Set<Int>
    ): Int? {
        val availableNumbers = (1..25).filter { it !in calledNumbers }
        if (availableNumbers.isEmpty()) return null

        // TEE-HEE "Cheater" Peek: Maximize bot gain - user gain
        return availableNumbers.maxByOrNull { number ->
            // How much would THIS number help me if I placed it now?
            val myGain = calculatePlacementGain(number, botBoard, calledNumbers)
            // How much does it help the user?
            val userGain = calculateTacticalScore(number, userBoard, calledNumbers)
            
            myGain - userGain
        }
    }

    private fun calculatePlacementGain(number: Int, board: BingoBoard, calledNumbers: Set<Int>): Int {
        val emptyIndices = board.numbers.mapIndexedNotNull { index, n -> if (n == null) index else null }
        if (emptyIndices.isEmpty()) return 0
        
        // Find the best possible score if we placed 'number' in any empty cell
        return emptyIndices.maxOf { index ->
            calculateCellScore(index, number, board, calledNumbers)
        }
    }

    private fun calculateCellScore(index: Int, number: Int, board: BingoBoard, calledNumbers: Set<Int>): Int {
        var score = 0
        // Temporarily treat the number as called if it's the one we are placing
        val currentAndIncomingCalls = calledNumbers + number
        
        BingoLineDetector.ALL_LINES.values.forEach { line ->
            if (index in line) {
                val calledInLine = line.count { cellIdx ->
                    val nAtCell = if (cellIdx == index) number else board.numbers[cellIdx]
                    nAtCell != null && nAtCell in currentAndIncomingCalls
                }
                
                score += when (calledInLine) {
                    5 -> 100000 // Completes a line immediately!
                    4 -> 10000
                    3 -> 1000
                    2 -> 100
                    else -> 10
                }
            }
        }
        return score
    }
}
