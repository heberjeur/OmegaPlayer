package com.arslandaim.omegaplayer.util

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File

data class PendingMediaRename(
    val uri: Uri,
    val newDisplayName: String,
    val oldPath: String = ""
)

data class PendingFolderRename(
    val folderPath: String,
    val newFolderName: String,
    val items: List<Pair<Uri, String>>,
    val oldPath: String,
    val newPath: String
)

object MediaUtils {


    private const val ALBUM_ART_BASE_URI = "content://media/external/audio/albumart"

    fun isVideoMediaItem(mediaItem: MediaItem?): Boolean {
        if (mediaItem == null) return false
        
        val mimeType = mediaItem.localConfiguration?.mimeType?.lowercase()
        if (mimeType?.startsWith("video/") == true) return true
        
        if (mediaItem.mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO) return true
        
        val uriStr = mediaItem.localConfiguration?.uri?.toString() ?: return false
        var extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uriStr)
        if (extension.isNullOrEmpty()) {
            extension = uriStr.substringAfterLast('.', "").substringBefore('?')
        }
        
        if (extension.isNotEmpty()) {
            val guessedMimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            if (guessedMimeType?.startsWith("video/") == true) return true
        }
        
        return false
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

    fun shareMedia(context: Context, uris: List<Uri>, mimeType: String = "*/*") {
        if (uris.isEmpty()) return
        val intent = if (uris.size == 1) {
            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, uris.first())
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, java.util.ArrayList(uris))
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = android.content.Intent.createChooser(intent, context.getString(com.arslandaim.omegaplayer.R.string.action_share))
        context.startActivity(chooser)
    }

    fun appendExtensionIfNeeded(originalName: String, newName: String): String {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return originalName
        val lastDot = originalName.lastIndexOf('.')
        if (lastDot > 0 && lastDot < originalName.length - 1) {
            val extension = originalName.substring(lastDot)
            if (!trimmed.endsWith(extension, ignoreCase = true)) {
                return "$trimmed$extension"
            }
        }
        return trimmed
    }

    fun renameMedia(
        context: Context,
        uri: Uri,
        currentName: String,
        newNameInput: String,
        oldPath: String,
        renameLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onComplete: (Boolean) -> Unit
    ): PendingMediaRename? {
        val trimmed = newNameInput.trim()
        if (trimmed.isEmpty()) {
            onComplete(false)
            return null
        }
        val targetName = appendExtensionIfNeeded(currentName, trimmed)
        if (targetName == currentName) {
            onComplete(true)
            return null
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && oldPath.isNotEmpty()) {
            val oldFile = File(oldPath)
            val newFile = File(oldFile.parentFile, targetName)
            if (oldFile.renameTo(newFile)) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetName)
                }
                context.contentResolver.update(uri, values, null, null)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(oldFile.absolutePath, newFile.absolutePath),
                    null,
                    null
                )
                onComplete(true)
                return null
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, targetName)
        }

        try {
            val rows = context.contentResolver.update(uri, values, null, null)
            if (rows > 0) {
                if (oldPath.isNotEmpty()) {
                    val oldFile = File(oldPath)
                    val newFile = File(oldFile.parentFile, targetName)
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(oldFile.absolutePath, newFile.absolutePath),
                        null,
                        null
                    )
                }
                onComplete(true)
                return null
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                val pending = PendingMediaRename(uri, targetName, oldPath)
                renameLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                return pending
            }
            val recoverableException = e as? RecoverableSecurityException
            if (recoverableException != null) {
                val pending = PendingMediaRename(uri, targetName, oldPath)
                renameLauncher.launch(IntentSenderRequest.Builder(recoverableException.userAction.actionIntent.intentSender).build())
                return pending
            }
            throw e
        }

        onComplete(false)
        return null
    }

    fun completePendingRename(
        context: Context,
        pending: PendingMediaRename
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, pending.newDisplayName)
        }
        val rows = context.contentResolver.update(pending.uri, values, null, null)
        if (pending.oldPath.isNotEmpty()) {
            val oldFile = File(pending.oldPath)
            val newFile = File(oldFile.parentFile, pending.newDisplayName)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(oldFile.absolutePath, newFile.absolutePath),
                null,
                null
            )
        }
        return rows > 0
    }

    fun renameFolder(
        context: Context,
        folderPath: String,
        newFolderName: String,
        mediaItems: List<Pair<Uri, String>>,
        renameLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onComplete: (Boolean) -> Unit
    ): PendingFolderRename? {
        val trimmedName = newFolderName.trim()
        val oldDir = File(folderPath)
        val parentDir = oldDir.parentFile ?: run {
            onComplete(false)
            return null
        }
        if (trimmedName.isEmpty() || oldDir.name == trimmedName) {
            onComplete(true)
            return null
        }
        val newDir = File(parentDir, trimmedName)

        if (oldDir.renameTo(newDir)) {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(oldDir.absolutePath, newDir.absolutePath),
                null,
                null
            )
            onComplete(true)
            return null
        }

        if (mediaItems.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onComplete(false)
            return null
        }

        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        val relativeDir = if (newDir.absolutePath.startsWith(rootPath)) {
            newDir.absolutePath.removePrefix(rootPath).removePrefix("/")
        } else {
            val parts = newDir.absolutePath.split("/").filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                parts.drop(2).joinToString("/")
            } else {
                trimmedName
            }
        }

        val itemsWithNewRelPath = mediaItems.map { (uri, filePath) ->
            val fileParent = File(filePath).parentFile?.absolutePath ?: oldDir.absolutePath
            val subPath = if (fileParent.startsWith(oldDir.absolutePath)) {
                fileParent.removePrefix(oldDir.absolutePath).removePrefix("/")
            } else {
                ""
            }
            val itemRelativeDir = if (subPath.isNotEmpty()) {
                "$relativeDir/$subPath"
            } else {
                relativeDir
            }
            val finalRelPath = if (itemRelativeDir.endsWith("/")) itemRelativeDir else "$itemRelativeDir/"
            uri to finalRelPath
        }

        val pending = PendingFolderRename(
            folderPath = folderPath,
            newFolderName = trimmedName,
            items = itemsWithNewRelPath,
            oldPath = oldDir.absolutePath,
            newPath = newDir.absolutePath
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = itemsWithNewRelPath.map { it.first }
            val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, uris)
            renameLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            return pending
        }

        var successCount = 0
        for ((uri, relPath) in itemsWithNewRelPath) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
            }
            try {
                if (context.contentResolver.update(uri, values, null, null) > 0) {
                    successCount++
                }
            } catch (e: SecurityException) {
                val recoverable = e as? RecoverableSecurityException
                if (recoverable != null) {
                    renameLauncher.launch(IntentSenderRequest.Builder(recoverable.userAction.actionIntent.intentSender).build())
                    return pending
                }
                throw e
            }
        }

        onComplete(successCount > 0)
        return null
    }

    fun completePendingFolderRename(
        context: Context,
        pending: PendingFolderRename
    ): Boolean {
        var successCount = 0
        for ((uri, newRelPath) in pending.items) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, newRelPath)
            }
            val rows = context.contentResolver.update(uri, values, null, null)
            if (rows > 0) successCount++
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(pending.oldPath, pending.newPath),
            null,
            null
        )
        return successCount > 0
    }
}
