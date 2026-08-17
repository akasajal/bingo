package com.ishaan.bingo.ui.screens.settings.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishaan.bingo.domain.model.PresetBoard
import com.ishaan.bingo.domain.repository.PresetBoardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PresetViewModel(
    private val repository: PresetBoardRepository
) : ViewModel() {

    val presets: StateFlow<List<PresetBoard>> = repository.getPresetBoards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPreset(preset: PresetBoard) {
        viewModelScope.launch {
            repository.addPresetBoard(preset)
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch {
            repository.deletePresetBoard(id)
        }
    }
}
