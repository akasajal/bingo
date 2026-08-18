package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.BotDifficulty

/** Wrapper that delegates to specific difficulty strategies. */
class BotMoveStrategy {
    private val easyStrategy = EasyBotStrategy()
    private val hardStrategy = HardBotStrategy()

    fun chooseNumber(
        botBoard: BingoBoard,
        userBoard: BingoBoard,
        calledNumbers: Set<Int>,
        difficulty: BotDifficulty
    ): Int? {
        val strategy = when (difficulty) {
            BotDifficulty.EASY -> easyStrategy
            BotDifficulty.HARD -> hardStrategy
        }
        return strategy.chooseNumber(botBoard, userBoard, calledNumbers)
    }
}
