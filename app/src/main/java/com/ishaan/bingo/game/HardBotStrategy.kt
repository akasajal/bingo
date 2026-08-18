package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard

/** Advanced logic: Bot evaluates its own gain and tries to minimize the user's gain. */
class HardBotStrategy : BotStrategy {
    override fun chooseNumber(
        botBoard: BingoBoard,
        userBoard: BingoBoard,
        calledNumbers: Set<Int>
    ): Int? {
        val availableNumbers = botBoard.numbers.filterNotNull().filter { it !in calledNumbers }
        if (availableNumbers.isEmpty()) return null

        return availableNumbers.maxWithOrNull(
            compareBy<Int> { number ->
                val myScore = calculateTacticalScore(number, botBoard, calledNumbers)
                val userGain = calculateTacticalScore(number, userBoard, calledNumbers)
                
                // Maximize my gain while minimizing user's gain
                myScore - userGain
            }
            .thenBy { calculateCentrality(it, botBoard) }
            .thenBy { -it }
        )
    }
}
