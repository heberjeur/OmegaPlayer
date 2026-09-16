/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

enum class PlaybackSpeedScope {
    GLOBAL, PER_FOLDER
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("app_theme")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val HISTORY_PAUSED_KEY = booleanPreferencesKey("history_paused")
    private val EXCLUDED_FOLDERS_KEY = stringSetPreferencesKey("excluded_folders")
    private val SHOW_RECENT_HISTORY_HOME_KEY = booleanPreferencesKey("show_recent_history_home")
    private val SHOW_HISTORY_TAB_KEY = booleanPreferencesKey("show_history_tab")
    private val SPEED_SCOPE_KEY = stringPreferencesKey("speed_scope")
    private val GLOBAL_SPEED_KEY = floatPreferencesKey("global_playback_speed")
    private val SHOW_PLAYER_CLOCK_KEY = booleanPreferencesKey("show_player_clock")
    private val SHOW_PLAYER_BATTERY_KEY = booleanPreferencesKey("show_player_battery")
    private val SHOW_PLAYER_MEDIA_INFO_KEY = booleanPreferencesKey("show_player_media_info")
    private val SHOW_PLAYER_VOLUME_KEY = booleanPreferencesKey("show_player_volume")
    private val SHOW_PLAYER_BRIGHTNESS_KEY = booleanPreferencesKey("show_player_brightness")
    private val DEFAULT_PLAYBACK_SPEED_KEY = floatPreferencesKey("default_playback_speed")
    private val DEFAULT_VIEW_MODE_KEY = intPreferencesKey("default_view_mode")
    private val DEFAULT_SORT_ORDER_KEY = stringPreferencesKey("default_sort_order")
    private val SUBTITLE_TEXT_SIZE_KEY = intPreferencesKey("subtitle_text_size")
    private val SUBTITLE_TEXT_COLOR_KEY = intPreferencesKey("subtitle_text_color")
    private val SUBTITLE_BG_STYLE_KEY = intPreferencesKey("subtitle_bg_style")

