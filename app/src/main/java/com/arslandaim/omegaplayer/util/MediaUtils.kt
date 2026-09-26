package com.arslandaim.omegaplayer.util

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import android.app.RecoverableSecurityException

object MediaUtils {
    private const val ALBUM_ART_BASE_URI = "content://media/external/audio/albumart"

    private val videoExtensions = listOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".3gp", ".m4v", ".flv", ".ts")

    fun isVideoMediaItem(mediaItem: MediaItem?): Boolean {
        if (mediaItem == null) return false
        val uriStr = mediaItem.localConfiguration?.uri?.toString()?.lowercase() ?: ""
        val mimeType = mediaItem.localConfiguration?.mimeType?.lowercase() ?: ""
        if (mimeType.startsWith("video/")) return true
        if (mediaItem.mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO) return true
        return videoExtensions.any { uriStr.endsWith(it) }
    }

    fun isAudioMediaItem(mediaItem: MediaItem?): Boolean {
        if (mediaItem == null) return false
        return !isVideoMediaItem(mediaItem)
    }

    fun safeEncodeUri(uri: String): String {
        var raw = uri
        while ((raw.contains("%3A", ignoreCase = true) || raw.contains("%2F", ignoreCase = true)) && hasOnlyValidEscapes(raw)) {
            raw = java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8.toString())
        }
        return java.net.URLEncoder.encode(raw, java.nio.charset.StandardCharsets.UTF_8.toString())
    }

    fun safeDecodeUri(uri: String): String {
        var raw = uri
        while ((raw.contains("%3A", ignoreCase = true) || raw.contains("%2F", ignoreCase = true)) && hasOnlyValidEscapes(raw)) {
            raw = java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8.toString())
        }
        return raw
    }

    private fun hasOnlyValidEscapes(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            if (value[index] == '%') {
                if (index + 2 >= value.length || value[index + 1].digitToIntOrNull(16) == null || value[index + 2].digitToIntOrNull(16) == null) return false
                index += 2
            }
            index++
        }
        return true
    }

    fun albumArtUri(albumId: Long): Uri = ContentUris.withAppendedId(Uri.parse(ALBUM_ART_BASE_URI), albumId)

    fun formatDuration(durationMs: Long): String {
        if (durationMs == -9223372036854775807L) return "00:00"
        val totalSeconds = Math.abs(durationMs) / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun requestMediaDelete(
        context: Context,
        uris: List<Uri>,
        deleteLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onRequireInternalPopup: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            try {
                for (uri in uris) {
                    context.contentResolver.delete(uri, null, null)
                }
            } catch (e: SecurityException) {
                val recoverableException = e as? RecoverableSecurityException
                if (recoverableException != null) {
                    val intentSender = recoverableException.userAction.actionIntent.intentSender
                    deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                } else {
                    throw e
                }
            }
        } else {
            onRequireInternalPopup()
        }
    }
}
