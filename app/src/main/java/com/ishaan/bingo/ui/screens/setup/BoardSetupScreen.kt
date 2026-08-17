package com.ishaan.bingo.ui.screens.setup

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ishaan.bingo.ui.theme.bingoColors

@Composable
fun BoardSetupScreen(
    roomId: String,
    onStartGame: () -> Unit,
    viewModel: BoardSetupViewModel = viewModel(factory = com.ishaan.bingo.ui.AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear Board?") },
            text = { Text("This will remove all numbers you have placed. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("CLEAR ALL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "BUILD YOUR BOARD", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Tap cells to place numbers 1–25 sequentially.", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.bingoColors.board, MaterialTheme.shapes.medium)
                .padding(8.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(25) { index ->
                val number = uiState.board.numbers[index]
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            color = if (number != null) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.bingoColors.cell
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                        .clickable { 
                            if (number == null) {
                                viewModel.onCellClick(index)
                            } else if (index == uiState.history.lastOrNull()) {
                                viewModel.undo() // Tapping ONLY the last placed box undoes it
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (number != null) {
                        Text(
                            text = number.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp)) // Professional gap after grid
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            val statusText = if (uiState.nextNumber <= 25) {
                "Next Number: ${uiState.nextNumber}"
            } else {
                "BOARD COMPLETE"
            }
            Text(
                text = statusText,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(48.dp)) // Distinct gap before buttons

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.undo() }, 
                enabled = uiState.history.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("UNDO", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { showDeleteDialog = true }, 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("DELETE", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Undo last placement or clear the board",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.submitBoard(roomId, onStartGame) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isReady,
            shape = MaterialTheme.shapes.medium
        ) {
            Text("START GAME", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
