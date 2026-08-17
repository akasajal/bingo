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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ishaan.bingo.domain.model.GameStatus
import com.ishaan.bingo.ui.AppViewModelProvider
import com.ishaan.bingo.ui.components.BingoGridOverlay
import com.ishaan.bingo.ui.theme.bingoColors
import com.ishaan.bingo.ui.screens.settings.SettingsViewModel
import com.ishaan.bingo.ui.theme.HapticManager

@Composable
fun GameScreen(
    roomId: String,
    onGameFinished: (String) -> Unit,
    viewModel: GameViewModel,
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val confirmCalls by settingsViewModel.confirmCalls.collectAsState()
    val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState()
    val room = uiState.gameRoom
    val board = uiState.playerBoard
    val myPlayer = room?.players?.get(viewModel.repository.playerId)

    val context = LocalContext.current
    val hapticManager = remember { HapticManager(context) }

    // For double-tap confirmation
    var selectedNumber by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(room?.status, room?.winnerPlayerId) {
        if (room?.status == GameStatus.FINISHED && room.winnerPlayerId != null) {
            onGameFinished(room.winnerPlayerId)
        }
    }

    // Clear selection if turn ends or number is called
    LaunchedEffect(room?.currentTurnPlayerId, room?.calledNumbers) {
        selectedNumber = null
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
        val isPlaying = room?.status == GameStatus.PLAYING

        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    !isPlaying -> MaterialTheme.colorScheme.surfaceVariant
                    isMyTurn -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = when {
                    !isPlaying -> "⌛ WAITING FOR OPPONENT..."
                    isMyTurn -> "⚡ YOUR TURN"
                    else -> "⌛ OPPONENT'S TURN"
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
                        val isSelected = number != null && selectedNumber == number

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    color = when {
                                        isCalled -> MaterialTheme.bingoColors.calledCell
                                        isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.bingoColors.cell
                                    }
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = MaterialTheme.shapes.small
                                )
                                .clickable(enabled = isMyTurn && !isCalled && number != null) {
                                    number?.let { n ->
                                        if (hapticsEnabled) hapticManager.performTick()
                                        if (!confirmCalls) {
                                            viewModel.callNumber(n)
                                        } else {
                                            if (selectedNumber == n) {
                                                viewModel.callNumber(n)
                                                selectedNumber = null
                                            } else {
                                                selectedNumber = n
                                            }
                                        }
                                    }
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

                val history = room?.calledNumbers?.reversed() ?: emptyList()
                val historyText = buildAnnotatedString {
                    history.forEachIndexed { index, number ->
                        val callerId = room?.callerMap?.get(number.toString())
                        val isMe = callerId == viewModel.repository.playerId

                        if (isMe) {
                            withStyle(style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )) {
                                append(number.toString())
                            }
                        } else {
                            withStyle(style = SpanStyle(
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Normal,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )) {
                                append(number.toString())
                            }
                        }

                        if (index < history.size - 1) {
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) {
                                append("  ·  ")
                            }
                        }
                    }
                    if (history.isEmpty()) {
                        append("No numbers called yet")
                    }
                }

                Text(
                    text = historyText,
                    style = MaterialTheme.typography.bodyMedium,
                    minLines = 2,
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isMe) androidx.compose.ui.text.font.FontStyle.Normal else androidx.compose.ui.text.font.FontStyle.Italic,
                    textDecoration = if (isMe) androidx.compose.ui.text.style.TextDecoration.Underline else null
                )) {
                    append(label)
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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