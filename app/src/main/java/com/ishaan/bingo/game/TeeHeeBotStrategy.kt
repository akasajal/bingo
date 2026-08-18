package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard

/**
 * TEE-HEE Strategy:
 * The bot doesn't have a fixed board. It places numbers strategically as they are called.
 */
class TeeHeeBotStrategy : BotStrategy {
    
    // Game-specific weights to make the bot choose different win paths each match
    private val lineWeights = BingoLineDetector.ALL_LINES.keys.associateWith { 
        (0..50).random() // Tiny random bias for each possible BINGO line
    }

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

        // Fix: Find all best indices to break ties randomly
        val scores = emptyIndices.associateWith { index ->
            calculateCellScore(index, number, board, calledNumbers)
        }
        val maxScore = scores.values.maxOrNull() ?: 0
        val bestIndices = scores.filter { it.value == maxScore }.keys.toList()
        
        val bestIndex = bestIndices.random() // Random tie-breaking

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

        // Fix: Find all best numbers to break ties randomly
        val scores = availableNumbers.associateWith { number ->
            val myGain = calculatePlacementGain(number, botBoard, calledNumbers)
            val userGain = calculateTacticalScore(number, userBoard, calledNumbers)
            myGain - userGain
        }
        
        val maxScore = scores.values.maxOrNull() ?: 0
        val bestNumbers = scores.filter { it.value == maxScore }.keys.toList()

        return bestNumbers.random() // Random tie-breaking
    }

    private fun calculatePlacementGain(number: Int, board: BingoBoard, calledNumbers: Set<Int>): Int {
        val emptyIndices = board.numbers.mapIndexedNotNull { index, n -> if (n == null) index else null }
        if (emptyIndices.isEmpty()) return 0
        
        return emptyIndices.maxOf { index ->
            calculateCellScore(index, number, board, calledNumbers)
        }
    }

    private fun calculateCellScore(index: Int, number: Int, board: BingoBoard, calledNumbers: Set<Int>): Int {
        var score = 0
        val currentAndIncomingCalls = calledNumbers + number
        
        BingoLineDetector.ALL_LINES.forEach { (lineId, line) ->
            if (index in line) {
                val calledInLine = line.count { cellIdx ->
                    val nAtCell = if (cellIdx == index) number else board.numbers[cellIdx]
                    nAtCell != null && nAtCell in currentAndIncomingCalls
                }
                
                score += when (calledInLine) {
                    5 -> 100000 
                    4 -> 10000
                    3 -> 1000
                    2 -> 100
                    else -> 10
                }
                
                // Add the game-specific weight for this line to vary bot preference
                score += lineWeights[lineId] ?: 0
            }
        }
        return score
    }
}
