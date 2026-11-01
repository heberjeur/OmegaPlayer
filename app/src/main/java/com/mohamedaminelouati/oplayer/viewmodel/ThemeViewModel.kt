package com.mohamedaminelouati.oplayer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedaminelouati.oplayer.data.AppIconStyle
import com.mohamedaminelouati.oplayer.data.AppTheme
import com.mohamedaminelouati.oplayer.data.TextTruncationMode
import com.mohamedaminelouati.oplayer.data.ThemePreferences
import com.mohamedaminelouati.oplayer.util.AppIconManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    val showHistoryDate: StateFlow<Boolean> = themePreferences.showHistoryDate.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val appIconPrimaryColor: StateFlow<Int> = themePreferences.appIconPrimaryColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.mohamedaminelouati.oplayer.ui.theme.DefaultDarkPrimaryInt
    )

    val appIconSecondaryColor: StateFlow<Int> = themePreferences.appIconSecondaryColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.mohamedaminelouati.oplayer.ui.theme.DefaultDarkSecondaryInt
    )

    val appIconStyle: StateFlow<AppIconStyle> = themePreferences.appIconStyle
        .map { AppIconStyle.fromId(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppIconStyle.DEFAULT
        )

    val appLanguage: StateFlow<String> = themePreferences.appLanguage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val useIndianNumerals: StateFlow<Boolean> = themePreferences.useIndianNumerals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setAppLanguage(language: String) {
        viewModelScope.launch {
            themePreferences.saveAppLanguage(language)
        }
    }

    fun setUseIndianNumerals(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveUseIndianNumerals(enabled)
        }
    }

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

    fun setAppIconStyle(context: Context, style: AppIconStyle) {
        viewModelScope.launch {
            themePreferences.saveAppIconStyle(style.id)
            AppIconManager.setAppIcon(context, style)
        }
    }

    fun saveAppIconColors(context: Context, primaryColor: Int, secondaryColor: Int) {
        viewModelScope.launch {
            themePreferences.saveAppIconPrimaryColor(primaryColor)
            themePreferences.saveAppIconSecondaryColor(secondaryColor)
            AppIconManager.applyIconForColors(context, primaryColor, secondaryColor)
        }
    }

    fun resetAppIconColors(context: Context) {
        viewModelScope.launch {
            themePreferences.resetAppIconColors()
            AppIconManager.setAppIcon(context, AppIconStyle.DEFAULT)
        }
    }
}
