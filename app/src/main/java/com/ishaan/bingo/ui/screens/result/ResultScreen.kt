package com.ishaan.bingo.ui.screens.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.ui.AppViewModelProvider
import com.ishaan.bingo.ui.components.BingoGridOverlay
import com.ishaan.bingo.ui.theme.bingoColors

@Composable
fun ResultScreen(
    roomId: String,
    onHome: () -> Unit,
    onPlayAgainReady: () -> Unit,
    viewModel: ResultViewModel = viewModel(factory = AppViewModelProvider.resultViewModelFactory(roomId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val room = uiState.gameRoom ?: return
    var playAgainNavigationHandled by remember(roomId) { mutableStateOf(false) }

    LaunchedEffect(room.status) {
        if (!playAgainNavigationHandled && room.status == GameStatus.BOARD_SETUP) {
            playAgainNavigationHandled = true
            onPlayAgainReady()
        }
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Could not play again") },
            text = { Text(uiState.error.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    BackHandler { onHome() }
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
            .systemBarsPadding()
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
                ResultBoard(
                    board = currentBoard,
                    calledNumbers = remember(room.calledNumbers) { room.calledNumbers.toSet() },
                    modifier = Modifier.fillMaxSize()
                )

                // Strikethrough Overlay for revealed board — Calculate locally for visual consistency
                val localLineDetector = remember { com.ishaan.bingo.game.BingoLineDetector() }
                val currentBoardNumbers = currentBoard?.numbers ?: List(25) { null }
                val locallyCompletedLines = remember(room.calledNumbers, currentBoardNumbers) {
                    localLineDetector.detectCompletedLines(currentBoardNumbers, room.calledNumbers.toSet()).toList()
                }

                BingoGridOverlay(
                    completedLines = locallyCompletedLines,
                    lineColor = MaterialTheme.bingoColors.success.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !uiState.isPlayingAgain
            ) {
                Text("HOME", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.playAgain() },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !uiState.isPlayingAgain
            ) {
                if (uiState.isPlayingAgain) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("PLAY AGAIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ResultBoard(
    board: BingoBoard?,
    calledNumbers: Set<Int>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(5) { row ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { column ->
                    val index = row * 5 + column
                    val number = board?.numbers?.get(index)
                    val isCalled = number != null && number in calledNumbers

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
        }
    }
}
