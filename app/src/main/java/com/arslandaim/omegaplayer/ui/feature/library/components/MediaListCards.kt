package com.arslandaim.omegaplayer.ui.feature.library.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arslandaim.omegaplayer.data.AudioModel
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.ui.common.videoThumbImageRequest
import com.arslandaim.omegaplayer.util.MediaUtils
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun VideoListItem(
    video: VideoModel,
    isPlaying: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    isInPlaylistView: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(video.name) }
    
    var showInfoDialog by remember { mutableStateOf(false) }
    var detailedInfo by remember { mutableStateOf<com.arslandaim.omegaplayer.util.DetailedMediaInfo?>(null) }

    if (showInfoDialog && detailedInfo != null) {
        MediaInfoDialog(
            info = detailedInfo!!,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.info_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRenameDialog = false
                    try {
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, newName)
                        }
                        context.contentResolver.update(video.uri, values, null, null)
                        android.widget.Toast.makeText(context, "Renamed", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: SecurityException) {
                        android.widget.Toast.makeText(context, "Needs Scoped Storage Permission", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.action_apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .sharedBounds(
                    rememberSharedContentState(key = "video_bounds_${video.uri}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceVariant 
                                else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 84.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(118.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    val context = LocalContext.current
                    val imageRequest = remember(video.id) {
                        videoThumbImageRequest(context, video.uri, "thumb_${video.id}")
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedElement(
                                rememberSharedContentState(key = "video_thumb_${video.uri}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
                        contentScale = ContentScale.Crop
                    )

                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayingVisualizer()
                        }
                    }
                    
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "${video.size / (1024 * 1024)} MB",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(modifier = Modifier.padding(start = 10.dp, end = 4.dp).weight(1f)) {
                    Text(
                        text = video.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Justify
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_rename)) },
                                onClick = { 
                                    showMenu = false
                                    showRenameDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                onClick = { 
                                    showMenu = false
                                    MediaUtils.shareMedia(context, listOf(video.uri), "video/*")
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
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
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isInPlaylistView) stringResource(R.string.menu_remove_from_playlist) else stringResource(R.string.menu_add_to_playlist)) },
                                onClick = { 
                                    showMenu = false
                                    onPlaylistClick() 
                                },
                                leadingIcon = { 
                                    Icon(
                                        if (isInPlaylistView) Icons.Default.PlaylistRemove else Icons.AutoMirrored.Filled.PlaylistAdd, 
                                        contentDescription = null
                                    ) 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                onClick = { 
                                    showMenu = false
                                    onDeleteClick() 
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioListItem(
    audio: AudioModel,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    isInPlaylistView: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(audio.name) }
    
    var showInfoDialog by remember { mutableStateOf(false) }
    var detailedInfo by remember { mutableStateOf<com.arslandaim.omegaplayer.util.DetailedMediaInfo?>(null) }

    if (showInfoDialog && detailedInfo != null) {
        MediaInfoDialog(
            info = detailedInfo!!,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.info_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRenameDialog = false
                    try {
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, newName)
                        }
                        context.contentResolver.update(audio.uri, values, null, null)
                        android.widget.Toast.makeText(context, "Renamed", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: SecurityException) {
                        android.widget.Toast.makeText(context, "Needs Scoped Storage Permission", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.action_apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    val albumArtUri = remember(audio) {
        MediaUtils.albumArtUri(audio.albumId)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceVariant 
                            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .heightIn(min = 84.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(118.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )

                val context = LocalContext.current
                val imageRequest = remember(albumArtUri) {
                    ImageRequest.Builder(context)
                        .data(albumArtUri)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingVisualizer()
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = formatDuration(audio.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${audio.size / (1024 * 1024)} MB",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(modifier = Modifier.padding(start = 10.dp, end = 4.dp).weight(1f)) {
                Text(
                    text = audio.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Justify
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            onClick = {
                                showMenu = false
                                showRenameDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = {
                                showMenu = false
                                MediaUtils.shareMedia(context, listOf(audio.uri), "audio/*")
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
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
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isInPlaylistView) stringResource(R.string.menu_remove_from_playlist) else stringResource(R.string.menu_add_to_playlist)) },
                            onClick = {
                                showMenu = false
                                onPlaylistClick()
                            },
                            leadingIcon = { Icon(if (isInPlaylistView) Icons.Default.PlaylistRemove else Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    }
}
