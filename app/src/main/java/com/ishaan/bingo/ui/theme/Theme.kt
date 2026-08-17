package com.ishaan.bingo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ishaan.bingo.ui.domain.model.ThemeMode

@Immutable
data class BingoColors(
    val board: Color,
    val cell: Color,
    val success: Color,
    val calledCell: Color
)

val LocalBingoColors = staticCompositionLocalOf {
    BingoColors(
        board = Color.Unspecified,
        cell = Color.Unspecified,
        success = Color.Unspecified,
        calledCell = Color.Unspecified
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = BackgroundDark, // Dark text on light coral
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = TextDark,
    secondary = SecondaryDark,
    onSecondary = BackgroundDark, // Dark text on golden amber
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = TextDark,
    background = BackgroundDark,
    onBackground = TextDark,
    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MutedTextDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = TextLight,
    secondary = SecondaryLight,
    onSecondary = SurfaceLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = TextLight,
    background = BackgroundLight,
    onBackground = TextLight,
    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = MutedTextLight,
    outline = OutlineLight
)

private val DarkBingoColors = BingoColors(
    board = BoardDark,
    cell = CellDark,
    success = BingoCompleteDark,
    calledCell = CalledCellDark
)

private val LightBingoColors = BingoColors(
    board = BoardLight,
    cell = CellLight,
    success = BingoCompleteLight,
    calledCell = CalledCellLight
)

@Composable
fun BingoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val bingoColors = if (darkTheme) DarkBingoColors else LightBingoColors

    CompositionLocalProvider(LocalBingoColors provides bingoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

val MaterialTheme.bingoColors: BingoColors
    @Composable
    get() = LocalBingoColors.current
