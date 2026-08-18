package com.ishaan.bingo.ui

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ishaan.bingo.data.remote.FirebaseGameDataSource
import com.ishaan.bingo.data.repository.BingoDatabase
import com.ishaan.bingo.data.repository.GameRepositoryImpl
import com.ishaan.bingo.data.repository.LocalGameRepository
import com.ishaan.bingo.data.repository.LocalPresetRepository
import com.ishaan.bingo.ui.screens.game.GameViewModel
import com.ishaan.bingo.ui.screens.lobby.LobbyViewModel
import com.ishaan.bingo.ui.screens.result.ResultViewModel
import com.ishaan.bingo.ui.screens.settings.SettingsViewModel
import com.ishaan.bingo.ui.screens.settings.presets.PresetViewModel
import com.ishaan.bingo.ui.screens.setup.BoardSetupViewModel

object AppViewModelProvider {
    val repository = GameRepositoryImpl(FirebaseGameDataSource())
    val botRepository = LocalGameRepository()
    val settingsViewModel = SettingsViewModel()
    lateinit var presetRepository: LocalPresetRepository

    fun init(db: BingoDatabase) {
        presetRepository = LocalPresetRepository(db)
    }

    val Factory = viewModelFactory {
        initializer { LobbyViewModel(repository) }
        initializer { BoardSetupViewModel(repository) }
        initializer { settingsViewModel }
        initializer { PresetViewModel(presetRepository) }
    }

    fun gameViewModelFactory(roomId: String, isBot: Boolean = false) = viewModelFactory {
        initializer { GameViewModel(if (isBot) botRepository else repository, roomId) }
    }

    fun resultViewModelFactory(roomId: String, isBot: Boolean = false) = viewModelFactory {
        initializer { ResultViewModel(if (isBot) botRepository else repository, roomId) }
    }
}