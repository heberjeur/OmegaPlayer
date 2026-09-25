package com.arslandaim.omegaplayer.ui.feature.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration
import coil.compose.AsyncImage
import com.arslandaim.omegaplayer.ui.common.WaveformVisualizer
import android.net.Uri
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.ui.graphics.graphicsLayer
import androidx.media3.session.MediaController
import com.arslandaim.omegaplayer.data.AudioModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerTopBar(
    onBack: () -> Unit,
    showMoreOptions: Boolean,
    onMoreOptionsChange: (Boolean) -> Unit,
    stopAfterCurrent: Boolean,
    sleepTimerActive: Boolean,
    sleepTimerTimeLeft: Long,
    playbackSpeed: Float,
    onShowQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSleepTimer: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onEqualizer: () -> Unit,
    onInfo: () -> Unit,
    onDeleteRequested: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = { onShowQueue() }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.queue), tint = Color.White)
            }
            IconButton(onClick = { onMoreOptionsChange(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more), tint = Color.White)
            }
            DropdownMenu(
                expanded = showMoreOptions,
                onDismissRequest = { onMoreOptionsChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_to_playlist)) },
                    onClick = {
                        onMoreOptionsChange(false)
                        onAddToPlaylist()
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = {
                        val text = if (stopAfterCurrent) stringResource(R.string.sleep_timer_end_of_track)
                                  else if (sleepTimerActive) stringResource(R.string.sleep_timer_format, formatDuration(sleepTimerTimeLeft))
                                  else stringResource(R.string.sleep_timer)
                        Text(text)
                    },
                    onClick = {
                        onMoreOptionsChange(false)
                        onSleepTimer()
                    },
                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = if (sleepTimerActive) MaterialTheme.colorScheme.primary else Color.White) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.playback_speed_format, String.format(java.util.Locale.US, "%.2f", playbackSpeed))) },
                    onClick = {
                        onMoreOptionsChange(false)
                        onPlaybackSpeed()
                    },
                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.equalizer)) },
                    onClick = {
                        onMoreOptionsChange(false)
                        onEqualizer()
                    },
                    leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_information)) },
                    onClick = {
                        onMoreOptionsChange(false)
                        onInfo()
                    },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        onMoreOptionsChange(false)
                        onDeleteRequested()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun AudioPlayerContent(
    padding: PaddingValues,
    controller: MediaController?,
    currentAudio: AudioModel?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    pendingSeekPosition: Long,
    repeatMode: Int,
    isShuffle: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    artScale: Float,
    albumArtUri: Uri?,
    onSeekPreview: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onCycleRepeatClick: () -> Unit,
    onToggleShuffleClick: () -> Unit
) {
    if (controller == null) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        val player = controller!!
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        val mediaInfo: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentAudio?.name ?: stringResource(R.string.unknown_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }

        val visualizer: @Composable () -> Unit = {
            WaveformVisualizer(
                isPlaying = isPlaying,
                modifier = Modifier.padding(vertical = if (isLandscape) 8.dp else 24.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        val seekBar: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxWidth()) {
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                androidx.compose.material3.Slider(
                    value = if (duration > 0) {
                        val pos = if (pendingSeekPosition >= 0) pendingSeekPosition else currentPosition
                        pos.toFloat() / duration.toFloat()
                    } else 0f,
                    onValueChange = onSeekPreview,
                    onValueChangeFinished = onSeekFinished,
                    valueRange = 0f..1f,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        androidx.compose.material3.SliderDefaults.Track(
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                            ),
                            sliderState = sliderState,
                            modifier = Modifier.height(2.dp)
                        )
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        val controls: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = if (isLandscape) 8.dp else 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCycleRepeatClick) {
                    Icon(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = stringResource(R.string.action_repeat),
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                    )
                }

                IconButton(onClick = { player.seekToPrevious() }, enabled = hasPrevious) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.action_previous),
                        modifier = Modifier.size(36.dp),
                        tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f)
                    )
                }

                FilledIconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(84.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.action_play_pause),
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(onClick = { player.seekToNext() }, enabled = hasNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.action_next),
                        modifier = Modifier.size(36.dp),
                        tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f)
                    )
                }

                IconButton(onClick = onToggleShuffleClick) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = stringResource(R.string.action_shuffle),
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 48.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                mediaInfo()
                visualizer()
                seekBar()
                controls()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .graphicsLayer {
                            scaleX = artScale
                            scaleY = artScale
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = albumArtUri,
                        contentDescription = stringResource(R.string.album_art),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = rememberVectorPainter(Icons.Default.MusicNote),
                        fallback = rememberVectorPainter(Icons.Default.MusicNote)
                    )
                }
                mediaInfo()
                visualizer()
                seekBar()
                controls()
            }
        }
    }
}
