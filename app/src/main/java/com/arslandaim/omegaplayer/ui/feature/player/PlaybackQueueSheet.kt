package com.arslandaim.omegaplayer.ui.feature.player

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.arslandaim.omegaplayer.ui.feature.library.components.MediaInfoDialog
import com.arslandaim.omegaplayer.ui.feature.library.components.PlayingVisualizer
import com.arslandaim.omegaplayer.ui.feature.library.components.SelectionFooterBar
import com.arslandaim.omegaplayer.ui.theme.LocalTextTruncationMode
import com.arslandaim.omegaplayer.ui.theme.maxLines
import com.arslandaim.omegaplayer.ui.theme.overflow
import coil.request.ImageRequest
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.media.PlaybackQueueItem
import com.arslandaim.omegaplayer.util.DetailedMediaInfo
import com.arslandaim.omegaplayer.util.MediaUtils
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration
import com.arslandaim.omegaplayer.util.extractDetailedMediaInfo
import kotlinx.coroutines.launch

private data class DraggableQueueItem(
    val instanceId: Long,
    val item: PlaybackQueueItem
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaybackQueueSheet(
    queueItems: List<PlaybackQueueItem>,
    currentMediaIndex: Int,
    isPlaying: Boolean = false,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onItemClick: (index: Int) -> Unit,
    onItemRemove: (index: Int) -> Unit = {},
    onItemMove: (from: Int, to: Int) -> Unit = { _, _ -> },
    onItemAddToPlaylist: ((PlaybackQueueItem) -> Unit)? = null,
    onItemsAddToPlaylist: ((List<PlaybackQueueItem>) -> Unit)? = null,
    onItemDeleteFromDevice: ((PlaybackQueueItem) -> Unit)? = null,
    onItemsDeleteFromDevice: ((List<PlaybackQueueItem>) -> Unit)? = null,
    isFullyExpanded: Boolean = true
) {
    var mutableQueue by remember {
        mutableStateOf(queueItems.mapIndexed { idx, it -> DraggableQueueItem(idx.toLong(), it) })
    }
    var isDragging by remember { mutableStateOf(false) }
    var initialDragIndex by remember { mutableIntStateOf(-1) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var draggedOffsetY by remember { mutableFloatStateOf(0f) }
    var selectedInstanceIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedInstanceIds.isNotEmpty()
    var detailedInfoItem by remember { mutableStateOf<DetailedMediaInfo?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    BackHandler(enabled = isSelectionMode) {
        selectedInstanceIds = emptySet()
    }

    val itemHeightPx = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat()
        ?: with(density) { 98.dp.toPx() }

    LaunchedEffect(queueItems) {
        if (!isDragging) {
            mutableQueue = queueItems.mapIndexed { idx, it -> DraggableQueueItem(idx.toLong(), it) }
            val validIds = mutableQueue.map { it.instanceId }.toSet()
            selectedInstanceIds = selectedInstanceIds.filter { validIds.contains(it) }.toSet()
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val columnModifier = if (isFullyExpanded) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxWidth()
                .then(if (!isSelectionMode) Modifier.navigationBarsPadding() else Modifier)
        }

        Column(
            modifier = columnModifier
        ) {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedInstanceIds = emptySet() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${selectedInstanceIds.size}/${mutableQueue.size}",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    val isAllSelected = mutableQueue.isNotEmpty() && selectedInstanceIds.size == mutableQueue.size
                    TextButton(
                        onClick = {
                            selectedInstanceIds = if (isAllSelected) {
                                emptySet()
                            } else {
                                mutableQueue.map { it.instanceId }.toSet()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(if (isAllSelected) R.string.action_deselect_all else R.string.action_select_all),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.up_next),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = isFullyExpanded),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (isSelectionMode) 8.dp else 24.dp)
            ) {
                itemsIndexed(mutableQueue, key = { _, entry -> entry.instanceId }) { index, entry ->
                    val item = entry.item
                    val isDragged = index == draggedIndex
                    val isSelected = selectedInstanceIds.contains(entry.instanceId)
                    val isCurrent = index == currentMediaIndex
                    
                    val elevation by animateFloatAsState(if (isDragged) 8f else 0f)
                    
                    val cardModifier = if (isDragged) {
                        Modifier
                            .zIndex(10f)
                            .graphicsLayer { translationY = draggedOffsetY }
                    } else {
                        Modifier.animateItem()
                    }

                    Box(
                        modifier = cardModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .combinedClickable(
                                    enabled = !isDragging,
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedInstanceIds = if (isSelected) {
                                                selectedInstanceIds - entry.instanceId
                                            } else {
                                                selectedInstanceIds + entry.instanceId
                                            }
                                        } else {
                                            onItemClick(index)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedInstanceIds = if (isSelected) {
                                            selectedInstanceIds - entry.instanceId
                                        } else {
                                            selectedInstanceIds + entry.instanceId
                                        }
                                    }
                                ),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isDragged -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            ),
                            border = when {
                                isDragged -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                isCurrent -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                else -> null
                            },
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .pointerInput(entry.instanceId) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    val startIdx = mutableQueue.indexOfFirst { it.instanceId == entry.instanceId }
                                                    if (startIdx != -1) {
                                                        selectedInstanceIds = emptySet()
                                                        isDragging = true
                                                        initialDragIndex = startIdx
                                                        draggedIndex = startIdx
                                                        draggedOffsetY = 0f
                                                    }
                                                },
                                            onDragEnd = {
                                                val from = initialDragIndex
                                                val to = draggedIndex
                                                if (from != -1 && to != null && from != to) {
                                                    onItemMove(from, to)
                                                }
                                                isDragging = false
                                                initialDragIndex = -1
                                                draggedIndex = null
                                                draggedOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                val from = initialDragIndex
                                                val to = draggedIndex
                                                if (from != -1 && to != null && from != to) {
                                                    onItemMove(from, to)
                                                } else {
                                                    mutableQueue = queueItems.mapIndexed { idx, it -> DraggableQueueItem(idx.toLong(), it) }
                                                }
                                                isDragging = false
                                                initialDragIndex = -1
                                                draggedIndex = null
                                                draggedOffsetY = 0f
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            if (mutableQueue.size <= 1) return@detectDragGestures
                                            var currentIdx = draggedIndex ?: return@detectDragGestures
                                            val step = itemHeightPx.coerceAtLeast(1f)
                                            val threshold = step * 0.5f

                                            draggedOffsetY += dragAmount.y

                                            while (draggedOffsetY > threshold && currentIdx < mutableQueue.lastIndex) {
                                                val nextIndex = currentIdx + 1
                                                val updated = mutableQueue.toMutableList()
                                                val temp = updated[currentIdx]
                                                updated[currentIdx] = updated[nextIndex]
                                                updated[nextIndex] = temp
                                                mutableQueue = updated
                                                currentIdx = nextIndex
                                                draggedIndex = nextIndex
                                                draggedOffsetY -= step

                                                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: nextIndex
                                                if (nextIndex >= lastVisible - 1) {
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem((nextIndex + 1).coerceAtMost(mutableQueue.lastIndex))
                                                    }
                                                }
                                            }

                                            while (draggedOffsetY < -threshold && currentIdx > 0) {
                                                val prevIndex = currentIdx - 1
                                                val updated = mutableQueue.toMutableList()
                                                val temp = updated[currentIdx]
                                                updated[currentIdx] = updated[prevIndex]
                                                updated[prevIndex] = temp
                                                mutableQueue = updated
                                                currentIdx = prevIndex
                                                draggedIndex = prevIndex
                                                draggedOffsetY += step

                                                if (prevIndex <= listState.firstVisibleItemIndex + 1) {
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem((prevIndex - 1).coerceAtLeast(0))
                                                    }
                                                }
                                            }

                                            if (currentIdx == 0 && draggedOffsetY < 0f) {
                                                draggedOffsetY = 0f
                                            } else if (currentIdx == mutableQueue.lastIndex && draggedOffsetY > 0f) {
                                                draggedOffsetY = 0f
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = null,
                                    tint = if (isDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(118.dp)
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(if (!item.isVideo) {
                                        Modifier.background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                                                )
                                            )
                                        )
                                    } else Modifier)
                            ) {
                                if (item.isVideo) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(com.arslandaim.omegaplayer.util.SmartVideoThumb(Uri.parse(item.uri), item.duration, item.uri.hashCode().toString()))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = rememberVectorPainter(Icons.Default.Movie)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPlaying) {
                                            PlayingVisualizer()
                                        } else {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.6f),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Pause,
                                                        contentDescription = stringResource(R.string.action_pause),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = formatDuration(item.duration),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            val truncation = LocalTextTruncationMode.current
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = item.title,
                                    color = if (isDragged || isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isDragged || isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = truncation.maxLines,
                                    overflow = truncation.overflow
                                )
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = stringResource(if (isPlaying) R.string.now_playing else R.string.state_paused),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedInstanceIds = if (checked) {
                                            selectedInstanceIds + entry.instanceId
                                        } else {
                                            selectedInstanceIds - entry.instanceId
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        val updated = mutableQueue.toMutableList()
                                        if (index in updated.indices) {
                                            updated.removeAt(index)
                                            mutableQueue = updated
                                        }
                                        onItemRemove(index)
                                    },
                                    enabled = !isDragging
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                val selectedEntries = remember(mutableQueue, selectedInstanceIds) {
                    mutableQueue.filter { selectedInstanceIds.contains(it.instanceId) }
                }
                val currentItem = mutableQueue.getOrNull(currentMediaIndex)
                val canPlayNext = selectedEntries.isNotEmpty() && selectedEntries.any { it != currentItem }
                val canMoveToTop = selectedEntries.isNotEmpty() && selectedEntries.any { mutableQueue.indexOf(it) > 0 }
                val canMoveToBottom = selectedEntries.isNotEmpty() && selectedEntries.any { mutableQueue.indexOf(it) < mutableQueue.lastIndex }

                SelectionFooterBar(
                    onPlay = {
                        val firstSel = selectedEntries.firstOrNull()
                        val idx = if (firstSel != null) mutableQueue.indexOf(firstSel) else -1
                        selectedInstanceIds = emptySet()
                        if (idx != -1) onItemClick(idx)
                    },
                    onPlayNext = {
                        val listToMove = selectedEntries.filter { it != currentItem }
                        if (listToMove.isNotEmpty()) {
                            val updated = mutableQueue.toMutableList()
                            listToMove.reversed().forEach { sel ->
                                val curIdx = updated.indexOf(sel)
                                val currMediaIdx = if (currentItem != null) updated.indexOf(currentItem) else currentMediaIndex
                                val targetIdx = if (curIdx > currMediaIdx) currMediaIdx + 1 else currMediaIdx
                                if (curIdx != -1 && targetIdx in updated.indices && curIdx != targetIdx) {
                                    val moved = updated.removeAt(curIdx)
                                    updated.add(targetIdx, moved)
                                    onItemMove(curIdx, targetIdx)
                                }
                            }
                            mutableQueue = updated
                            selectedInstanceIds = emptySet()
                        }
                    },
                    onMoveToTop = {
                        val updated = mutableQueue.toMutableList()
                        selectedEntries.reversed().forEach { sel ->
                            val curIdx = updated.indexOf(sel)
                            if (curIdx > 0) {
                                val moved = updated.removeAt(curIdx)
                                updated.add(0, moved)
                                onItemMove(curIdx, 0)
                            }
                        }
                        mutableQueue = updated
                        selectedInstanceIds = emptySet()
                    },
                    onMoveToBottom = {
                        val updated = mutableQueue.toMutableList()
                        selectedEntries.forEach { sel ->
                            val curIdx = updated.indexOf(sel)
                            if (curIdx != -1 && curIdx < updated.lastIndex) {
                                val moved = updated.removeAt(curIdx)
                                updated.add(updated.size, moved)
                                onItemMove(curIdx, updated.lastIndex)
                            }
                        }
                        mutableQueue = updated
                        selectedInstanceIds = emptySet()
                    },
                    onAddToPlaylist = if (onItemAddToPlaylist != null || onItemsAddToPlaylist != null) {
                        {
                            val items = selectedEntries.map { it.item }
                            if (onItemsAddToPlaylist != null) {
                                onItemsAddToPlaylist(items)
                            } else if (onItemAddToPlaylist != null) {
                                items.forEach { onItemAddToPlaylist(it) }
                            }
                            selectedInstanceIds = emptySet()
                        }
                    } else null,
                    onShare = {
                        val items = selectedEntries.map { it.item }
                        val uris = items.map { Uri.parse(it.uri) }
                        val mime = if (items.all { it.isVideo }) "video/*" else if (items.all { !it.isVideo }) "audio/*" else "*/*"
                        MediaUtils.shareMedia(context, uris, mime)
                        selectedInstanceIds = emptySet()
                    },
                    onInformation = if (selectedEntries.size == 1) {
                        {
                            val item = selectedEntries.first().item
                            coroutineScope.launch {
                                try {
                                    val file = java.io.File(item.uri)
                                    val sizeBytes = if (file.exists()) file.length() else 0L
                                    detailedInfoItem = extractDetailedMediaInfo(
                                        context = context,
                                        uri = Uri.parse(item.uri),
                                        fileName = item.title,
                                        path = item.uri,
                                        sizeBytes = sizeBytes,
                                        durationMs = item.duration
                                    )
                                } catch (e: Exception) {
                                    Log.e("PlaybackQueueSheet", "Error extracting info", e)
                                }
                            }
                            selectedInstanceIds = emptySet()
                        }
                    } else null,
                    onRemoveFromQueue = {
                        val indicesToRemove = mutableQueue.mapIndexedNotNull { idx, entry ->
                            if (selectedInstanceIds.contains(entry.instanceId)) idx else null
                        }
                        val updated = mutableQueue.toMutableList()
                        indicesToRemove.sortedDescending().forEach { idx ->
                            if (idx in updated.indices) {
                                updated.removeAt(idx)
                            }
                            onItemRemove(idx)
                        }
                        mutableQueue = updated
                        selectedInstanceIds = emptySet()
                    },
                    onDelete = if (onItemDeleteFromDevice != null || onItemsDeleteFromDevice != null) {
                        {
                            val items = mutableQueue.filter { selectedInstanceIds.contains(it.instanceId) }.map { it.item }
                            if (onItemsDeleteFromDevice != null) {
                                onItemsDeleteFromDevice(items)
                            } else if (onItemDeleteFromDevice != null) {
                                items.forEach { onItemDeleteFromDevice(it) }
                            }
                            selectedInstanceIds = emptySet()
                        }
                    } else null,
                    deleteLabel = stringResource(R.string.delete_from_device),
                    canPlay = selectedEntries.isNotEmpty(),
                    canPlayNext = canPlayNext,
                    canMoveToTop = canMoveToTop,
                    canMoveToBottom = canMoveToBottom,
                    canAddToPlaylist = selectedEntries.isNotEmpty(),
                    canShare = selectedEntries.isNotEmpty(),
                    canInformation = selectedEntries.size == 1,
                    canRemoveFromQueue = selectedEntries.isNotEmpty(),
                    canDelete = selectedEntries.isNotEmpty(),
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }

    if (detailedInfoItem != null) {
        MediaInfoDialog(
            info = detailedInfoItem!!,
            onDismiss = { detailedInfoItem = null }
        )
    }
}
