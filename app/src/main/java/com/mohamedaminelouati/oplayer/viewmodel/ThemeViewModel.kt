package com.mohamedaminelouati.oplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedaminelouati.oplayer.data.AppTheme
import com.mohamedaminelouati.oplayer.data.TextTruncationMode
import com.mohamedaminelouati.oplayer.data.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val theme: StateFlow<AppTheme> = themePreferences.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppTheme.SYSTEM
    )

    val dynamicColor: StateFlow<Boolean> = themePreferences.dynamicColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val darkPrimaryColor: StateFlow<Int> = themePreferences.darkPrimaryColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.mohamedaminelouati.oplayer.ui.theme.DefaultDarkPrimaryInt
    )

    val darkSecondaryColor: StateFlow<Int> = themePreferences.darkSecondaryColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.mohamedaminelouati.oplayer.ui.theme.DefaultDarkSecondaryInt
    )

    val lightPrimaryColor: StateFlow<Int> = themePreferences.lightPrimaryColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.mohamedaminelouati.oplayer.ui.theme.DefaultLightPrimaryInt
    )

    val lightSecondaryColor: StateFlow<Int> = themePreferences.lightSecondaryColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.mohamedaminelouati.oplayer.ui.theme.DefaultLightSecondaryInt
    )

    val folderFlattenThreshold: StateFlow<Int> = themePreferences.folderFlattenThreshold.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 5
    )

    val controlsTimeout: StateFlow<Int> = themePreferences.controlsTimeout.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 3
    )

    val seekIncrement: StateFlow<Int> = themePreferences.seekIncrement.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 10
    )

    val playerOrientation: StateFlow<Int> = themePreferences.playerOrientation.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = 0
    )

    val upNextFullyExpanded: StateFlow<Boolean> = themePreferences.upNextFullyExpanded.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = true
    )

    val textTruncationMode: StateFlow<TextTruncationMode> = themePreferences.textTruncationMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TextTruncationMode.TWO_LINES
    )

    val savePositionOnQuit: StateFlow<Boolean> = themePreferences.savePositionOnQuit.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themePreferences.saveTheme(theme)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveDynamicColor(enabled)
        }
    }

    fun setFolderFlattenThreshold(threshold: Int) {
        viewModelScope.launch {
            themePreferences.setFolderFlattenThreshold(threshold)
        }
    }

    fun setControlsTimeout(timeout: Int) {
        viewModelScope.launch {
            themePreferences.saveControlsTimeout(timeout)
        }
    }

    fun setSeekIncrement(increment: Int) {
        viewModelScope.launch {
            themePreferences.saveSeekIncrement(increment)
        }
    }

    fun setPlayerOrientation(mode: Int) {
        viewModelScope.launch {
            themePreferences.savePlayerOrientation(mode)
        }
    }

    fun setUpNextFullyExpanded(expanded: Boolean) {
        viewModelScope.launch {
            themePreferences.saveUpNextFullyExpanded(expanded)
        }
    }

    fun setTextTruncationMode(mode: TextTruncationMode) {
        viewModelScope.launch {
            themePreferences.saveTextTruncationMode(mode)
        }
    }

    fun setDarkPrimaryColor(color: Int) {
        viewModelScope.launch {
            themePreferences.saveDarkPrimaryColor(color)
        }
    }

    fun setDarkSecondaryColor(color: Int) {
        viewModelScope.launch {
            themePreferences.saveDarkSecondaryColor(color)
        }
    }

    fun setLightPrimaryColor(color: Int) {
        viewModelScope.launch {
            themePreferences.saveLightPrimaryColor(color)
        }
    }

    fun setLightSecondaryColor(color: Int) {
        viewModelScope.launch {
            themePreferences.saveLightSecondaryColor(color)
        }
    }

    fun resetThemeColors() {
        viewModelScope.launch {
            themePreferences.resetThemeColors()
        }
    }
}
