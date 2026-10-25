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

enum class BrightnessScope {
    NONE, GLOBAL, PER_FOLDER
}

enum class TextTruncationMode {
    TWO_LINES, THREE_LINES, FULL_NAME
}

enum class SecurityLockType {
    NONE, PIN, PATTERN
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
    private val HISTORY_SPEED_KEY = floatPreferencesKey("history_playback_speed")
    private val SHOW_PLAYER_CLOCK_KEY = booleanPreferencesKey("show_player_clock")
    private val SHOW_PLAYER_BATTERY_KEY = booleanPreferencesKey("show_player_battery")
    private val SHOW_PLAYER_MEDIA_INFO_KEY = booleanPreferencesKey("show_player_media_info")
    private val SHOW_PLAYER_VOLUME_KEY = booleanPreferencesKey("show_player_volume")
    private val SHOW_PLAYER_BRIGHTNESS_KEY = booleanPreferencesKey("show_player_brightness")
    private val DEFAULT_PLAYBACK_SPEED_KEY = floatPreferencesKey("default_playback_speed")
    private val DEFAULT_VIEW_MODE_KEY = intPreferencesKey("default_view_mode")
    private val DEFAULT_SORT_ORDER_KEY = stringPreferencesKey("default_sort_order")
    private val DEFAULT_SEARCH_SCOPE_KEY = stringPreferencesKey("default_search_scope")
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
    private val AUDIO_NORMALIZATION_ENABLED_KEY = booleanPreferencesKey("audio_normalization_enabled")
    private val AUDIO_NORMALIZATION_DB_KEY = floatPreferencesKey("audio_normalization_db")
    private val SKIP_SILENCE_ENABLED_KEY = booleanPreferencesKey("skip_silence_enabled")
    private val PLAYER_ORIENTATION_KEY = intPreferencesKey("player_orientation")
    private val UP_NEXT_FULLY_EXPANDED_KEY = booleanPreferencesKey("up_next_fully_expanded")
    private val SEEK_INCREMENT_KEY = intPreferencesKey("seek_increment")
    private val TEXT_TRUNCATION_MODE_KEY = stringPreferencesKey("text_truncation_mode")
    private val SAVE_POSITION_ON_QUIT_KEY = booleanPreferencesKey("save_position_on_quit")
    private val CLOSE_AFTER_QUEUE_ENDS_KEY = booleanPreferencesKey("close_after_queue_ends")
    private val BRIGHTNESS_SCOPE_KEY = stringPreferencesKey("brightness_scope")
    private val GLOBAL_BRIGHTNESS_KEY = floatPreferencesKey("global_brightness")
    private val KEEP_SCREEN_ON_WHEN_PAUSED_KEY = booleanPreferencesKey("keep_screen_on_when_paused")
    private val GESTURE_VOLUME_ENABLED_KEY = booleanPreferencesKey("gesture_volume_enabled")
    private val GESTURE_BRIGHTNESS_ENABLED_KEY = booleanPreferencesKey("gesture_brightness_enabled")
    private val GESTURE_PINCH_TO_ZOOM_ENABLED_KEY = booleanPreferencesKey("gesture_pinch_to_zoom_enabled")
    private val GESTURE_SEEK_FORWARD_ENABLED_KEY = booleanPreferencesKey("gesture_seek_forward_enabled")
    private val GESTURE_SEEK_BACKWARD_ENABLED_KEY = booleanPreferencesKey("gesture_seek_backward_enabled")
    private val GESTURE_PLAY_PAUSE_ENABLED_KEY = booleanPreferencesKey("gesture_play_pause_enabled")
    private val GESTURE_LONG_PRESS_SPEED_ENABLED_KEY = booleanPreferencesKey("gesture_long_press_speed_enabled")
    private val GESTURE_TAP_CONTROLS_ENABLED_KEY = booleanPreferencesKey("gesture_tap_controls_enabled")
    private val SECURITY_LOCK_TYPE_KEY = stringPreferencesKey("security_lock_type")
    private val SECURITY_PIN_HASH_KEY = stringPreferencesKey("security_pin_hash")
    private val SECURITY_PATTERN_HASH_KEY = stringPreferencesKey("security_pattern_hash")
    private val SECURITY_APP_LOCK_KEY = booleanPreferencesKey("security_app_lock")
    private val SECURITY_LOCKED_FOLDERS_KEY = stringSetPreferencesKey("security_locked_folders")
    private val SECURITY_LOCKED_MEDIA_KEY = stringSetPreferencesKey("security_locked_media")
    private val DARK_PRIMARY_COLOR_KEY = intPreferencesKey("dark_primary_color")
    private val DARK_SECONDARY_COLOR_KEY = intPreferencesKey("dark_secondary_color")
    private val LIGHT_PRIMARY_COLOR_KEY = intPreferencesKey("light_primary_color")
    private val LIGHT_SECONDARY_COLOR_KEY = intPreferencesKey("light_secondary_color")

