package com.ishaan.bingo.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.ui.AppViewModelProvider
import com.ishaan.bingo.ui.components.BingoGridOverlay
import com.ishaan.bingo.ui.theme.bingoColors

@Composable
fun GameScreen(
    roomId: String,
    onGameFinished: (String) -> Unit,
    viewModel: GameViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val room = uiState.gameRoom
    val board = uiState.playerBoard
    val myPlayer = room?.players?.get(viewModel.repository.playerId)
    
    LaunchedEffect(room?.status, room?.winnerPlayerId) {
        if (room?.status == GameStatus.FINISHED && room.winnerPlayerId != null) {
            onGameFinished(room.winnerPlayerId)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // BINGO Progress Header
        BingoHeader(progress = myPlayer?.bingoProgress ?: 0)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Turn Indicator Card
        val isMyTurn = room?.currentTurnPlayerId == viewModel.repository.playerId
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMyTurn) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = if (isMyTurn) "⚡ YOUR TURN" else "⌛ OPPONENT'S TURN",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isMyTurn) MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 5x5 Board with Strikethrough Overlay
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.bingoColors.board, MaterialTheme.shapes.medium)
                .padding(8.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(25) { index ->
                        val number = board?.numbers?.get(index)
                        val isCalled = number != null && room?.calledNumbers?.contains(number) == true
                        
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    color = if (isCalled) MaterialTheme.bingoColors.calledCell
                                            else MaterialTheme.bingoColors.cell
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                .clickable(enabled = isMyTurn && !isCalled && number != null) {
                                    number?.let { viewModel.callNumber(it) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (number != null) {
                                Text(
                                    text = number.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCalled) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Strikethrough Overlay
                myPlayer?.let { player ->
                    BingoGridOverlay(
                        completedLines = player.completedLines,
                        lineColor = MaterialTheme.bingoColors.success.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Last Called Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "CALL HISTORY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = room?.calledNumbers?.reversed()?.joinToString("  ·  ") ?: "No numbers called yet",
                    style = MaterialTheme.typography.bodyMedium,
                    minLines = 2,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun BingoHeader(progress: Int) {
    val letters = listOf("B", "I", "N", "G", "O")
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        letters.forEachIndexed { index, letter ->
            val active = index < progress
            val containerColor = if (active) MaterialTheme.bingoColors.success 
                                else MaterialTheme.colorScheme.surface
            val contentColor = if (active) MaterialTheme.colorScheme.onPrimary 
                              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                modifier = Modifier
                    .size(56.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium
                    ),
                tonalElevation = if (active) 2.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}
