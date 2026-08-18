package com.ishaan.bingo.ui.screens.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.ui.AppViewModelProvider
import com.ishaan.bingo.ui.components.BingoGridOverlay
import com.ishaan.bingo.ui.theme.bingoColors
import com.ishaan.bingo.ui.screens.settings.SettingsViewModel
import com.ishaan.bingo.ui.theme.HapticManager
import com.ishaan.bingo.game.BingoLineDetector
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    roomId: String,
    onGameFinished: (String) -> Unit,
    onForfeit: () -> Unit,
    viewModel: GameViewModel,
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val confirmCalls by settingsViewModel.confirmCalls.collectAsState()
    val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState()
    val room = uiState.gameRoom
    val board = uiState.playerBoard
    
    val context = LocalContext.current
    val hapticManager = remember { HapticManager(context) }

    // For double-tap confirmation
    var selectedNumber by remember { mutableStateOf<Int?>(null) }
    var resultHandled by remember(roomId) { mutableStateOf(false) }

    LaunchedEffect(room?.status, room?.winnerPlayerId) {
        if (!resultHandled && room?.status == GameStatus.FINISHED && room.winnerPlayerId != null) {
            resultHandled = true
            delay(650)
            onGameFinished(room.winnerPlayerId)
        }
    }

    // Clear selection if turn ends or number is called
    LaunchedEffect(room?.currentTurnPlayerId, room?.calledNumbers) {
        selectedNumber = null
    }

    // Intercept system back — show confirm dialog instead of silently popping
    var showForfeitDialog by remember { mutableStateOf(false) }
    BackHandler { showForfeitDialog = true }

    if (showForfeitDialog) {
        AlertDialog(
            onDismissRequest = { showForfeitDialog = false },
            title = { Text("Forfeit game?") },
            text = { Text("Are you sure you want to leave? Your current game will be abandoned.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForfeitDialog = false
                        onForfeit()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("FORFEIT") }
            },
            dismissButton = {
                TextButton(onClick = { showForfeitDialog = false }) { Text("KEEP PLAYING") }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // BINGO Progress Header — Prefer local progress for zero latency
        BingoHeader(progress = uiState.localBingoProgress)

        Spacer(modifier = Modifier.height(12.dp))

        // Turn Indicator Card
        val isMyTurn = room?.currentTurnPlayerId == viewModel.repository.playerId
        val isPlaying = room?.status == GameStatus.PLAYING
        val turnContainerColor by animateColorAsState(
            targetValue = when {
                !isPlaying -> MaterialTheme.colorScheme.surfaceVariant
                isMyTurn -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            animationSpec = tween(durationMillis = 180),
            label = "turnContainerColor"
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = turnContainerColor),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = when {
                    !isPlaying -> "WAITING FOR OPPONENT"
                    isMyTurn -> "YOUR TURN"
                    else -> "OPPONENT'S TURN"
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    !isPlaying -> MaterialTheme.colorScheme.onSurfaceVariant
                    isMyTurn -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5x5 Board with Strikethrough Overlay
        val calledNumbers = remember(room?.calledNumbers) {
            room?.calledNumbers?.toSet().orEmpty()
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.bingoColors.board, MaterialTheme.shapes.medium)
                .padding(8.dp)
            ) {
                BingoGameBoard(
                    board = board,
                    calledNumbers = calledNumbers,
                    selectedNumber = selectedNumber,
                    isMyTurn = isMyTurn && !uiState.isCallingNumber,
                    onNumberClick = { n ->
                        if (hapticsEnabled) hapticManager.performTick()
                        if (!confirmCalls) {
                            viewModel.callNumber(n)
                        } else if (selectedNumber == n) {
                            viewModel.callNumber(n)
                            selectedNumber = null
                        } else {
                            selectedNumber = n
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Strikethrough Overlay — Unify logic locally to prevent visual desync
                val localLineDetector = remember { BingoLineDetector() }
                val currentBoardNumbers = board?.numbers ?: List(25) { null }
                val locallyCompletedLines = remember(calledNumbers, currentBoardNumbers) {
                    localLineDetector.detectCompletedLines(currentBoardNumbers, calledNumbers).toList()
                }

                BingoGridOverlay(
                    completedLines = locallyCompletedLines,
                    lineColor = MaterialTheme.bingoColors.success.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Last Called Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "CALL HISTORY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                val history = room?.calledNumbers?.reversed() ?: emptyList()
                val callerMap = room?.callerMap.orEmpty()
                val playerId = viewModel.repository.playerId
                val myColor = MaterialTheme.colorScheme.primary
                val opponentColor = MaterialTheme.colorScheme.outline
                
                val historyText = remember(history, callerMap, playerId, myColor, opponentColor) {
                    buildCallHistoryText(history, callerMap, playerId, myColor, opponentColor)
                }

                Text(
                    text = historyText,
                    style = MaterialTheme.typography.bodyMedium,
                    minLines = 2,
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Color Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(
                color = MaterialTheme.colorScheme.primary,
                label = "My Calls",
                isMe = true
            )
            Spacer(modifier = Modifier.width(24.dp))
            LegendItem(
                color = MaterialTheme.colorScheme.outline,
                label = "Opponent Calls",
                isMe = false
            )
        }
    }
}

@Composable
private fun BingoGameBoard(
    board: BingoBoard?,
    calledNumbers: Set<Int>,
    selectedNumber: Int?,
    isMyTurn: Boolean,
    onNumberClick: (Int) -> Unit,
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
                    val isSelected = number != null && selectedNumber == number
                    val cellColor by animateColorAsState(
                        targetValue = when {
                            isCalled -> MaterialTheme.bingoColors.calledCell
                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.bingoColors.cell
                        },
                        animationSpec = tween(durationMillis = 140),
                        label = "cellColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.small)
                            .background(cellColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable(enabled = isMyTurn && !isCalled && number != null) {
                                number?.let(onNumberClick)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (number != null) {
                            Text(
                                text = number.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
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

private fun buildCallHistoryText(
    history: List<Int>,
    callerMap: Map<String, String>,
    playerId: String,
    myColor: Color,
    opponentColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        history.forEachIndexed { index, number ->
            val callerId = callerMap[number.toString()]
            val isMe = callerId == playerId

            if (isMe) {
                withStyle(style = SpanStyle(
                    color = myColor,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )) {
                    append(number.toString())
                }
            } else {
                withStyle(style = SpanStyle(
                    color = opponentColor,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic
                )) {
                    append(number.toString())
                }
            }

            if (index < history.size - 1) {
                withStyle(style = SpanStyle(color = opponentColor.copy(alpha = 0.6f))) {
                    append("  ·  ")
                }
            }
        }
        if (history.isEmpty()) {
            append("No numbers called yet")
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, isMe: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, MaterialTheme.shapes.extraSmall)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(
                    color = color,
                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isMe) FontStyle.Normal else FontStyle.Italic,
                    textDecoration = if (isMe) TextDecoration.Underline else null
                )) {
                    append(label)
                }
            },
            style = MaterialTheme.typography.labelSmall
        )
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
