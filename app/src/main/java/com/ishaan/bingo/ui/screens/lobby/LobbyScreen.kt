package com.ishaan.bingo.ui.screens.lobby

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ishaan.bingo.domain.model.BotDifficulty

@Composable
fun LobbyScreen(
    onGameJoined: (roomId: String, isBot: Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: LobbyViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var joinCode by remember { mutableStateOf("") }
    var showDifficultyPopup by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.shouldNavigateToSetup, uiState.joinedRoomId) {
        if (uiState.shouldNavigateToSetup) {
            uiState.joinedRoomId?.let {
                viewModel.consumeNavigation() // clear flag before navigating so back doesn't re-trigger
                onGameJoined(it, uiState.isBotGame)
            }
        }
    }

    if (showDifficultyPopup) {
        AlertDialog(
            onDismissRequest = { showDifficultyPopup = false },
            title = { Text("Select Difficulty", fontWeight = FontWeight.Bold) },
            text = { Text("Choose how smart the bot should be.") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.playWithBot(BotDifficulty.EASY)
                            showDifficultyPopup = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("EASY MODE")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.playWithBot(BotDifficulty.HARD)
                            showDifficultyPopup = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("HARD MODE")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDifficultyPopup = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (uiState.gameCode.isNotBlank() && !uiState.shouldNavigateToSetup && !uiState.isBotGame) {
        AlertDialog(
            onDismissRequest = { viewModel.resetLobby() },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Game Created!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Share this code with your friend:", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 4.dp
                    ) {
                        Text(
                            text = uiState.gameCode,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        if (uiState.isLoading) "Creating game..." else "Waiting for opponent...",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.resetLobby() }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Something went wrong") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("OK") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "BINGO",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = { viewModel.createGame() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isLoading,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("CREATE NEW GAME", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showDifficultyPopup = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isLoading,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("PLAY VS BOT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("  OR JOIN  ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = joinCode,
                onValueChange = { 
                    joinCode = it.uppercase()
                    viewModel.onCodeInputChanged(joinCode)
                },
                label = { Text("Game Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.joinGame(joinCode) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isLoading && joinCode.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isLoading && uiState.joinedRoomId == null) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("JOINING...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text("JOIN GAME", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.isLoading && uiState.joinedRoomId != null) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator()
            }

        }

        Surface(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).size(56.dp),
            shape = RoundedCornerShape(percent = 40),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
