package com.mohamedaminelouati.oplayer.media

import com.mohamedaminelouati.oplayer.data.PlaybackSpeedScope

object PlaybackSpeedResolver {

    fun isHistorySource(from: String?, playbackSource: PlaybackSource): Boolean {
        return from == "history" || (from != "folder" && !from.orEmpty().startsWith("playlist") && playbackSource is PlaybackSource.History)
    }

    fun resolvePlaylistId(from: String?, playbackSource: PlaybackSource): Int? {
        return when {
            from?.startsWith("playlist_") == true -> from.removePrefix("playlist_").toIntOrNull() ?: -1
            from == "playlist" -> (playbackSource as? PlaybackSource.Playlist)?.playlistId ?: -1
            playbackSource is PlaybackSource.Playlist -> playbackSource.playlistId
            else -> null
        }
    }

    fun resolveEffectiveSpeed(
        scope: PlaybackSpeedScope,
        globalSpeed: Float,
        isHistory: Boolean,
        historySpeed: Float?,
        playlistId: Int?,
        playlistSpeed: Float?,
        folderSpeed: Float?
    ): Float {
        if (scope == PlaybackSpeedScope.GLOBAL) {
            return globalSpeed
        }
        if (isHistory) {
            return historySpeed ?: globalSpeed
        }
        if (playlistId != null) {
            return playlistSpeed ?: globalSpeed
        }
        return folderSpeed ?: globalSpeed
    }
}
