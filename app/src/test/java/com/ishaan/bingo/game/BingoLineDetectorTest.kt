package com.ishaan.bingo.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BingoLineDetectorTest {
    private val detector = BingoLineDetector()
    
    // A sample board: 1..25 sequentially
    private val sampleBoard = (1..25).toList()

    @Test
    fun `no numbers called returns no lines`() {
        val completedLines = detector.detectCompletedLines(sampleBoard, emptySet())
        assertTrue(completedLines.isEmpty())
    }

    @Test
    fun `completing first row returns LINE_0`() {
        val calledNumbers = setOf(1, 2, 3, 4, 5)
        val completedLines = detector.detectCompletedLines(sampleBoard, calledNumbers)
        assertEquals(setOf("LINE_0"), completedLines)
    }

    @Test
    fun `completing first column returns LINE_5`() {
        val calledNumbers = setOf(1, 6, 11, 16, 21)
        val completedLines = detector.detectCompletedLines(sampleBoard, calledNumbers)
        assertEquals(setOf("LINE_5"), completedLines)
    }

    @Test
    fun `completing main diagonal returns LINE_10`() {
        val calledNumbers = setOf(1, 7, 13, 19, 25)
        val completedLines = detector.detectCompletedLines(sampleBoard, calledNumbers)
        assertEquals(setOf("LINE_10"), completedLines)
    }

    @Test
    fun `completing anti diagonal returns LINE_11`() {
        val calledNumbers = setOf(5, 9, 13, 17, 21)
        val completedLines = detector.detectCompletedLines(sampleBoard, calledNumbers)
        assertEquals(setOf("LINE_11"), completedLines)
    }

    @Test
    fun `multiple lines completed simultaneously`() {
        // Completing Row 0 and Col 0 simultaneously with numbers {1,2,3,4,5} and {6,11,16,21}
        val calledNumbers = setOf(1, 2, 3, 4, 5, 6, 11, 16, 21)
        val completedLines = detector.detectCompletedLines(sampleBoard, calledNumbers)
        assertEquals(setOf("LINE_0", "LINE_5"), completedLines)
    }
}
