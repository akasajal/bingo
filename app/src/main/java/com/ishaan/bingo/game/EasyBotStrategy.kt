package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard

/** Current standard logic: Bot only cares about its own board. */
class EasyBotStrategy : BotStrategy {
    override fun chooseNumber(
        botBoard: BingoBoard,
        userBoard: BingoBoard,
        calledNumbers: Set<Int>
    ): Int? {
        val availableNumbers = botBoard.numbers.filterNotNull().filter { it !in calledNumbers }
        if (availableNumbers.isEmpty()) return null

        return availableNumbers.maxWithOrNull(
            compareBy<Int> { calculateTacticalScore(it, botBoard, calledNumbers) }
                .thenBy { calculateCentrality(it, botBoard) }
                .thenBy { -it }
        )
    }
}
