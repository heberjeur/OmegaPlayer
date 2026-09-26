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

    private val NOTIF_SHOW_PREVIOUS_KEY = booleanPreferencesKey("notif_show_previous")
    private val NOTIF_SHOW_REWIND_KEY = booleanPreferencesKey("notif_show_rewind")
    private val NOTIF_SHOW_FORWARD_KEY = booleanPreferencesKey("notif_show_forward")
    private val NOTIF_SHOW_NEXT_KEY = booleanPreferencesKey("notif_show_next")
    private val NOTIF_SHOW_SPEED_KEY = booleanPreferencesKey("notif_show_speed")
    private val NOTIF_SHOW_STOP_KEY = booleanPreferencesKey("notif_show_stop")
    private val NOTIF_SHOW_CLOSE_KEY = booleanPreferencesKey("notif_show_close")
    private val NOTIF_SHOW_REPEAT_KEY = booleanPreferencesKey("notif_show_repeat")
    private val NOTIF_SHOW_SHUFFLE_KEY = booleanPreferencesKey("notif_show_shuffle")
    private val AUTO_PLAY_NEXT_KEY = booleanPreferencesKey("auto_play_next")
    private val AUTO_PIP_KEY = booleanPreferencesKey("auto_pip")
    private val SHOW_SYSTEM_STATUS_BAR_KEY = booleanPreferencesKey("show_system_status_bar")
    private val FOLDER_FLATTEN_THRESHOLD_KEY = intPreferencesKey("folder_flatten_threshold")
    private val CONTROLS_TIMEOUT_KEY = intPreferencesKey("controls_timeout")
    private val VOLUME_BOOST_ENABLED_KEY = booleanPreferencesKey("volume_boost_enabled")
    private val PLAYER_ORIENTATION_KEY = intPreferencesKey("player_orientation")
    private val UP_NEXT_FULLY_EXPANDED_KEY = booleanPreferencesKey("up_next_fully_expanded")

    val theme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.SYSTEM.name
        AppTheme.values().firstOrNull { it.name == themeName } ?: AppTheme.SYSTEM
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

    val volumeBoostEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOLUME_BOOST_ENABLED_KEY] ?: false
    }

    val showRecentHistoryOnHome: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_RECENT_HISTORY_HOME_KEY] ?: false
    }

    val showHistoryTab: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_HISTORY_TAB_KEY] ?: true
    }

    val speedScope: Flow<PlaybackSpeedScope> = context.dataStore.data.map { preferences ->
        val scopeName = preferences[SPEED_SCOPE_KEY] ?: PlaybackSpeedScope.GLOBAL.name
        PlaybackSpeedScope.values().firstOrNull { it.name == scopeName } ?: PlaybackSpeedScope.GLOBAL
    }

    val globalPlaybackSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_PLAYBACK_SPEED_KEY] ?: preferences[GLOBAL_SPEED_KEY] ?: 1.0f
    }

    val showPlayerClock: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_CLOCK_KEY] ?: false
    }

    val showSystemStatusBar: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_SYSTEM_STATUS_BAR_KEY] ?: true
    }

    val notifShowPrevious: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_SHOW_PREVIOUS_KEY] ?: true }
    val notifShowRewind: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_SHOW_REWIND_KEY] ?: true }
    val notifShowForward: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_SHOW_FORWARD_KEY] ?: true }
    val notifShowNext: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_SHOW_NEXT_KEY] ?: true }
    val notifShowSpeed: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_SHOW_SPEED_KEY] ?: false }
    val notifShowStop: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_SHOW_STOP_KEY] ?: false }
    val notifShowClose: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_SHOW_CLOSE_KEY] ?: false }
    val notifShowRepeat: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_SHOW_REPEAT_KEY] ?: false }
    val notifShowShuffle: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_SHOW_SHUFFLE_KEY] ?: false }

    val autoPlayNext: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_PLAY_NEXT_KEY] ?: true
    }

    val autoPip: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_PIP_KEY] ?: false
    }

    val upNextFullyExpanded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[UP_NEXT_FULLY_EXPANDED_KEY] ?: false
    }

    val playerOrientation: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PLAYER_ORIENTATION_KEY] ?: 2
    }

    val folderFlattenThreshold: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[FOLDER_FLATTEN_THRESHOLD_KEY] ?: 5
    }

    val showPlayerBattery: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_BATTERY_KEY] ?: false
    }

    val showPlayerMediaInfo: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_MEDIA_INFO_KEY] ?: false
    }

    val showPlayerVolume: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_VOLUME_KEY] ?: false
    }

    val showPlayerBrightness: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PLAYER_BRIGHTNESS_KEY] ?: false
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

    suspend fun savePlayerOrientation(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_ORIENTATION_KEY] = mode
        }
    }

    suspend fun saveUpNextFullyExpanded(expanded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[UP_NEXT_FULLY_EXPANDED_KEY] = expanded
        }
    }

    suspend fun saveHistoryPaused(paused: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HISTORY_PAUSED_KEY] = paused
        }
    }

    suspend fun saveVolumeBoostEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_BOOST_ENABLED_KEY] = enabled
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
        preferences[SUBTITLE_TEXT_SIZE_KEY] ?: 18
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

    suspend fun saveSubtitleBgStyle(style: Int) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_BG_STYLE_KEY] = style
        }
    }

    suspend fun saveNotifShowPrevious(show: Boolean) { context.dataStore.edit { it[NOTIF_SHOW_PREVIOUS_KEY] = show } }
    suspend fun saveNotifShowRewind(show: Boolean) { context.dataStore.edit { it[NOTIF_SHOW_REWIND_KEY] = show } }
    suspend fun saveNotifShowForward(show: Boolean) { context.dataStore.edit { it[NOTIF_SHOW_FORWARD_KEY] = show } }
    suspend fun saveNotifShowNext(show: Boolean) { context.dataStore.edit { it[NOTIF_SHOW_NEXT_KEY] = show } }
    suspend fun saveNotifShowSpeed(show: Boolean) { context.dataStore.edit { it[NOTIF_SHOW_SPEED_KEY] = show } }
    suspend fun saveNotifShowStop(show: Boolean) { context.dataStore.edit { it[NOTIF_SHOW_STOP_KEY] = show } }
    suspend fun saveNotifShowClose(show: Boolean) { context.dataStore.edit { it[NOTIF_SHOW_CLOSE_KEY] = show } }
    suspend fun saveNotifShowRepeat(show: Boolean) { context.dataStore.edit { it[NOTIF_SHOW_REPEAT_KEY] = show } }
    suspend fun saveNotifShowShuffle(show: Boolean) { context.dataStore.edit { it[NOTIF_SHOW_SHUFFLE_KEY] = show } }
    suspend fun saveAutoPlayNext(autoPlay: Boolean) { context.dataStore.edit { it[AUTO_PLAY_NEXT_KEY] = autoPlay } }

    suspend fun saveShowSystemStatusBar(show: Boolean) { context.dataStore.edit { it[SHOW_SYSTEM_STATUS_BAR_KEY] = show } }

    suspend fun saveAutoPip(autoPip: Boolean) { context.dataStore.edit { it[AUTO_PIP_KEY] = autoPip } }

    suspend fun setFolderFlattenThreshold(threshold: Int) { context.dataStore.edit { it[FOLDER_FLATTEN_THRESHOLD_KEY] = threshold } }

    val controlsTimeout: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CONTROLS_TIMEOUT_KEY] ?: 3
    }

    suspend fun saveControlsTimeout(timeout: Int) { context.dataStore.edit { it[CONTROLS_TIMEOUT_KEY] = timeout } }
}
