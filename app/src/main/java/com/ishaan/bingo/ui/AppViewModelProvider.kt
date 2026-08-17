package com.ishaan.bingo.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ishaan.bingo.data.remote.FirebaseGameDataSource
import com.ishaan.bingo.data.repository.GameRepositoryImpl
import com.ishaan.bingo.ui.screens.game.GameViewModel
import com.ishaan.bingo.ui.screens.lobby.LobbyViewModel
import com.ishaan.bingo.ui.screens.setup.BoardSetupViewModel
import com.ishaan.bingo.ui.screens.settings.SettingsViewModel
import com.ishaan.bingo.ui.screens.settings.presets.PresetViewModel
import com.ishaan.bingo.ui.screens.result.ResultViewModel
import com.ishaan.bingo.data.repository.LocalPresetRepository

object AppViewModelProvider {
    // Switch to GameRepositoryImpl for actual 1v1 multiplayer
    val repository = GameRepositoryImpl(FirebaseGameDataSource())
    // val repository = com.ishaan.bingo.data.repository.LocalGameRepository()
    val settingsViewModel = SettingsViewModel()
    val presetRepository = LocalPresetRepository()

    val Factory = viewModelFactory {
        initializer {
            LobbyViewModel(repository)
        }
        initializer {
            BoardSetupViewModel(repository)
        }
        initializer {
            settingsViewModel
        }
        initializer {
            PresetViewModel(presetRepository)
        }
    }

    fun gameViewModelFactory(roomId: String) = viewModelFactory {
        initializer {
            GameViewModel(repository, roomId)
        }
    }

    fun resultViewModelFactory(roomId: String) = viewModelFactory {
        initializer {
            ResultViewModel(repository, roomId)
        }
    }
}
