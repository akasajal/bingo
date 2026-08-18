package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.BotDifficulty

/** Wrapper that delegates to specific difficulty strategies. */
class BotMoveStrategy {
    private val easyStrategy = EasyBotStrategy()
    private val hardStrategy = HardBotStrategy()
    private val teeHeeStrategy = TeeHeeBotStrategy()

    fun chooseNumber(
        botBoard: BingoBoard,
        userBoard: BingoBoard,
        calledNumbers: Set<Int>,
        difficulty: BotDifficulty
    ): Int? {
        val strategy: BotStrategy = when (difficulty) {
            BotDifficulty.EASY -> easyStrategy
            BotDifficulty.HARD -> hardStrategy
            BotDifficulty.TEE_HEE -> teeHeeStrategy
        }
        return strategy.chooseNumber(botBoard, userBoard, calledNumbers)
    }

    /**
     * Specifically for TEE-HEE mode: generates a strategic board all at once.
     */
    fun manifestTeeHeeBoard(calledNumbers: List<Int>, uncalledNumbers: List<Int>): BingoBoard {
        return teeHeeStrategy.manifestBoard(calledNumbers, uncalledNumbers)
    }
}
