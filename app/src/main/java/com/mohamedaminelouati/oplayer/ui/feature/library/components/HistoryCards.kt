package com.mohamedaminelouati.oplayer.ui.feature.library.components

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
import com.mohamedaminelouati.oplayer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mohamedaminelouati.oplayer.data.RecentPlayback
import com.mohamedaminelouati.oplayer.ui.common.videoThumbImageRequest
import com.mohamedaminelouati.oplayer.util.MediaUtils.formatDuration
import com.mohamedaminelouati.oplayer.ui.theme.LocalSavePositionOnQuit
import com.mohamedaminelouati.oplayer.ui.theme.LocalTextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.maxLines
import com.mohamedaminelouati.oplayer.ui.theme.overflow

@Composable
fun RecentPlaybackItem(
    item: RecentPlayback,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.mediaType == "video") {
                val context = LocalContext.current
                val imageRequest = remember(item.uri) {
                    videoThumbImageRequest(context, Uri.parse(item.uri))
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 1f
                )
            } else {
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
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                }
            }

            val truncation = LocalTextTruncationMode.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Justify,
                    maxLines = truncation.maxLines,
                    overflow = truncation.overflow
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (item.lastPlayed > 0L) {
                    val context = LocalContext.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = com.mohamedaminelouati.oplayer.util.MediaUtils.formatPlaybackDate(item.lastPlayed, context),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = formatDuration(item.duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    if (item.size > 0L) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${item.size / (1024 * 1024)} MB",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                val savePositionOnQuit = LocalSavePositionOnQuit.current
                if (savePositionOnQuit) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val progress = item.position.toFloat() / item.duration.coerceAtLeast(1L)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
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
fun HistoryGridCard(
    item: RecentPlayback,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onDeleteFromDevice: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    aspectRatio: Float = 1f,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect() else onClick()
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
            if (item.mediaType == "video") {
                val context = LocalContext.current
                val imageRequest = remember(item.uri) {
                    videoThumbImageRequest(context, Uri.parse(item.uri))
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            val truncation = LocalTextTruncationMode.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                    .padding(start = 8.dp, end = if (onDelete != null || onDeleteFromDevice != null) 36.dp else 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Justify,
                    maxLines = truncation.maxLines,
                    overflow = truncation.overflow
                )
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
                                onClick()
                            },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_add_to_playlist)) },
                                onClick = {
                                    showMenu = false
                                    onAddToPlaylist()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove_from_history)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                        if (onDeleteFromDevice != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_from_device)) },
                                onClick = {
                                    showMenu = false
                                    onDeleteFromDevice()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                    .padding(8.dp)
            ) {
                if (item.lastPlayed > 0L) {
                    val context = LocalContext.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = com.mohamedaminelouati.oplayer.util.MediaUtils.formatPlaybackDate(item.lastPlayed, context),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = formatDuration(item.duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    if (item.size > 0L) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${item.size / (1024 * 1024)} MB",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                val savePositionOnQuit = LocalSavePositionOnQuit.current
                if (savePositionOnQuit) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val progress = item.position.toFloat() / item.duration.coerceAtLeast(1L)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
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
fun HistoryItem(
    item: RecentPlayback,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDeleteFromDevice: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect() else onClick()
                },
                onLongClick = onToggleSelect
            ),
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.mediaType == "video") {
                    val context = LocalContext.current
                    val imageRequest = remember(item.uri) {
                        videoThumbImageRequest(context, Uri.parse(item.uri))
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = formatDuration(item.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (item.size > 0L) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "${item.size / (1024 * 1024)} MB",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            val truncation = LocalTextTruncationMode.current
            Column(modifier = Modifier.padding(start = 10.dp, end = 4.dp).weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Justify,
                    maxLines = truncation.maxLines,
                    overflow = truncation.overflow
                )
                if (item.lastPlayed > 0L) {
                    val context = LocalContext.current
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = com.mohamedaminelouati.oplayer.util.MediaUtils.formatPlaybackDate(item.lastPlayed, context),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                val savePositionOnQuit = LocalSavePositionOnQuit.current
                if (savePositionOnQuit) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val progress = item.position.toFloat() / item.duration.coerceAtLeast(1L)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    IconButton(onClick = onToggleSelect) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
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
                                text = { Text(stringResource(R.string.action_play)) },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            if (onAddToPlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_add_to_playlist)) },
                                    onClick = {
                                        showMenu = false
                                        onAddToPlaylist()
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove_from_history)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_from_device)) },
                                onClick = {
                                    showMenu = false
                                    onDeleteFromDevice()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                    }
                }
            }
        }
    }
}
