package com.ishaan.bingo.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.ishaan.bingo.ui.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    private val _confirmCalls = MutableStateFlow(false)
    val confirmCalls = _confirmCalls.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(false)
    val hapticsEnabled = _hapticsEnabled.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setConfirmCalls(enabled: Boolean) {
        _confirmCalls.value = enabled
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
    }
}