    val darkPrimaryColor: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DARK_PRIMARY_COLOR_KEY] ?: com.arslandaim.omegaplayer.ui.theme.DefaultDarkPrimaryInt
    }

    val darkSecondaryColor: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DARK_SECONDARY_COLOR_KEY] ?: com.arslandaim.omegaplayer.ui.theme.DefaultDarkSecondaryInt
    }

    val lightPrimaryColor: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LIGHT_PRIMARY_COLOR_KEY] ?: com.arslandaim.omegaplayer.ui.theme.DefaultLightPrimaryInt
    }

    val lightSecondaryColor: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LIGHT_SECONDARY_COLOR_KEY] ?: com.arslandaim.omegaplayer.ui.theme.DefaultLightSecondaryInt
    }

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

    val audioNormalizationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUDIO_NORMALIZATION_ENABLED_KEY] ?: false
    }

    val audioNormalizationDb: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[AUDIO_NORMALIZATION_DB_KEY] ?: 6.0f
    }

    val skipSilenceEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SKIP_SILENCE_ENABLED_KEY] ?: false
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

    val brightnessScope: Flow<BrightnessScope> = context.dataStore.data.map { preferences ->
        val scopeName = preferences[BRIGHTNESS_SCOPE_KEY] ?: BrightnessScope.NONE.name
        BrightnessScope.values().firstOrNull { it.name == scopeName } ?: BrightnessScope.NONE
    }

    val globalBrightness: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[GLOBAL_BRIGHTNESS_KEY] ?: -1f
    }

    val keepScreenOnWhenPaused: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEEP_SCREEN_ON_WHEN_PAUSED_KEY] ?: false
    }

    val globalPlaybackSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[GLOBAL_SPEED_KEY] ?: preferences[DEFAULT_PLAYBACK_SPEED_KEY] ?: 1.0f
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
        preferences[UP_NEXT_FULLY_EXPANDED_KEY] ?: true
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

    suspend fun saveDarkPrimaryColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[DARK_PRIMARY_COLOR_KEY] = color
        }
    }

    suspend fun saveDarkSecondaryColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[DARK_SECONDARY_COLOR_KEY] = color
        }
    }

    suspend fun saveLightPrimaryColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[LIGHT_PRIMARY_COLOR_KEY] = color
        }
    }

    suspend fun saveLightSecondaryColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[LIGHT_SECONDARY_COLOR_KEY] = color
        }
    }

    suspend fun resetThemeColors() {
        context.dataStore.edit { preferences ->
            preferences.remove(DARK_PRIMARY_COLOR_KEY)
            preferences.remove(DARK_SECONDARY_COLOR_KEY)
            preferences.remove(LIGHT_PRIMARY_COLOR_KEY)
            preferences.remove(LIGHT_SECONDARY_COLOR_KEY)
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

    suspend fun saveAudioNormalizationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_NORMALIZATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveAudioNormalizationDb(db: Float) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_NORMALIZATION_DB_KEY] = db
        }
    }

    suspend fun saveSkipSilenceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SKIP_SILENCE_ENABLED_KEY] = enabled
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
            preferences[DEFAULT_PLAYBACK_SPEED_KEY] = speed
        }
    }

    fun getFolderSpeed(folderName: String): Flow<Float?> = context.dataStore.data.map { preferences ->
        preferences[floatPreferencesKey("folder_speed_$folderName")] ?: preferences[GLOBAL_SPEED_KEY] ?: preferences[DEFAULT_PLAYBACK_SPEED_KEY] ?: 1.0f
    }

    suspend fun saveFolderPlaybackSpeed(folderName: String, speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[floatPreferencesKey("folder_speed_$folderName")] = speed
        }
    }

    fun getHistorySpeed(): Flow<Float?> = context.dataStore.data.map { preferences ->
        preferences[HISTORY_SPEED_KEY] ?: preferences[GLOBAL_SPEED_KEY] ?: preferences[DEFAULT_PLAYBACK_SPEED_KEY] ?: 1.0f
    }

    suspend fun saveHistoryPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[HISTORY_SPEED_KEY] = speed
        }
    }

    fun getPlaylistSpeed(playlistId: Int): Flow<Float?> = context.dataStore.data.map { preferences ->
        val key = if (playlistId > 0) "playlist_speed_$playlistId" else "playlist_speed_default"
        preferences[floatPreferencesKey(key)] ?: preferences[GLOBAL_SPEED_KEY] ?: preferences[DEFAULT_PLAYBACK_SPEED_KEY] ?: 1.0f
    }

    suspend fun savePlaylistPlaybackSpeed(playlistId: Int, speed: Float) {
        val key = if (playlistId > 0) "playlist_speed_$playlistId" else "playlist_speed_default"
        context.dataStore.edit { preferences ->
            preferences[floatPreferencesKey(key)] = speed
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
            preferences[GLOBAL_SPEED_KEY] = speed
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

    val defaultSearchScope: Flow<SearchScope> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_SEARCH_SCOPE_KEY]?.let { name ->
            runCatching { SearchScope.valueOf(name) }.getOrNull()
        } ?: SearchScope.CURRENT_TYPE
    }

    suspend fun saveDefaultSearchScope(scope: SearchScope) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_SEARCH_SCOPE_KEY] = scope.name
        }
    }

    suspend fun resetDefaultPreferences(clearFolderCustomizations: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PLAYBACK_SPEED_KEY] = 1.0f
            preferences[GLOBAL_SPEED_KEY] = 1.0f
            preferences[DEFAULT_VIEW_MODE_KEY] = 0
            preferences[DEFAULT_SORT_ORDER_KEY] = MediaSortOrder.DATE_DESC.name
            preferences[DEFAULT_SEARCH_SCOPE_KEY] = SearchScope.CURRENT_TYPE.name

            if (clearFolderCustomizations) {
                val keysToRemove = preferences.asMap().keys.filter { key ->
                    key.name.startsWith("folder_speed_") ||
                    key.name.startsWith("playlist_speed_") ||
                    key.name == "history_speed" ||
                    key.name.startsWith("view_mode_") ||
                    key.name.startsWith("grid_view_") ||
                    key.name.startsWith("sort_order_") ||
                    key.name.startsWith("folder_brightness_")
                }
                keysToRemove.forEach { key ->
                    preferences.remove(key)
                }
            }
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

    val seekIncrement: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SEEK_INCREMENT_KEY] ?: 10
    }

    suspend fun saveSeekIncrement(increment: Int) { context.dataStore.edit { it[SEEK_INCREMENT_KEY] = increment } }

    val textTruncationMode: Flow<TextTruncationMode> = context.dataStore.data.map { preferences ->
        val modeName = preferences[TEXT_TRUNCATION_MODE_KEY] ?: TextTruncationMode.TWO_LINES.name
        TextTruncationMode.values().firstOrNull { it.name == modeName } ?: TextTruncationMode.TWO_LINES
    }

    suspend fun saveTextTruncationMode(mode: TextTruncationMode) {
        context.dataStore.edit { preferences ->
            preferences[TEXT_TRUNCATION_MODE_KEY] = mode.name
        }
    }

    val savePositionOnQuit: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SAVE_POSITION_ON_QUIT_KEY] ?: true
    }

    suspend fun saveSavePositionOnQuit(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SAVE_POSITION_ON_QUIT_KEY] = enabled
        }
    }

    val closeAfterQueueEnds: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CLOSE_AFTER_QUEUE_ENDS_KEY] ?: false
    }

    suspend fun saveCloseAfterQueueEnds(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLOSE_AFTER_QUEUE_ENDS_KEY] = enabled
        }
    }

    suspend fun saveBrightnessScope(scope: BrightnessScope) {
        context.dataStore.edit { preferences ->
            preferences[BRIGHTNESS_SCOPE_KEY] = scope.name
        }
    }

    suspend fun saveGlobalBrightness(brightness: Float) {
        context.dataStore.edit { preferences ->
            preferences[GLOBAL_BRIGHTNESS_KEY] = brightness
        }
    }

    fun getFolderBrightness(folderName: String): Flow<Float?> = context.dataStore.data.map { preferences ->
        preferences[floatPreferencesKey("folder_brightness_$folderName")] ?: preferences[GLOBAL_BRIGHTNESS_KEY]
    }

    suspend fun saveFolderBrightness(folderName: String, brightness: Float) {
        context.dataStore.edit { preferences ->
            preferences[floatPreferencesKey("folder_brightness_$folderName")] = brightness
        }
    }

    suspend fun saveKeepScreenOnWhenPaused(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON_WHEN_PAUSED_KEY] = enabled
        }
    }

    val gestureVolumeEnabled: Flow<Boolean> = context.dataStore.data.map { it[GESTURE_VOLUME_ENABLED_KEY] ?: true }
    suspend fun saveGestureVolumeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_VOLUME_ENABLED_KEY] = enabled }
    }

    val gestureBrightnessEnabled: Flow<Boolean> = context.dataStore.data.map { it[GESTURE_BRIGHTNESS_ENABLED_KEY] ?: true }
    suspend fun saveGestureBrightnessEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_BRIGHTNESS_ENABLED_KEY] = enabled }
    }

    val gesturePinchToZoomEnabled: Flow<Boolean> = context.dataStore.data.map { it[GESTURE_PINCH_TO_ZOOM_ENABLED_KEY] ?: true }
    suspend fun saveGesturePinchToZoomEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_PINCH_TO_ZOOM_ENABLED_KEY] = enabled }
    }

    val gestureSeekForwardEnabled: Flow<Boolean> = context.dataStore.data.map { it[GESTURE_SEEK_FORWARD_ENABLED_KEY] ?: true }
    suspend fun saveGestureSeekForwardEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_SEEK_FORWARD_ENABLED_KEY] = enabled }
    }

    val gestureSeekBackwardEnabled: Flow<Boolean> = context.dataStore.data.map { it[GESTURE_SEEK_BACKWARD_ENABLED_KEY] ?: true }
    suspend fun saveGestureSeekBackwardEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_SEEK_BACKWARD_ENABLED_KEY] = enabled }
    }

    val gesturePlayPauseEnabled: Flow<Boolean> = context.dataStore.data.map { it[GESTURE_PLAY_PAUSE_ENABLED_KEY] ?: true }
    suspend fun saveGesturePlayPauseEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_PLAY_PAUSE_ENABLED_KEY] = enabled }
    }

    val gestureLongPressSpeedEnabled: Flow<Boolean> = context.dataStore.data.map { it[GESTURE_LONG_PRESS_SPEED_ENABLED_KEY] ?: true }
    suspend fun saveGestureLongPressSpeedEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_LONG_PRESS_SPEED_ENABLED_KEY] = enabled }
    }

    val gestureTapControlsEnabled: Flow<Boolean> = context.dataStore.data.map { it[GESTURE_TAP_CONTROLS_ENABLED_KEY] ?: true }
    suspend fun saveGestureTapControlsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_TAP_CONTROLS_ENABLED_KEY] = enabled }
    }

    val securityLockType: Flow<SecurityLockType> = context.dataStore.data.map { preferences ->
        val typeName = preferences[SECURITY_LOCK_TYPE_KEY] ?: SecurityLockType.NONE.name
        SecurityLockType.values().firstOrNull { it.name == typeName } ?: SecurityLockType.NONE
    }

    val securityPinHash: Flow<String> = context.dataStore.data.map { it[SECURITY_PIN_HASH_KEY] ?: "" }
    val securityPatternHash: Flow<String> = context.dataStore.data.map { it[SECURITY_PATTERN_HASH_KEY] ?: "" }
    val securityAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[SECURITY_APP_LOCK_KEY] ?: false }
    val securityLockedFolders: Flow<Set<String>> = context.dataStore.data.map { it[SECURITY_LOCKED_FOLDERS_KEY] ?: emptySet() }
    val securityLockedMedia: Flow<Set<String>> = context.dataStore.data.map { it[SECURITY_LOCKED_MEDIA_KEY] ?: emptySet() }

    suspend fun saveSecurityLockType(type: SecurityLockType) {
        context.dataStore.edit { it[SECURITY_LOCK_TYPE_KEY] = type.name }
    }

    suspend fun saveSecurityPinHash(hash: String) {
        context.dataStore.edit { it[SECURITY_PIN_HASH_KEY] = hash }
    }

    suspend fun saveSecurityPatternHash(hash: String) {
        context.dataStore.edit { it[SECURITY_PATTERN_HASH_KEY] = hash }
    }

    suspend fun saveSecurityAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SECURITY_APP_LOCK_KEY] = enabled }
    }

    suspend fun toggleFolderLock(path: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SECURITY_LOCKED_FOLDERS_KEY] ?: emptySet()
            prefs[SECURITY_LOCKED_FOLDERS_KEY] = if (current.contains(path)) current - path else current + path
        }
    }

    suspend fun unlockFolder(path: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SECURITY_LOCKED_FOLDERS_KEY] ?: emptySet()
            prefs[SECURITY_LOCKED_FOLDERS_KEY] = current - path
        }
    }

    suspend fun toggleMediaLock(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SECURITY_LOCKED_MEDIA_KEY] ?: emptySet()
            prefs[SECURITY_LOCKED_MEDIA_KEY] = if (current.contains(uri)) current - uri else current + uri
        }
    }

    suspend fun unlockMedia(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SECURITY_LOCKED_MEDIA_KEY] ?: emptySet()
            prefs[SECURITY_LOCKED_MEDIA_KEY] = current - uri
        }
    }

    suspend fun clearAllLocks() {
        context.dataStore.edit { prefs ->
            prefs[SECURITY_LOCKED_FOLDERS_KEY] = emptySet()
            prefs[SECURITY_LOCKED_MEDIA_KEY] = emptySet()
        }
    }
}

