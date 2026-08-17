package com.ishaan.bingo.ui.screens.settings.howto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishaan.bingo.ui.theme.bingoColors
import kotlinx.coroutines.launch

data class HowToStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
)

val howToSteps: List<HowToStep>
    @Composable
    get() = listOf(
        HowToStep(
            title = "The Lobby",
            description = "Start by creating a new game or joining one. Share your unique 5-digit code with a friend to play together!",
            icon = Icons.Default.Groups
        ),
        HowToStep(
            title = "Build Your Board",
            description = "Place numbers 1-25 in any order on your secret 5x5 grid. Your opponent cannot see your arrangement!",
            icon = Icons.Default.GridView
        ),
        HowToStep(
            title = "Ready Up",
            description = "Once your board is complete, tap START. The game begins as soon as both players are ready.",
            icon = Icons.Default.CheckCircle
        ),
        HowToStep(
            title = "Call a Number",
            description = "On your turn, tap any uncalled number on your board to call it out. Strategy is key!",
            icon = Icons.Default.Campaign
        ),
        HowToStep(
            title = "Cross it Out",
            description = "When a number is called, it gets crossed out on BOTH boards. Watch your board fill up!",
            icon = Icons.Default.Cancel,
            iconColor = { MaterialTheme.bingoColors.calledCell }
        ),
        HowToStep(
            title = "B-I-N-G-O Progress",
            description = "Complete a row, column, or diagonal to earn a letter. Every new line gets you one step closer to B-I-N-G-O.",
            icon = Icons.Default.Timeline,
            iconColor = { MaterialTheme.bingoColors.success }
        ),
        HowToStep(
            title = "Victory!",
            description = "The first player to earn all 5 letters wins the game. Call wisely and aim for BINGO!",
            icon = Icons.Default.EmojiEvents,
            iconColor = { MaterialTheme.colorScheme.secondary }
        )
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(
    onBackClick: () -> Unit
) {
    val steps = howToSteps
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Play", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val step = steps[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(bottom = 32.dp),
                        shape = RoundedCornerShape(percent = 40),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = step.iconColor()
                            )
                        }
                    }
                    
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Pager Indicator and Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Text("PREVIOUS")
                }

                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(steps.size) { index ->
                        val active = pagerState.currentPage == index
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = if (active) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(if (active) 10.dp else 8.dp)
                        ) {}
                    }
                }

                // Next/Finish Button
                if (pagerState.currentPage == steps.size - 1) {
                    Button(onClick = onBackClick) {
                        Text("FINISH")
                    }
                } else {
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    }
}
