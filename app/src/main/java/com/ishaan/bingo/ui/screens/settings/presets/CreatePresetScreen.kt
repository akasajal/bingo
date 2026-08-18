package com.ishaan.bingo.ui.screens.settings.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ishaan.bingo.ui.AppViewModelProvider
import com.ishaan.bingo.ui.screens.setup.BoardSetupViewModel
import com.ishaan.bingo.ui.theme.bingoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePresetScreen(
    presetId: String? = null,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: BoardSetupViewModel = viewModel(factory = AppViewModelProvider.boardSetupViewModelFactory("", isBot = false)),
    presetViewModel: PresetViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val allPresets by presetViewModel.presets.collectAsState()
    var presetName by remember { mutableStateOf("My Board") }
    val isEditMode = presetId != null

    // Load existing board data if in edit mode
    LaunchedEffect(presetId) {
        if (isEditMode) {
            val existing = allPresets.find { it.id == presetId }
            existing?.let {
                presetName = it.name
                viewModel.loadBoard(it.board)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Preset" else "Create Preset",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Transparent so it shows Scaffold background
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Explicitly set Scaffold background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("Preset Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                                    viewModel.undo()
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

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.undo() },
                    enabled = uiState.history.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("UNDO")
                }
                Button(
                    onClick = { viewModel.delete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("DELETE")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.saveAsPreset(
                        name = presetName,
                        repository = AppViewModelProvider.presetRepository,
                        existingId = presetId,
                        onComplete = onSaved
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isReady
            ) {
                Text(
                    text = if (isEditMode) "UPDATE PRESET" else "SAVE PRESET",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}