    val theme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: true
    }

    val isHistoryPaused: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HISTORY_PAUSED_KEY] ?: false
    }

    val excludedFolders: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[EXCLUDED_FOLDERS_KEY] ?: emptySet()
    }

    val showRecentHistoryOnHome: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_RECENT_HISTORY_HOME_KEY] ?: true
    }

    val showHistoryTab: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_HISTORY_TAB_KEY] ?: false
    }

    val speedScope: Flow<PlaybackSpeedScope> = context.dataStore.data.map { preferences ->
        val scopeName = preferences[SPEED_SCOPE_KEY] ?: PlaybackSpeedScope.GLOBAL.name
        try {
            PlaybackSpeedScope.valueOf(scopeName)
        } catch (e: Exception) {
            PlaybackSpeedScope.GLOBAL
        }
    }

    val globalPlaybackSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_PLAYBACK_SPEED_KEY] ?: preferences[GLOBAL_SPEED_KEY] ?: 1.0f
    }

    val showPlayerClock: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_CLOCK_KEY] ?: true
    }

    val showPlayerBattery: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_BATTERY_KEY] ?: true
    }

    val showPlayerMediaInfo: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_MEDIA_INFO_KEY] ?: true
    }

    val showPlayerVolume: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_VOLUME_KEY] ?: true
    }

    val showPlayerBrightness: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_BRIGHTNESS_KEY] ?: true
    }

    suspend fun saveTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    suspend fun saveDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    suspend fun saveHistoryPaused(paused: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HISTORY_PAUSED_KEY] = paused
        }
    }

    suspend fun addExcludedFolder(folderName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[EXCLUDED_FOLDERS_KEY] ?: emptySet()
            preferences[EXCLUDED_FOLDERS_KEY] = current + folderName
        }
    }

    suspend fun removeExcludedFolder(folderName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[EXCLUDED_FOLDERS_KEY] ?: emptySet()
            preferences[EXCLUDED_FOLDERS_KEY] = current - folderName
        }
    }

    suspend fun clearExcludedFolders() {
        context.dataStore.edit { preferences ->
            preferences.remove(EXCLUDED_FOLDERS_KEY)
        }
    }

    suspend fun saveShowRecentHistoryOnHome(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_RECENT_HISTORY_HOME_KEY] = show
        }
    }

    suspend fun saveShowHistoryTab(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_HISTORY_TAB_KEY] = show
        }
    }

    suspend fun saveSpeedScope(scope: PlaybackSpeedScope) {
        context.dataStore.edit { preferences ->
            preferences[SPEED_SCOPE_KEY] = scope.name
        }
    }

    suspend fun saveGlobalPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[GLOBAL_SPEED_KEY] = speed
        }
    }

    fun getFolderSpeed(folderName: String): Flow<Float?> = context.dataStore.data.map { preferences ->
        preferences[floatPreferencesKey("folder_speed_$folderName")] ?: preferences[DEFAULT_PLAYBACK_SPEED_KEY] ?: preferences[GLOBAL_SPEED_KEY] ?: 1.0f
    }

    suspend fun saveFolderPlaybackSpeed(folderName: String, speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[floatPreferencesKey("folder_speed_$folderName")] = speed
        }
    }

    fun getFolderViewMode(folderKey: String, defaultMode: Int? = null): Flow<Int> = context.dataStore.data.map { preferences ->
        val mode = preferences[intPreferencesKey("view_mode_$folderKey")]
        if (mode != null) {
            mode
        } else {
            val legacy = preferences[booleanPreferencesKey("grid_view_$folderKey")]
            if (legacy != null) (if (legacy) 1 else 0)
            else preferences[DEFAULT_VIEW_MODE_KEY] ?: defaultMode ?: 0
        }
    }

    suspend fun saveFolderViewMode(folderKey: String, mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[intPreferencesKey("view_mode_$folderKey")] = mode
            preferences[booleanPreferencesKey("grid_view_$folderKey")] = (mode != 0)
        }
    }

    fun getFolderGridView(folderKey: String, defaultGrid: Boolean): Flow<Boolean> = context.dataStore.data.map { preferences ->
        val mode = preferences[intPreferencesKey("view_mode_$folderKey")]
        if (mode != null) mode != 0
        else preferences[booleanPreferencesKey("grid_view_$folderKey")] ?: defaultGrid
    }

    suspend fun saveFolderGridView(folderKey: String, isGrid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("grid_view_$folderKey")] = isGrid
            preferences[intPreferencesKey("view_mode_$folderKey")] = if (isGrid) 1 else 0
        }
    }

    fun getFolderSortOrder(folderKey: String, defaultSort: String? = null): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[stringPreferencesKey("sort_order_$folderKey")] ?: preferences[DEFAULT_SORT_ORDER_KEY] ?: defaultSort ?: "DATE_DESC"
    }

    suspend fun saveFolderSortOrder(folderKey: String, sortOrder: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("sort_order_$folderKey")] = sortOrder
        }
    }

    suspend fun saveShowPlayerClock(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PLAYER_CLOCK_KEY] = show
        }
    }

    suspend fun saveShowPlayerBattery(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PLAYER_BATTERY_KEY] = show
        }
    }

    suspend fun saveShowPlayerMediaInfo(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PLAYER_MEDIA_INFO_KEY] = show
        }
    }

    suspend fun saveShowPlayerVolume(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PLAYER_VOLUME_KEY] = show
        }
    }

    suspend fun saveShowPlayerBrightness(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PLAYER_BRIGHTNESS_KEY] = show
        }
    }

    val defaultPlaybackSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_PLAYBACK_SPEED_KEY] ?: 1.0f
    }

    suspend fun saveDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PLAYBACK_SPEED_KEY] = speed
        }
    }

    val defaultViewMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_VIEW_MODE_KEY] ?: 0
    }

    suspend fun saveDefaultViewMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_VIEW_MODE_KEY] = mode
        }
    }

    val defaultSortOrder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_SORT_ORDER_KEY] ?: "DATE_DESC"
    }

    suspend fun saveDefaultSortOrder(sortOrder: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_SORT_ORDER_KEY] = sortOrder
        }
    }

    val subtitleTextSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_TEXT_SIZE_KEY] ?: 16
    }

    suspend fun saveSubtitleTextSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_TEXT_SIZE_KEY] = size
        }
    }

    val subtitleTextColor: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_TEXT_COLOR_KEY] ?: 0
    }

    suspend fun saveSubtitleTextColor(colorIndex: Int) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_TEXT_COLOR_KEY] = colorIndex
        }
    }

    val subtitleBgStyle: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_BG_STYLE_KEY] ?: 1
    }

    suspend fun saveSubtitleBgStyle(bgIndex: Int) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_BG_STYLE_KEY] = bgIndex
        }
    }
}
