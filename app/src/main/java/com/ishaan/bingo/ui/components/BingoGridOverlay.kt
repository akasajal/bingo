package com.ishaan.bingo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun BingoGridOverlay(
    completedLines: Set<String>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cellSize = size.width / 5f
        val strokeWidth = 6.dp.toPx()

        completedLines.forEach { lineId ->
            val index = lineId.removePrefix("LINE_").toIntOrNull() ?: return@forEach
            
            when {
                // Rows (LINE_0 to LINE_4)
                index in 0..4 -> {
                    val y = (index * cellSize) + (cellSize / 2f)
                    drawLine(
                        color = lineColor,
                        start = Offset(x = 0f, y = y),
                        end = Offset(x = size.width, y = y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
                // Columns (LINE_5 to LINE_9)
                index in 5..9 -> {
                    val colIndex = index - 5
                    val x = (colIndex * cellSize) + (cellSize / 2f)
                    drawLine(
                        color = lineColor,
                        start = Offset(x = x, y = 0f),
                        end = Offset(x = x, y = size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
                // Diagonals
                index == 10 -> { // Main Diagonal (Top-Left to Bottom-Right)
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
                index == 11 -> { // Anti Diagonal (Top-Right to Bottom-Left)
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
