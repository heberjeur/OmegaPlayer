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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("app_theme")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val HISTORY_PAUSED_KEY = booleanPreferencesKey("history_paused")
    private val EXCLUDED_FOLDERS_KEY = stringSetPreferencesKey("excluded_folders")
    private val SHOW_RECENT_HISTORY_HOME_KEY = booleanPreferencesKey("show_recent_history_home")
    private val SHOW_HISTORY_TAB_KEY = booleanPreferencesKey("show_history_tab")

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
}
