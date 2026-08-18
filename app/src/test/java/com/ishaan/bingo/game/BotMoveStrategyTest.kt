package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BotMoveStrategyTest {
    private val strategy = BotMoveStrategy()
    private val orderedBoard = BingoBoard((1..25).toList())

    @Test
    fun `opens with the centre number when it is available`() {
        assertEquals(13, strategy.chooseNumber(orderedBoard, emptySet()))
    }

    @Test
    fun `continues a row containing two called numbers`() {
        assertTrue(strategy.chooseNumber(orderedBoard, setOf(1, 2)) in 3..5)
    }

    @Test
    fun `finishes a nearly complete line before extending another`() {
        assertEquals(5, strategy.chooseNumber(orderedBoard, setOf(1, 2, 3, 4, 7, 8)))
    }
}
