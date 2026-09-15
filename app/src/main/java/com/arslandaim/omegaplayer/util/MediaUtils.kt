package com.arslandaim.omegaplayer.util

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

object MediaUtils {
    private val videoExtensions = listOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".3gp", ".m4v", ".flv", ".ts")

    fun isVideoMediaItem(mediaItem: MediaItem?): Boolean {
        if (mediaItem == null) return false
        val uriStr = mediaItem.localConfiguration?.uri?.toString()?.lowercase() ?: ""
        val mimeType = mediaItem.localConfiguration?.mimeType?.lowercase() ?: ""
        if (mimeType.startsWith("video/")) return true
        if (mediaItem.mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO) return true
        if (uriStr.contains("video")) return true
        return videoExtensions.any { uriStr.contains(it) }
    }

    fun isAudioMediaItem(mediaItem: MediaItem?): Boolean {
        if (mediaItem == null) return false
        return !isVideoMediaItem(mediaItem)
    }
}
