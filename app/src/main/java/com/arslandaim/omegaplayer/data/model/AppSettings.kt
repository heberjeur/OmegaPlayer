package com.arslandaim.omegaplayer.data.model

import com.arslandaim.omegaplayer.data.AppTheme
import com.arslandaim.omegaplayer.data.PlaybackSpeedScope

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
    val isHistoryPaused: Boolean = false,
    val excludedFolders: Set<String> = emptySet(),
    val showRecentHistoryOnHome: Boolean = false,
    val showHistoryTab: Boolean = true,
    val speedScope: PlaybackSpeedScope = PlaybackSpeedScope.GLOBAL,
    val globalPlaybackSpeed: Float = 1.0f,
    val showPlayerClock: Boolean = false,
    val showPlayerBattery: Boolean = false,
    val showPlayerMediaInfo: Boolean = false,
    val showPlayerVolume: Boolean = false,
    val showPlayerBrightness: Boolean = false,
    val showSystemStatusBar: Boolean = true,
    val notifShowPrevious: Boolean = true,
    val notifShowRewind: Boolean = true,
    val notifShowForward: Boolean = true,
    val notifShowNext: Boolean = true,
    val notifShowSpeed: Boolean = false,
    val notifShowStop: Boolean = false,
    val notifShowClose: Boolean = false,
    val notifShowRepeat: Boolean = false,
    val notifShowShuffle: Boolean = false,
    val autoPlayNext: Boolean = true,
    val autoPip: Boolean = false,
    val folderFlattenThreshold: Int = 5,
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultViewMode: Int = 0,
    val defaultSortOrder: String = "DATE_DESC",
    val subtitleTextSize: Int = 18,
    val subtitleTextColor: Int = -1,
    val subtitleBgStyle: Int = 0,
    val controlsTimeout: Int = 3000,
    val playerOrientation: Int = 0
)
