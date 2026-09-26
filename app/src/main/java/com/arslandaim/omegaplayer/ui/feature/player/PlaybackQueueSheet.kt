package com.arslandaim.omegaplayer.ui.feature.player

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.media.PlaybackQueueItem
import com.arslandaim.omegaplayer.util.MediaUtils
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaybackQueueSheet(
    queueItems: List<PlaybackQueueItem>,
    currentMediaIndex: Int,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onItemClick: (index: Int) -> Unit,
    onItemRemove: (index: Int) -> Unit = {},
    onItemMove: (from: Int, to: Int) -> Unit = { _, _ -> }
) {
    var mutableQueue by remember(queueItems) { mutableStateOf(queueItems) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var draggedOffsetY by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.up_next),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(mutableQueue, key = { _, item -> item.uri }) { index, item ->
                    val isCurrent = index == currentMediaIndex
                    val isDragged = index == draggedIndex
                    
                    val elevation by animateFloatAsState(if (isDragged) 8f else 0f)
                    
                    val cardModifier = if (isDragged) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer { translationY = draggedOffsetY }
                    } else {
                        Modifier.animateItem()
                    }

                    Card(
                        modifier = cardModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onItemClick(index) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                                             else Color.Transparent
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { draggedIndex = index },
                                            onDragEnd = {
                                                draggedIndex = null
                                                draggedOffsetY = 0f
                                                if (mutableQueue != queueItems) {
                                                }
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                draggedOffsetY = 0f
                                                mutableQueue = queueItems
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            draggedOffsetY += dragAmount.y
                                            
                                            val threshold = 70f
                                            if (draggedOffsetY > threshold && index < mutableQueue.lastIndex) {
                                                val nextIndex = index + 1
                                                mutableQueue = mutableQueue.toMutableList().apply {
                                                    val temp = get(index)
                                                    set(index, get(nextIndex))
                                                    set(nextIndex, temp)
                                                }
                                                onItemMove(index, nextIndex)
                                                draggedIndex = nextIndex
                                                draggedOffsetY -= threshold
                                            } else if (draggedOffsetY < -threshold && index > 0) {
                                                val prevIndex = index - 1
                                                mutableQueue = mutableQueue.toMutableList().apply {
                                                    val temp = get(index)
                                                    set(index, get(prevIndex))
                                                    set(prevIndex, temp)
                                                }
                                                onItemMove(index, prevIndex)
                                                draggedIndex = prevIndex
                                                draggedOffsetY += threshold
                                            }
                                        }
                                    }
                            )
                            
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
                            
                            Text(
                                text = item.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(onClick = { onItemRemove(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
