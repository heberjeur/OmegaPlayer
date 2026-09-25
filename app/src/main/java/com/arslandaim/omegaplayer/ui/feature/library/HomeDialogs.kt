package com.arslandaim.omegaplayer.ui.feature.library

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.text.style.TextAlign
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.data.AudioModel
import com.arslandaim.omegaplayer.viewmodel.AudioViewModel
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel
import com.arslandaim.omegaplayer.data.RecentPlayback
import com.arslandaim.omegaplayer.ui.feature.library.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope

@Composable
fun DeleteFolderDialog(
    folderName: String,
    isVideoTab: Boolean,
    viewModel: VideoViewModel,
    audioViewModel: AudioViewModel,
    context: Context,
    scope: CoroutineScope,
    onProcessingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(text = if (isVideoTab) stringResource(R.string.delete_video_folder_title) else stringResource(R.string.delete_audio_folder_title), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
        text = { Text(text = stringResource(R.string.delete_folder_confirm, folderName), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                scope.launch {
                    onProcessingChange(true)
                    if (isVideoTab) {
                        val videosToDelete = viewModel.getVideosInFolder(folderName)
                        if (videosToDelete.isNotEmpty()) {
                            videosToDelete.forEach { viewModel.deleteHistoryItem(it.uri.toString()) }
                            viewModel.stopIfPlaying(videosToDelete.map { it.uri })
                            val deletedUris = withContext(Dispatchers.IO) {
                                videosToDelete.filter { context.contentResolver.delete(it.uri, null, null) > 0 }.map { it.uri }
                            }
                            if (deletedUris.isNotEmpty()) viewModel.onVideosDeleted(deletedUris)
                            if (deletedUris.size != videosToDelete.size) viewModel.refreshVideos(context)
                        }
                    } else {
                        val audiosToDelete = audioViewModel.getAudiosInFolder(folderName)
                        if (audiosToDelete.isNotEmpty()) {
                            audiosToDelete.forEach { audioViewModel.deleteHistoryItem(it.uri.toString()) }
                            audioViewModel.stopIfPlaying(audiosToDelete.map { it.uri })
                            val deletedUris = withContext(Dispatchers.IO) {
                                audiosToDelete.filter { context.contentResolver.delete(it.uri, null, null) > 0 }.map { it.uri }
                            }
                            if (deletedUris.isNotEmpty()) audioViewModel.onAudiosDeleted(deletedUris)
                            if (deletedUris.size != audiosToDelete.size) audioViewModel.refreshAudios(context)
                        }
                    }
                    onProcessingChange(false)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun DeleteVideoDialog(
    video: VideoModel,
    viewModel: VideoViewModel,
    context: Context,
    scope: CoroutineScope,
    onProcessingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(text = stringResource(R.string.delete_video_title), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
        text = { Text(text = stringResource(R.string.delete_media_confirm, video.name), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                scope.launch {
                    onProcessingChange(true)
                    viewModel.deleteHistoryItem(video.uri.toString())
                    viewModel.stopIfPlaying(video.uri)
                    val deleted = withContext(Dispatchers.IO) { context.contentResolver.delete(video.uri, null, null) }
                    if (deleted > 0) viewModel.onVideosDeleted(listOf(video.uri)) else viewModel.refreshVideos(context)
                    onProcessingChange(false)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun DeleteAudioDialog(
    audio: AudioModel,
    audioViewModel: AudioViewModel,
    context: Context,
    scope: CoroutineScope,
    onProcessingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(text = stringResource(R.string.delete_audio_title), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
        text = { Text(text = stringResource(R.string.delete_media_confirm, audio.name), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                scope.launch {
                    onProcessingChange(true)
                    audioViewModel.deleteHistoryItem(audio.uri.toString())
                    audioViewModel.stopIfPlaying(audio.uri)
                    val deleted = withContext(Dispatchers.IO) { context.contentResolver.delete(audio.uri, null, null) }
                    if (deleted > 0) audioViewModel.onAudiosDeleted(listOf(audio.uri)) else audioViewModel.refreshAudios(context)
                    onProcessingChange(false)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun DeleteHistoryItemDialog(
    item: RecentPlayback,
    viewModel: VideoViewModel,
    audioViewModel: AudioViewModel,
    context: Context,
    scope: CoroutineScope,
    onProcessingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(text = stringResource(R.string.delete_media_title), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
        text = { Text(text = stringResource(R.string.delete_media_from_device_confirm, item.name), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                scope.launch {
                    onProcessingChange(true)
                    viewModel.deleteHistoryItem(item.uri)
                    val uriToDel = Uri.parse(item.uri)
                    val isVideo = item.mediaType == "video"
                    if (isVideo) viewModel.stopIfPlaying(uriToDel) else audioViewModel.stopIfPlaying(uriToDel)
                    val deleted = withContext(Dispatchers.IO) { context.contentResolver.delete(uriToDel, null, null) }
                    if (deleted > 0) {
                        if (isVideo) viewModel.onVideosDeleted(listOf(uriToDel)) else audioViewModel.onAudiosDeleted(listOf(uriToDel))
                    } else {
                        viewModel.refreshVideos(context)
                        audioViewModel.refreshAudios(context)
                    }
                    onProcessingChange(false)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.action_cancel)) } }
    )
}

