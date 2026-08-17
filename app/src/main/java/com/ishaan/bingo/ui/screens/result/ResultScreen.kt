package com.ishaan.bingo.ui.screens.result

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ResultScreen(
    roomId: String,
    winnerId: String,
    myPlayerId: String,
    onPlayAgain: () -> Unit
) {
    val isWinner = winnerId == myPlayerId
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isWinner) "BINGO!" else "GAME OVER",
            style = MaterialTheme.typography.displayLarge,
            color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isWinner) "You won!" else "You lost.",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
            Text("PLAY AGAIN")
        }
    }
}
