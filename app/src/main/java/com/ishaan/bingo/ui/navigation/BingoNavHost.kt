package com.ishaan.bingo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ishaan.bingo.ui.AppViewModelProvider
import com.ishaan.bingo.ui.screens.lobby.LobbyScreen
import com.ishaan.bingo.ui.screens.setup.BoardSetupScreen
import com.ishaan.bingo.ui.screens.game.GameScreen
import com.ishaan.bingo.ui.screens.result.ResultScreen
import com.ishaan.bingo.ui.screens.settings.SettingsScreen
import com.ishaan.bingo.ui.screens.settings.presets.PresetListScreen
import com.ishaan.bingo.ui.screens.settings.presets.CreatePresetScreen
import com.ishaan.bingo.ui.screens.settings.howto.HowToPlayScreen

sealed class Screen(val route: String) {
    object Lobby : Screen("lobby")
    object Settings : Screen("settings")
    object HowToPlay : Screen("how_to_play")
    object PresetList : Screen("preset_list")
    object CreatePreset : Screen("create_preset?presetId={presetId}") {
        fun createRoute(presetId: String? = null) = 
            if (presetId != null) "create_preset?presetId=$presetId" else "create_preset"
    }
    object BoardSetup : Screen("setup/{roomId}") {
        fun createRoute(roomId: String) = "setup/$roomId"
    }
    object Game : Screen("game/{roomId}") {
        fun createRoute(roomId: String) = "game/$roomId"
    }
    object Result : Screen("result/{roomId}/{winnerId}") {
        fun createRoute(roomId: String, winnerId: String) = "result/$roomId/$winnerId"
    }
}

@Composable
fun BingoNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Screen.Lobby.route) {
        composable(Screen.Lobby.route) {
            LobbyScreen(
                onGameJoined = { roomId ->
                    navController.navigate(Screen.BoardSetup.createRoute(roomId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onPresetsClick = { navController.navigate(Screen.PresetList.route) },
                onHowToPlayClick = { navController.navigate(Screen.HowToPlay.route) }
            )
        }
        composable(Screen.HowToPlay.route) {
            HowToPlayScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.PresetList.route) {
            PresetListScreen(
                onBackClick = { navController.popBackStack() },
                onAddClick = { navController.navigate(Screen.CreatePreset.createRoute()) },
                onEditClick = { presetId -> 
                    navController.navigate(Screen.CreatePreset.createRoute(presetId))
                }
            )
        }
        composable(Screen.CreatePreset.route) { backStackEntry ->
            val presetId = backStackEntry.arguments?.getString("presetId")
            CreatePresetScreen(
                presetId = presetId,
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(Screen.BoardSetup.route) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            BoardSetupScreen(
                roomId = roomId,
                onStartGame = {
                    navController.navigate(Screen.Game.createRoute(roomId))
                }
            )
        }
        composable(Screen.Game.route) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            GameScreen(
                roomId = roomId,
                onGameFinished = { winnerId ->
                    navController.navigate(Screen.Result.createRoute(roomId, winnerId))
                },
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = AppViewModelProvider.gameViewModelFactory(roomId)
                )
            )
        }
        composable(Screen.Result.route) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            ResultScreen(
                roomId = roomId,
                onPlayAgain = {
                    navController.navigate(Screen.Lobby.route) {
                        popUpTo(Screen.Lobby.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
