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
    val settingsViewModel = SettingsViewModel()
    lateinit var presetRepository: LocalPresetRepository

    var currentBotRepository: LocalGameRepository? = null

    fun freshBotRepository(): LocalGameRepository {
        val repo = LocalGameRepository()
        currentBotRepository = repo
        return repo
    }

    fun init(db: BingoDatabase) {
        presetRepository = LocalPresetRepository(db)
    }

    // LobbyViewModel as a true singleton — one instance for the entire app lifetime
    val lobbyViewModel: LobbyViewModel by lazy { LobbyViewModel(repository) }

    // Factory for screens that still need viewModel() scoping (Settings, Presets)
    val Factory = viewModelFactory {
        initializer { settingsViewModel }
        initializer { PresetViewModel(presetRepository) }
    }

    fun boardSetupViewModelFactory(roomId: String, isBot: Boolean = false) = viewModelFactory {
        initializer {
            val repo = if (isBot) currentBotRepository ?: LocalGameRepository() else repository
            BoardSetupViewModel(repo)
        }
    }

    fun gameViewModelFactory(roomId: String, isBot: Boolean = false) = viewModelFactory {
        initializer {
            val repo = if (isBot) currentBotRepository ?: LocalGameRepository() else repository
            GameViewModel(repo, roomId)
        }
    }

    fun resultViewModelFactory(roomId: String, isBot: Boolean = false) = viewModelFactory {
        initializer {
            val repo = if (isBot) currentBotRepository ?: LocalGameRepository() else repository
            ResultViewModel(repo, roomId)
        }
    }
}