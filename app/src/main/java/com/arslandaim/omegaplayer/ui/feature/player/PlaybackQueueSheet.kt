package com.arslandaim.omegaplayer.ui.feature.player

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.media.PlaybackQueueItem
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration
import android.content.ContentUris

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackQueueSheet(
    queueItems: List<PlaybackQueueItem>,
    currentMediaIndex: Int,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onItemClick: (index: Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.up_next),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn {
                itemsIndexed(queueItems) { index, item ->
                    val isCurrent = index == currentMediaIndex
                    ListItem(
                        headlineContent = {
                            Text(
                                item.title,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White
                            )
                        },
                        supportingContent = { Text(formatDuration(item.duration), color = Color.Gray) },
                        leadingContent = {
                            if (item.isVideo) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(com.arslandaim.omegaplayer.util.SmartVideoThumb(Uri.parse(item.uri), item.duration, item.uri.hashCode().toString()))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                    error = rememberVectorPainter(Icons.Default.Movie)
                                )
                            } else {
                                val albumArtUri = item.albumId?.let { id: Long ->
                                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)
                                }
                                AsyncImage(
                                    model = albumArtUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                    error = rememberVectorPainter(Icons.Default.MusicNote)
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onItemClick(index) }
                    )
                }
            }
        }
    }
}
