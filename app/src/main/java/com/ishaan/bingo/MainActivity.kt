package com.ishaan.bingo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ishaan.bingo.data.repository.BingoDatabase
import com.ishaan.bingo.ui.AppViewModelProvider
import com.ishaan.bingo.ui.navigation.BingoNavHost
import com.ishaan.bingo.ui.screens.settings.SettingsViewModel
import com.ishaan.bingo.ui.theme.BingoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppViewModelProvider.init(BingoDatabase.getInstance(this))
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val themeMode by settingsViewModel.themeMode.collectAsState()
            BingoTheme(themeMode = themeMode) {
                // Plain Surface — each screen handles its own insets
                Surface(modifier = Modifier.fillMaxSize()) {
                    BingoNavHost()
                }
            }
        }
    }
}