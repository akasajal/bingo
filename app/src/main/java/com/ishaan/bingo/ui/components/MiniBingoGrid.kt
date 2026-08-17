package com.ishaan.bingo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishaan.bingo.domain.model.BingoBoard
import com.ishaan.bingo.ui.theme.bingoColors

@Composable
fun MiniBingoGrid(board: BingoBoard) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        repeat(5) { rowIndex ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(5) { colIndex ->
                    val index = rowIndex * 5 + colIndex
                    val number = board.numbers[index]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.bingoColors.cell,
                                shape = RoundedCornerShape(1.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (number != null) {
                            Text(
                                text = number.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
