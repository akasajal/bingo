package com.ishaan.bingo.game

import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class BingoGameEngineTest {
    private val engine = BingoGameEngine()
    
    private val p1Id = "p1"
    private val p2Id = "p2"
    
    private val p1Board = BingoBoard((1..25).toList())
    private val p2Board = BingoBoard((1..25).toList().reversed()) // Reversed to be different

    private val initialRoom = GameRoom(
        id = "room1",
        status = GameStatus.PLAYING,
        players = mapOf(
            p1Id to Player(id = p1Id),
            p2Id to Player(id = p2Id)
        ),
        currentTurnPlayerId = p1Id
    )

    @Test
    fun `calling a number that completes a line awards progress`() {
        // P1 board has 1..5 in first row.
        // We call 1, 2, 3, 4. No progress yet.
        var room = initialRoom
        val boards = mapOf(p1Id to p1Board, p2Id to p2Board)
        
        for (i in 1..4) {
            room = engine.processCall(room, boards, i)
        }
        
        assertEquals(0, room.players[p1Id]?.bingoProgress)
        
        // Call 5 to complete Row 0
        room = engine.processCall(room, boards, 5)
        
        assertEquals(1, room.players[p1Id]?.bingoProgress) // 'B' earned
        assertEquals(p2Id, room.currentTurnPlayerId)
    }

    @Test
    fun `multiple lines in one turn award multiple progress letters`() {
        // P1 board has 1 at (0,0). Row 0 is 1..5, Col 0 is 1,6,11,16,21, Diag 0 is 1,7,13,19,25.
        // If we call all but '1' and then call '1', 3 lines complete.
        var room = initialRoom
        val boards = mapOf(p1Id to p1Board, p2Id to p2Board)
        
        val setupNumbers = listOf(2,3,4,5, 6,11,16,21, 7,13,19,25)
        setupNumbers.forEach { room = engine.processCall(room, boards, it) }
        
        assertEquals(0, room.players[p1Id]?.bingoProgress)
        
        room = engine.processCall(room, boards, 1)
        
        // Now awards 3 progress letters for 3 lines
        assertEquals(3, room.players[p1Id]?.bingoProgress) 
        assertEquals(3, room.players[p1Id]?.completedLines?.size)
    }

    @Test
    fun `first player to reach 5 progress wins`() {
        var room = initialRoom
        val boards = mapOf(p1Id to p1Board, p2Id to p2Board)
        
        // Call numbers to complete 5 rows for P1
        // Row 0: 1-5, Row 1: 6-10, Row 2: 11-15, Row 3: 16-20, Row 4: 21-25
        // We need 5 turns where progress is earned.
        
        // Turn 1: 1,2,3,4,5 -> P1 progress 1
        for (i in 1..5) room = engine.processCall(room, boards, i)
        // Turn 2: 6,7,8,9,10 -> P1 progress 2
        for (i in 6..10) room = engine.processCall(room, boards, i)
        // Turn 3: 11,12,13,14,15 -> P1 progress 3
        for (i in 11..15) room = engine.processCall(room, boards, i)
        // Turn 4: 16,17,18,19,20 -> P1 progress 4
        for (i in 16..20) room = engine.processCall(room, boards, i)
        // Turn 5: 21,22,23,24,25 -> P1 progress 5 -> WIN
        for (i in 21..25) room = engine.processCall(room, boards, i)
        
        assertEquals(5, room.players[p1Id]?.bingoProgress)
        assertEquals(GameStatus.FINISHED, room.status)
        assertEquals(p1Id, room.winnerPlayerId)
    }
}
