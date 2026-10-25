package com.arslandaim.omegaplayer.ui.feature.library.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.arslandaim.omegaplayer.R
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arslandaim.omegaplayer.data.AudioModel
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.ui.common.videoThumbImageRequest
import com.arslandaim.omegaplayer.util.MediaUtils
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration
import com.arslandaim.omegaplayer.ui.theme.LocalPlaybackProgress
import com.arslandaim.omegaplayer.ui.theme.LocalSavePositionOnQuit
import com.arslandaim.omegaplayer.ui.theme.LocalTextTruncationMode
import com.arslandaim.omegaplayer.ui.theme.maxLines
import com.arslandaim.omegaplayer.ui.theme.overflow

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun VideoGridItem(
    video: VideoModel,
    isPlaying: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (String) -> Unit,
    onDelete: () -> Unit,
    onPlaylist: () -> Unit,
    onRename: (String) -> Unit = {},
    isInPlaylistView: Boolean = false,
    isLocked: Boolean = false,
    onToggleLock: (() -> Unit)? = null,
    aspectRatio: Float = 1f,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var detailedInfo by remember { mutableStateOf<com.arslandaim.omegaplayer.util.DetailedMediaInfo?>(null) }

    if (showInfoDialog && detailedInfo != null) {
        MediaInfoDialog(
            info = detailedInfo!!,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showRenameDialog) {
        MediaRenameDialog(
            currentName = video.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { onRename(it) }
        )
    }
    
    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onToggleSelect() else onClick(MediaUtils.safeEncodeUri(video.uri.toString()))
                    },
                    onLongClick = onToggleSelect
                ),
            shape = RoundedCornerShape(20.dp),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val imageRequest = remember(video.id) {
                    videoThumbImageRequest(context, video.uri, "thumb_${video.id}")
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (isPlaying) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        PlayingVisualizer()
                    }
                }

                val truncation = LocalTextTruncationMode.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                        .padding(start = 8.dp, end = 36.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = video.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Justify,
                        maxLines = truncation.maxLines,
                        overflow = truncation.overflow
                    )
                }

                if (isLocked) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                if (isSelectionMode) {
                    IconButton(
                        onClick = onToggleSelect,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                    ) {
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.action_more),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_play)) },
                            onClick = {
                                showMenu = false
                                onClick(MediaUtils.safeEncodeUri(video.uri.toString()))
                            },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isInPlaylistView) stringResource(R.string.menu_remove_from_playlist) else stringResource(R.string.menu_add_to_playlist)) },
                            onClick = {
                                showMenu = false
                                onPlaylist()
                            },
                            leadingIcon = {
                                Icon(
                                    if (isInPlaylistView) Icons.Default.PlaylistRemove else Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            onClick = {
                                showMenu = false
                                showRenameDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = {
                                showMenu = false
                                MediaUtils.shareMedia(context, listOf(video.uri), "video/*")
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_information)) },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch {
                                    detailedInfo = com.arslandaim.omegaplayer.util.extractDetailedMediaInfo(
                                        context = context,
                                        uri = video.uri,
                                        fileName = video.name,
                                        path = video.path,
                                        sizeBytes = video.size,
                                        durationMs = video.duration
                                    )
                                    showInfoDialog = true
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        if (onToggleLock != null) {
                            DropdownMenuItem(
                                text = { Text(if (isLocked) stringResource(R.string.action_unlock_media) else stringResource(R.string.action_lock_media)) },
                                onClick = {
                                    showMenu = false
                                    onToggleLock()
                                },
                                leadingIcon = { Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_from_device)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${video.size / (1024 * 1024)} MB",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                val savePositionOnQuit = LocalSavePositionOnQuit.current
                val playbackProgress = if (savePositionOnQuit) LocalPlaybackProgress.current[video.uri.toString()] ?: 0f else 0f
                if (playbackProgress > 0f) {
                    LinearProgressIndicator(
                        progress = { playbackProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(3.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioGridItem(
    audio: AudioModel,
    isPlaying: Boolean = false,
    onClick: (String) -> Unit,
    onDelete: () -> Unit,
    onPlaylist: () -> Unit,
    onRename: (String) -> Unit = {},
    isInPlaylistView: Boolean = false,
    isLocked: Boolean = false,
    onToggleLock: (() -> Unit)? = null,
    aspectRatio: Float = 1f,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var detailedInfo by remember { mutableStateOf<com.arslandaim.omegaplayer.util.DetailedMediaInfo?>(null) }

    if (showInfoDialog && detailedInfo != null) {
        MediaInfoDialog(
            info = detailedInfo!!,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showRenameDialog) {
        MediaRenameDialog(
            currentName = audio.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { onRename(it) }
        )
    }
    
    val albumArtUri = remember(audio) {
        MediaUtils.albumArtUri(audio.albumId)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect() else onClick(MediaUtils.safeEncodeUri(audio.uri.toString()))
                },
                onLongClick = onToggleSelect
            ),
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        )
                    )
                )
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            )

            AsyncImage(
                model = albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (isPlaying) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    PlayingVisualizer()
                }
            }

            val truncation = LocalTextTruncationMode.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                    .padding(start = 8.dp, end = 36.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = audio.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Justify,
                    maxLines = truncation.maxLines,
                    overflow = truncation.overflow
                )
            }

            if (isLocked) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            if (isSelectionMode) {
                IconButton(
                    onClick = onToggleSelect,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                ) {
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_play)) },
                        onClick = {
                            showMenu = false
                            onClick(MediaUtils.safeEncodeUri(audio.uri.toString()))
                        },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isInPlaylistView) stringResource(R.string.menu_remove_from_playlist) else stringResource(R.string.menu_add_to_playlist)) },
                        onClick = {
                            showMenu = false
                            onPlaylist()
                        },
                        leadingIcon = {
                            Icon(
                                if (isInPlaylistView) Icons.Default.PlaylistRemove else Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share)) },
                        onClick = {
                            showMenu = false
                            MediaUtils.shareMedia(context, listOf(audio.uri), "audio/*")
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_information)) },
                        onClick = {
                            showMenu = false
                            coroutineScope.launch {
                                detailedInfo = com.arslandaim.omegaplayer.util.extractDetailedMediaInfo(
                                    context = context,
                                    uri = audio.uri,
                                    fileName = audio.name,
                                    path = audio.path,
                                    sizeBytes = audio.size,
                                    durationMs = audio.duration
                                )
                                showInfoDialog = true
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    if (onToggleLock != null) {
                        DropdownMenuItem(
                            text = { Text(if (isLocked) stringResource(R.string.action_unlock_media) else stringResource(R.string.action_lock_media)) },
                            onClick = {
                                showMenu = false
                                onToggleLock()
                            },
                            leadingIcon = { Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_from_device)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = formatDuration(audio.duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = "${audio.size / (1024 * 1024)} MB",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            val savePositionOnQuit = LocalSavePositionOnQuit.current
            val playbackProgress = if (savePositionOnQuit) LocalPlaybackProgress.current[audio.uri.toString()] ?: 0f else 0f
            if (playbackProgress > 0f) {
                LinearProgressIndicator(
                    progress = { playbackProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(3.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}
