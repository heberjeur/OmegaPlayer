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
        preferences[GLOBAL_SPEED_KEY] ?: 1.0f
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

    fun getFolderPlaybackSpeed(folderName: String): Flow<Float?> = context.dataStore.data.map { preferences ->
        preferences[floatPreferencesKey("folder_speed_$folderName")]
    }

    suspend fun saveFolderPlaybackSpeed(folderName: String, speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[floatPreferencesKey("folder_speed_$folderName")] = speed
        }
    }

    fun getFolderGridView(folderKey: String, defaultGrid: Boolean): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("grid_view_$folderKey")] ?: defaultGrid
    }

    suspend fun saveFolderGridView(folderKey: String, isGrid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("grid_view_$folderKey")] = isGrid
        }
    }

    fun getFolderSortOrder(folderKey: String, defaultSort: String): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[stringPreferencesKey("sort_order_$folderKey")] ?: defaultSort
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
}
