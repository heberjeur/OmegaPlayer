/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.ui.feature.player

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import com.arslandaim.omegaplayer.util.MediaUtils
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.arslandaim.omegaplayer.viewmodel.AudioViewModel
import com.arslandaim.omegaplayer.ui.common.WaveformVisualizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import android.content.ContentUris
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.BackHandler

import android.media.audiofx.Equalizer
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import android.provider.MediaStore
import android.content.Intent
import android.widget.Toast
import com.arslandaim.omegaplayer.data.Playlist
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import com.arslandaim.omegaplayer.data.AudioModel
import android.content.Context
import android.app.ActivityManager
import android.os.Build
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    stopAfterCurrent: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onStopAfterCurrentToggle: (Boolean) -> Unit
) {
    var minutes by remember { mutableIntStateOf(if (currentMinutes > 0) currentMinutes else 30) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!stopAfterCurrent) {
                    Text(stringResource(R.string.minutes_format, minutes))
                    Slider(
                        value = minutes.toFloat(),
                        onValueChange = { minutes = it.toInt() },
                        valueRange = 0f..120f,
                        steps = 23 // 5 min increments if 0-120
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = stopAfterCurrent,
                        onCheckedChange = { onStopAfterCurrentToggle(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.stop_after_current))
                }

                if (currentMinutes > 0 || stopAfterCurrent) {
                    TextButton(onClick = { 
                        onConfirm(0)
                        onStopAfterCurrentToggle(false)
                    }) {
                        Text(stringResource(R.string.action_turn_off), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (!stopAfterCurrent) {
                Button(onClick = { onConfirm(minutes) }) {
                    Text(stringResource(R.string.action_set))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Int) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.menu_new_playlist)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        onCreatePlaylist(newPlaylistName)
                        showCreateDialog = false
                    }
                }) { Text(stringResource(R.string.action_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_add_to_playlist)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.menu_new_playlist))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.no_playlists_yet), modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                leadingContent = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                                modifier = Modifier.clickable { onPlaylistSelected(playlist.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
fun AudioEqualizerDialog(
    onDismiss: () -> Unit
) {
    var bassLevel by remember { mutableFloatStateOf(0.5f) }
    var midLevel by remember { mutableFloatStateOf(0.5f) }
    var trebleLevel by remember { mutableFloatStateOf(0.5f) }
    var selectedPreset by remember { mutableStateOf("Flat") }

    val presetFlat = stringResource(R.string.preset_flat)
    val presetBassBoost = stringResource(R.string.preset_bass_boost)
    val presetTrebleBoost = stringResource(R.string.preset_treble_boost)
    val presetRock = stringResource(R.string.preset_rock)
    val presetPop = stringResource(R.string.preset_pop)
    val presetVocal = stringResource(R.string.preset_vocal)

    val presets = listOf(presetFlat, presetBassBoost, presetTrebleBoost, presetRock, presetPop, presetVocal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.audio_equalizer), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.presets), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(3).forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                when (preset) {
                                    presetFlat -> { bassLevel = 0.5f; midLevel = 0.5f; trebleLevel = 0.5f }
                                    presetBassBoost -> { bassLevel = 0.85f; midLevel = 0.5f; trebleLevel = 0.4f }
                                    presetTrebleBoost -> { bassLevel = 0.4f; midLevel = 0.5f; trebleLevel = 0.85f }
                                }
                            },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.drop(3).forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                when (preset) {
                                    presetRock -> { bassLevel = 0.75f; midLevel = 0.6f; trebleLevel = 0.75f }
                                    presetPop -> { bassLevel = 0.6f; midLevel = 0.7f; trebleLevel = 0.6f }
                                    presetVocal -> { bassLevel = 0.3f; midLevel = 0.8f; trebleLevel = 0.5f }
                                }
                            },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(stringResource(R.string.bass_label), style = MaterialTheme.typography.labelSmall)
                Slider(value = bassLevel, onValueChange = { bassLevel = it; selectedPreset = "Custom" })

                Text(stringResource(R.string.mid_label), style = MaterialTheme.typography.labelSmall)
                Slider(value = midLevel, onValueChange = { midLevel = it; selectedPreset = "Custom" })

                Text(stringResource(R.string.treble_label), style = MaterialTheme.typography.labelSmall)
                Slider(value = trebleLevel, onValueChange = { trebleLevel = it; selectedPreset = "Custom" })
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    audioUri: String,
    viewModel: AudioViewModel,
    onBack: () -> Unit,
    onVideoTransition: (String) -> Unit = {},
    initialPosition: Long = -1L
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMoreOptions by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val controller by viewModel.mediaController.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val sleepTimerActive by viewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val sleepTimerTimeLeft by viewModel.sleepTimerTimeLeft.collectAsStateWithLifecycle()
    val stopAfterCurrent by viewModel.stopAfterCurrent.collectAsStateWithLifecycle()
    val globalAudios by viewModel.audios.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val audios by viewModel.audiosInSelectedFolder.collectAsStateWithLifecycle()
    
    val speedScope by viewModel.speedScope.collectAsStateWithLifecycle()
    val globalSpeed by viewModel.globalPlaybackSpeed.collectAsStateWithLifecycle()
    val folderSpeed by (if (selectedFolder != null) viewModel.getFolderSpeed(selectedFolder!!) else flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
    val effectiveSpeed = remember(speedScope, globalSpeed, folderSpeed) {
        if (speedScope == com.arslandaim.omegaplayer.data.PlaybackSpeedScope.PER_FOLDER && folderSpeed != null) folderSpeed!! else globalSpeed
    }

    var currentAudio by remember { mutableStateOf(globalAudios.find { it.uri.toString() == audioUri }) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            currentAudio?.let { viewModel.stopIfPlaying(it.uri) }
            viewModel.refreshAudios(context)
            onBack()
        }
    }

    val currentFolder = remember(currentAudio, audioUri, globalAudios) {
        val path = currentAudio?.path ?: globalAudios.find { it.uri.toString() == audioUri }?.path
        if (!path.isNullOrBlank()) {
            java.io.File(path).parentFile?.name ?: "Internal"
        } else {
            "Internal"
        }
    }
    val folderAudios = remember(currentFolder, globalAudios) {
        viewModel.getAudiosInFolder(currentFolder)
    }
    val activeQueueAudios = remember(folderAudios, audios, globalAudios) {
        if (folderAudios.isNotEmpty()) folderAudios
        else if (audios.isNotEmpty()) audios
        else globalAudios
    }

    LaunchedEffect(globalAudios, audioUri) {
        if (selectedFolder == null && globalAudios.isNotEmpty()) {
            val audio = globalAudios.find { it.uri.toString() == audioUri }
            audio?.let {
                val folderName = java.io.File(it.path).parentFile?.name ?: "Internal"
                viewModel.setSelectedFolder(folderName)
            }
        }
        if (currentAudio == null) {
            currentAudio = globalAudios.find { it.uri.toString() == audioUri }
        }
    }

    var isPlaying by remember { mutableStateOf(controller?.isPlaying ?: false) }
    var currentPosition by remember { mutableStateOf(controller?.currentPosition ?: 0L) }
    var duration by remember { mutableStateOf(controller?.duration?.coerceAtLeast(0L) ?: 0L) }
    var repeatMode by remember { mutableStateOf(controller?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    var playbackSpeed by remember { mutableFloatStateOf(effectiveSpeed) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val artScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "artScale"
    )

    val albumArtUri = remember(currentAudio) {
        currentAudio?.let {
            ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                it.albumId
            )
        }
    }

    var hasSeekedInitialPosition by remember(audioUri, initialPosition) { mutableStateOf(false) }

    LaunchedEffect(controller, activeQueueAudios, audioUri, initialPosition, effectiveSpeed) {
        val player = controller ?: return@LaunchedEffect
        
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = player.duration.coerceAtLeast(0L)
            }
            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val currentUri = mediaItem?.localConfiguration?.uri?.toString()
                currentAudio = activeQueueAudios.find { it.uri.toString() == currentUri } ?: globalAudios.find { it.uri.toString() == currentUri }
                if (currentUri != null && currentUri != audioUri && MediaUtils.isVideoMediaItem(mediaItem)) {
                    onVideoTransition(currentUri)
                }
            }
        }
        player.addListener(listener)
        
        isPlaying = player.isPlaying
        duration = player.duration.coerceAtLeast(0L)
        repeatMode = player.repeatMode
        
        val targetPos = if (initialPosition >= 0L) initialPosition else viewModel.getSavedPosition(audioUri)
        val currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri != audioUri) {
            var matchedIndex = -1
            for (i in 0 until player.mediaItemCount) {
                if (player.getMediaItemAt(i).localConfiguration?.uri?.toString() == audioUri) {
                    matchedIndex = i
                    break
                }
            }
            if (matchedIndex != -1) {
                player.seekTo(matchedIndex, targetPos.coerceAtLeast(0L))
                player.play()
            } else {
                val mediaItems = activeQueueAudios.map { audioItem ->
                    val artUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        audioItem.albumId
                    )
                    androidx.media3.common.MediaItem.Builder()
                        .setUri(audioItem.uri)
                        .setMediaId(audioItem.id.toString())
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(audioItem.name)
                                .setArtist(audioItem.artist)
                                .setAlbumTitle(audioItem.album)
                                .setArtworkUri(artUri)
                                .build()
                        )
                        .build()
                }
                val index = activeQueueAudios.indexOfFirst { it.uri.toString() == audioUri }.coerceAtLeast(0)
                
                if (mediaItems.isNotEmpty()) {
                    player.setMediaItems(mediaItems, index, targetPos.coerceAtLeast(0L))
                    player.prepare()
                    player.play()
                }
            }
            hasSeekedInitialPosition = true
        } else {
            currentAudio = activeQueueAudios.find { it.uri.toString() == audioUri } ?: globalAudios.find { it.uri.toString() == audioUri }
            if (!hasSeekedInitialPosition && targetPos > 0L) {
                player.seekTo(targetPos)
                hasSeekedInitialPosition = true
            }
        }
        player.setPlaybackSpeed(effectiveSpeed)
        playbackSpeed = effectiveSpeed
        
        try {
            while (true) {
                if (player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED) {
                    currentPosition = player.currentPosition
                }
                delay(1000)
            }
        } catch (e: Exception) {
            // Player might have been released
        } finally {
            player.removeListener(listener)
        }
    }

    if (showEqualizerDialog) {
        AudioEqualizerDialog(
            onDismiss = { showEqualizerDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutes = if (sleepTimerActive && !stopAfterCurrent) (sleepTimerTimeLeft / 60000).toInt() else 0,
            stopAfterCurrent = stopAfterCurrent,
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onStopAfterCurrentToggle = { enabled ->
                viewModel.setStopAfterCurrent(enabled)
            }
        )
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showPlaylistDialog = false },
            onPlaylistSelected = { playlistId ->
                val audio = currentAudio
                if (audio != null) {
                    viewModel.addToPlaylist(playlistId, audio.uri.toString(), "audio")
                    Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                }
                showPlaylistDialog = false
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePlaybackProgress()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showQueueSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.queue), tint = Color.White)
                        }
                        IconButton(onClick = { showMoreOptions = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more), tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMoreOptions,
                            onDismissRequest = { showMoreOptions = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_add_to_playlist)) },
                                onClick = { 
                                    showMoreOptions = false
                                    showPlaylistDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { 
                                    val text = if (stopAfterCurrent) stringResource(R.string.sleep_timer_end_of_track)
                                              else if (sleepTimerActive) stringResource(R.string.sleep_timer_format, formatTime(sleepTimerTimeLeft)) 
                                              else stringResource(R.string.sleep_timer)
                                    Text(text)
                                },
                                onClick = { 
                                    showMoreOptions = false
                                    showSleepTimerDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = if (sleepTimerActive) MaterialTheme.colorScheme.primary else Color.White) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.playback_speed_format, playbackSpeed.toString())) },
                                onClick = { 
                                    val nextSpeed = when(playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        2.0f -> 0.75f
                                        else -> 1.0f
                                    }
                                    playbackSpeed = nextSpeed
                                    controller?.setPlaybackSpeed(nextSpeed)
                                    showMoreOptions = false
                                },
                                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) }
                            )
                                DropdownMenuItem(
                                text = { Text(stringResource(R.string.equalizer)) },
                                onClick = { 
                                    showMoreOptions = false
                                    showEqualizerDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_information)) },
                                onClick = { 
                                    showMoreOptions = false
                                    showInfoDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                onClick = { 
                                    showMoreOptions = false
                                    showDeleteDialog = true
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
        ) { padding ->
            if (controller == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val player = controller!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Album Art with Breathing Animation
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
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Default.MusicNote),
                            fallback = rememberVectorPainter(Icons.Default.MusicNote)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentAudio?.name ?: "Unknown Title",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }

                    // Waveform Visualizer
                    WaveformVisualizer(
                        isPlaying = isPlaying,
                        modifier = Modifier.padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column {
                        // Seek Bar
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { 
                                currentPosition = it.toLong()
                                player.seekTo(it.toLong())
                            },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val nextMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                            player.repeatMode = nextMode
                        }) {
                            Icon(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(onClick = { player.seekToPreviousMediaItem() }) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                        
                        FilledIconButton(
                            onClick = { if (isPlaying) player.pause() else player.play() },
                            modifier = Modifier.size(84.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        IconButton(onClick = { player.seekToNextMediaItem() }) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }

                        var isShuffle by remember { mutableStateOf(player.shuffleModeEnabled) }
                        IconButton(onClick = {
                            isShuffle = !isShuffle
                            player.shuffleModeEnabled = isShuffle
                        }) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        ) {
            Column(modifier = Modifier.fillMaxHeight(0.6f).padding(16.dp)) {
                Text(
                    stringResource(R.string.up_next),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    items(activeQueueAudios) { audio ->
                        val isCurrent = currentAudio?.id == audio.id
                        ListItem(
                            headlineContent = { 
                                Text(
                                    audio.name, 
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White
                                ) 
                            },
                            supportingContent = { Text(formatDuration(audio.duration), color = Color.Gray) },
                            leadingContent = {
                                AsyncImage(
                                    model = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), audio.albumId),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                    error = rememberVectorPainter(Icons.Default.MusicNote)
                                )
                            },
                            modifier = Modifier.clickable {
                                val idx = activeQueueAudios.indexOfFirst { it.id == audio.id }
                                if (idx != -1 && idx < (controller?.mediaItemCount ?: 0)) {
                                    controller?.seekToDefaultPosition(idx)
                                    controller?.play()
                                } else {
                                    viewModel.togglePlayPause(audio)
                                }
                                showQueueSheet = false
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog && currentAudio != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(R.string.menu_information)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoRow(stringResource(R.string.info_name), currentAudio!!.name)
                    InfoRow(stringResource(R.string.info_artist), currentAudio!!.artist)
                    InfoRow(stringResource(R.string.info_path), currentAudio!!.path)
                    InfoRow(stringResource(R.string.info_duration), formatDuration(currentAudio!!.duration))
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text(stringResource(R.string.action_close)) }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showDeleteDialog && currentAudio != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete_audio_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_media_confirm, currentAudio!!.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        val audio = currentAudio!!
                        showDeleteDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(audio.uri))
                            deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                        } else {
                            viewModel.stopIfPlaying(audio.uri)
                            context.contentResolver.delete(audio.uri, null, null)
                            viewModel.refreshAudios(context)
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
