package com.ishaan.bingo.ui.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameRoom
import com.ishaan.bingo.ui.AppViewModelProvider
import com.ishaan.bingo.ui.components.BingoGridOverlay
import com.ishaan.bingo.ui.theme.bingoColors

@Composable
fun ResultScreen(
    roomId: String,
    onPlayAgain: () -> Unit,
    viewModel: ResultViewModel = viewModel(factory = AppViewModelProvider.resultViewModelFactory(roomId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val room = uiState.gameRoom ?: return
    val isWinner = room.winnerPlayerId == viewModel.myPlayerId
    
    val currentBoard = if (uiState.showingOpponentBoard) uiState.opponentBoard else uiState.myBoard
    val viewingPlayerId = if (uiState.showingOpponentBoard) {
        room.players.keys.firstOrNull { it != viewModel.myPlayerId }
    } else {
        viewModel.myPlayerId
    }
    val viewingPlayer = room.players[viewingPlayerId]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isWinner) "BINGO!" else "GAME OVER",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = if (isWinner) MaterialTheme.bingoColors.success else MaterialTheme.colorScheme.error
        )
        
        Text(
            text = if (isWinner) "Victory is yours!" else "Better luck next time.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Reveal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (uiState.showingOpponentBoard) "OPPONENT'S BOARD" else "YOUR BOARD",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BINGO Progress: ${viewingPlayer?.bingoProgress ?: 0}/5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = { viewModel.toggleBoardReveal() },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (uiState.showingOpponentBoard) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (uiState.showingOpponentBoard) "SHOW MINE" else "SHOW OPPONENT")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5x5 Board Display
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
                        val number = currentBoard?.numbers?.get(index)
                        val isCalled = number != null && room.calledNumbers.contains(number)
                        
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    color = if (isCalled) MaterialTheme.bingoColors.calledCell
                                            else MaterialTheme.bingoColors.cell
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
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

                // Strikethrough Overlay for revealed board
                viewingPlayer?.let { player ->
                    BingoGridOverlay(
                        completedLines = player.completedLines,
                        lineColor = MaterialTheme.bingoColors.success.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("PLAY AGAIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
