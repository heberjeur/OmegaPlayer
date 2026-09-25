/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.ui.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.request.videoFrameMillis
import com.arslandaim.omegaplayer.viewmodel.AudioViewModel
import com.arslandaim.omegaplayer.ui.common.WaveformVisualizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import com.arslandaim.omegaplayer.data.AudioModel
import android.content.Context
import android.app.ActivityManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.graphicsLayer

private const val TAG = "AudioPlayerScreen"

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
                    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                    Slider(
                        value = minutes.toFloat(),
                        onValueChange = { minutes = it.toInt() },
                        valueRange = 0f..120f,
                        steps = 23,
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
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                sliderState = sliderState,
                                modifier = Modifier.height(2.dp)
                            )
                        }
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
    eqManager: com.arslandaim.omegaplayer.media.EqManager,
    onDismiss: () -> Unit
) {
    val enabled by eqManager.enabled.collectAsState()
    val bands by eqManager.bands.collectAsState()
    val presets by eqManager.presets.collectAsState()
    
    var selectedPreset by remember { mutableStateOf("Custom") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.audio_equalizer), fontWeight = FontWeight.Bold)
                }
                Switch(checked = enabled, onCheckedChange = { eqManager.setEnabled(it) })
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (presets.isNotEmpty()) {
                    Text(stringResource(R.string.presets), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presets) { presetName ->
                            val index = presets.indexOf(presetName).toShort()
                            FilterChip(
                                selected = selectedPreset == presetName,
                                onClick = {
                                    selectedPreset = presetName
                                    eqManager.usePreset(index)
                                },
                                label = { Text(presetName) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (bands.isNotEmpty()) {
                    bands.forEach { band ->
                        val freqStr = if (band.centerFreq >= 1000000) {
                            "${band.centerFreq / 1000000} kHz"
                        } else {
                            "${band.centerFreq / 1000} Hz"
                        }
                        Text(freqStr, style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = band.level.toFloat(),
                            valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                            onValueChange = { 
                                eqManager.setBandLevel(band.id, it.toInt().toShort())
                                selectedPreset = "Custom"
                            },
                            enabled = enabled
                        )
                    }
                } else {
                    Text(stringResource(R.string.error_not_supported), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
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
    from: String? = null,
    viewModel: AudioViewModel,
    onBack: () -> Unit,
    onVideoTransition: (String) -> Unit = {},
    onAudioTransition: (String) -> Unit = {},
    initialPosition: Long = -1L
) {
    var showMoreOptions by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    
    val isAnyDialogShown = showMoreOptions || showSleepTimerDialog || showPlaylistDialog || 
                           showQueueSheet || showDeleteDialog || showEqualizerDialog || showInfoDialog || showPlaybackSpeedDialog

    BackHandler(enabled = !isAnyDialogShown, onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val upNextFullyExpanded by viewModel.upNextFullyExpanded.collectAsStateWithLifecycle(initialValue = false)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = upNextFullyExpanded)

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val volumeBoostEnabled by viewModel.volumeBoostEnabled.collectAsStateWithLifecycle(initialValue = false)
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager }
    val maxVolume = remember(audioManager) { audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15 }
    val initialVolume = remember(audioManager, maxVolume) {
        val current = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)
        (current.toFloat() / maxVolume).coerceIn(0f, 1f)
    }
    var volume by remember { mutableFloatStateOf(initialVolume) }

    val initialBrightness = remember {
        val activity = context as? android.app.Activity
        val currentAttr = activity?.window?.attributes?.screenBrightness
        if (currentAttr != null && currentAttr >= 0) {
            currentAttr
        } else {
            try {
                val sysBrightness = android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS)
                (sysBrightness / 255f).coerceIn(0.01f, 1f)
            } catch (_: android.provider.Settings.SettingNotFoundException) {
                0.5f
            }
        }
    }
    var brightness by remember { mutableFloatStateOf(initialBrightness) }

    val updateVolumeFromSystem: () -> Unit = {
        audioManager?.let { am ->
            val cur = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            if (max > 0) {
                val expectedSysVol = kotlin.math.round(volume.coerceAtMost(1f) * max).toInt()
                if (cur != expectedSysVol) {
                    val newVol = (cur.toFloat() / max).coerceIn(0f, 1f)
                    if (volume > 1f && newVol < 1f) {
                        viewModel.eqManager.setVolumeBoostScale(1.0f)
                        volume = newVol
                    } else if (volume <= 1f) {
                        volume = newVol
                    }
                }
            }
        }
    }

    DisposableEffect(context, audioManager) {
        updateVolumeFromSystem()
        val contentObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                updateVolumeFromSystem()
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                contentObserver
            )
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to register volume observer", e)
        }
        
        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(contentObserver)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to unregister volume observer", e)
            }
        }
    }
    
    var isVolumeVisible by remember { mutableStateOf(false) }
    var isBrightnessVisible by remember { mutableStateOf(false) }

    val controller by viewModel.mediaController.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val sleepTimerActive by viewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val sleepTimerTimeLeft by viewModel.sleepTimerTimeLeft.collectAsStateWithLifecycle()
    val stopAfterCurrent by viewModel.stopAfterCurrent.collectAsStateWithLifecycle()
    val playerOrientation by viewModel.playerOrientation.collectAsStateWithLifecycle(initialValue = 0)
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    LaunchedEffect(playerOrientation) {
        activity?.requestedOrientation = if (playerOrientation == 0) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER else if (playerOrientation == 1) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
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

    var pendingUrisToDelete by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingUrisToDelete.forEach { uri -> viewModel.deleteHistoryItem(uri.toString()) }
            val autoPlayNext = viewModel.autoPlayNext.value
            val hasNext = controller?.hasNextMediaItem() == true
            if (hasNext && autoPlayNext) {
                val currentIndex = controller?.currentMediaItemIndex ?: -1
                if (currentIndex != -1) {
                    controller?.removeMediaItem(currentIndex)
                }
            } else {
                currentAudio?.let { viewModel.stopIfPlaying(it.uri) }
                onBack()
            }
            if (pendingUrisToDelete.isNotEmpty()) viewModel.onAudiosDeleted(pendingUrisToDelete)
            pendingUrisToDelete = emptyList()
        } else {
            pendingUrisToDelete = emptyList()
        }
    }

    val currentFolder = remember(currentAudio, audioUri, globalAudios) {
        val path = currentAudio?.path ?: globalAudios.find { it.uri.toString() == audioUri }?.path
        if (!path.isNullOrBlank()) {
            java.io.File(path).parentFile?.absolutePath ?: ""
        } else {
            ""
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
    val activeQueue by viewModel.activeQueue.collectAsStateWithLifecycle()

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
    var isShuffle by remember { mutableStateOf(controller?.shuffleModeEnabled ?: false) }
    var currentMediaIndexState by remember { mutableIntStateOf(controller?.currentMediaItemIndex ?: 0) }
    var playbackSpeed by remember { mutableFloatStateOf(effectiveSpeed) }
    var hasNext by remember { mutableStateOf(controller?.hasNextMediaItem() ?: false) }
    var hasPrevious by remember { mutableStateOf(controller?.hasPreviousMediaItem() ?: false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var pendingSeekPosition by remember { mutableLongStateOf(-1L) }
    var seekGracePeriod by remember { mutableLongStateOf(0L) }
    var currentUriState by remember { mutableStateOf(audioUri) }

    LaunchedEffect(audioUri) {
        currentUriState = audioUri
    }

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

    var hasSeekedInitialPosition by remember(currentUriState, initialPosition) { mutableStateOf(false) }

    LaunchedEffect(currentMediaIndexState) {
        controller?.let { player ->
            if (playbackSpeed != effectiveSpeed) {
                playbackSpeed = effectiveSpeed
                player.setPlaybackSpeed(effectiveSpeed)
            }
        }
    }
    DisposableEffect(controller, activeQueueAudios, globalAudios, effectiveSpeed) {
        val player = controller ?: return@DisposableEffect onDispose {}
        
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
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                isShuffle = shuffleModeEnabled
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                currentMediaIndexState = player.currentMediaItemIndex
                val currentUri = mediaItem?.localConfiguration?.uri?.toString()
                if (currentUri != null) {
                    currentUriState = currentUri
                    currentAudio = activeQueueAudios.find { it.uri.toString() == currentUri } ?: globalAudios.find { it.uri.toString() == currentUri }
                    if (currentUri != audioUri && MediaUtils.isVideoMediaItem(mediaItem)) {
                        onVideoTransition(currentUri)
                    }
                }
            }
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) || events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                    hasNext = player.hasNextMediaItem()
                    hasPrevious = player.hasPreviousMediaItem()
                }
            }
        }
        player.addListener(listener)
        
        isPlaying = player.isPlaying
        duration = player.duration.coerceAtLeast(0L)
        repeatMode = player.repeatMode
        isShuffle = player.shuffleModeEnabled
        player.setPlaybackSpeed(effectiveSpeed)
        playbackSpeed = effectiveSpeed
        
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(controller, currentUriState, initialPosition) {
        val player = controller ?: return@LaunchedEffect
        val targetPos = if (initialPosition >= 0L) initialPosition else viewModel.getSavedPosition(currentUriState)
        val currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
        
        if (from == "intent" || from == null) {
            if (currentUri != currentUriState) {
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(androidx.media3.common.MediaItem.fromUri(currentUriState))
                player.prepare()
                if (targetPos > 0L) {
                    player.seekTo(targetPos)
                }
                player.play()
                hasSeekedInitialPosition = true
            } else {
                currentAudio = activeQueueAudios.find { it.uri.toString() == currentUriState } ?: globalAudios.find { it.uri.toString() == currentUriState }
                if (!hasSeekedInitialPosition && targetPos > 0L) {
                    player.seekTo(targetPos)
                    hasSeekedInitialPosition = true
                }
            }
        } else {
            currentAudio = activeQueueAudios.find { it.uri.toString() == currentUriState } ?: globalAudios.find { it.uri.toString() == currentUriState }
        }
    }

    LaunchedEffect(controller) {
        val player = controller ?: return@LaunchedEffect
        try {
            while (true) {
                if (player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED) {
                    val pos = player.currentPosition
                    val dur = player.duration.coerceAtLeast(0L)
                    if (dur > 0) duration = dur
                    if (!isDraggingSlider) {
                        if (pendingSeekPosition >= 0) {
                            if (System.currentTimeMillis() > seekGracePeriod || kotlin.math.abs(pos - pendingSeekPosition) < 2000L) {
                                pendingSeekPosition = -1L
                                currentPosition = pos
                            }
                        } else {
                            currentPosition = pos
                        }
                    }
                }
                delay(200)
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Playback position polling stopped", e)
        }
    }

    if (showEqualizerDialog) {
        AudioEqualizerDialog(
            eqManager = viewModel.eqManager,
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

    if (showPlaybackSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = playbackSpeed,
            onDismiss = { showPlaybackSpeedDialog = false },
            onConfirm = { speed, applyTemporarily ->
                playbackSpeed = speed
                controller?.setPlaybackSpeed(speed)
                if (!applyTemporarily) {
                    if (speedScope == com.arslandaim.omegaplayer.data.PlaybackSpeedScope.GLOBAL) {
                        viewModel.setGlobalPlaybackSpeed(speed)
                    } else if (selectedFolder != null) {
                        viewModel.setFolderPlaybackSpeed(selectedFolder!!, speed)
                    }
                }
                showPlaybackSpeedDialog = false
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
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        isVolumeVisible = false
                        isBrightnessVisible = false
                    },
                    onDragCancel = {
                        isVolumeVisible = false
                        isBrightnessVisible = false
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val isRightSide = change.position.x > size.width / 2
                    if (isRightSide) {
                        val oldVolume = volume
                        val maxVolMultiplier = if (volumeBoostEnabled) 2f else 1f
                        volume = (volume - dragAmount.y / size.height).coerceIn(0f, maxVolMultiplier)
                        if (kotlin.math.abs(volume - oldVolume) > 0.05f) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                        audioManager?.let { am ->
                            val targetVol = kotlin.math.round(volume.coerceAtMost(1f) * maxVolume).toInt()
                            am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVol, 0)
                        }
                        if (volumeBoostEnabled) {
                            viewModel.eqManager.setVolumeBoostScale(if (volume > 1f) volume else 1.0f)
                        }
                        isVolumeVisible = true
                    } else {
                        val oldBrightness = brightness
                        brightness = (brightness - dragAmount.y / size.height).coerceIn(0.01f, 1f)
                        if (kotlin.math.abs(brightness - oldBrightness) > 0.05f) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                        setBrightness(context, brightness)
                        isBrightnessVisible = true
                    }
                }
            }
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
                                              else if (sleepTimerActive) stringResource(R.string.sleep_timer_format, formatDuration(sleepTimerTimeLeft)) 
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
                                text = { Text(stringResource(R.string.playback_speed_format, String.format(java.util.Locale.US, "%.2f", playbackSpeed))) },
                                onClick = { 
                                    showMoreOptions = false
                                    showPlaybackSpeedDialog = true
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
                                    if (currentAudio != null) {
                                        pendingUrisToDelete = listOf(currentAudio!!.uri)
                                        com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
                                            context = context,
                                            uris = listOf(currentAudio!!.uri),
                                            deleteLauncher = deleteLauncher,
                                            onRequireInternalPopup = { showDeleteDialog = true }
                                        )
                                    }
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
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                
                val mediaInfo: @Composable () -> Unit = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentAudio?.name ?: "Unknown Title",
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
                            onValueChange = { fraction ->
                                isDraggingSlider = true
                                val target = (fraction * duration).toLong()
                                currentPosition = target
                                pendingSeekPosition = target
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                val target = pendingSeekPosition.coerceAtLeast(0L)
                                seekGracePeriod = System.currentTimeMillis() + 2000L
                                player.seekTo(target)
                            },
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

                        IconButton(onClick = { player.seekToPrevious() }, enabled = hasPrevious) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(36.dp),
                                tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f)
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

                        IconButton(onClick = { player.seekToNext() }, enabled = hasNext) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(36.dp),
                                tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }

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
                                contentDescription = "Album Art",
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

        VerticalIndicator(
            value = volume / if (volumeBoostEnabled) 2f else 1f,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            visible = isVolumeVisible,
            text = (volume * 100).toInt().toString(),
            color = if (volume > 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        
        VerticalIndicator(
            value = brightness,
            icon = Icons.Default.BrightnessMedium,
            visible = isBrightnessVisible,
            text = (brightness * 100).toInt().toString(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }

    if (showQueueSheet) {
        val queueItems = if (activeQueue.isNotEmpty()) {
            activeQueue
        } else if (activeQueueAudios.isNotEmpty()) {
            activeQueueAudios.map {
                com.arslandaim.omegaplayer.media.PlaybackQueueItem(
                    uri = it.uri.toString(),
                    title = it.name,
                    duration = it.duration,
                    isVideo = false,
                    artist = it.artist,
                    albumId = it.albumId
                )
            }
        } else {
            listOf(
                com.arslandaim.omegaplayer.media.PlaybackQueueItem(
                    uri = audioUri,
                    title = "Unknown",
                    duration = 0L,
                    isVideo = false
                )
            )
        }

        com.arslandaim.omegaplayer.ui.feature.player.PlaybackQueueSheet(
            queueItems = queueItems,
            currentMediaIndex = currentMediaIndexState,
            sheetState = sheetState,
            onDismiss = { showQueueSheet = false },
            onItemClick = { index ->
                showQueueSheet = false
                val item = queueItems[index]
                val isCurrent = index == currentMediaIndexState
                if (!isCurrent) {
                    val idx = queueItems.indexOfFirst { it.uri == item.uri }
                    if (idx != -1 && idx < (controller?.mediaItemCount ?: 0)) {
                        controller?.seekToDefaultPosition(idx)
                        controller?.play()
                    } else if (item.isVideo) {
                        onVideoTransition(item.uri)
                    } else {
                        onAudioTransition(item.uri)
                    }
                }
            }
        )
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
                        scope.launch {
                            viewModel.deleteHistoryItem(audio.uri.toString())
                            val deleted = withContext(Dispatchers.IO) {
                                context.contentResolver.delete(audio.uri, null, null)
                            }
                        
                            val autoPlayNext = viewModel.autoPlayNext.value
                            val hasNext = controller?.hasNextMediaItem() == true
                            if (hasNext && autoPlayNext) {
                                val currentIndex = controller?.currentMediaItemIndex ?: -1
                                if (currentIndex != -1) {
                                    controller?.removeMediaItem(currentIndex)
                                }
                            } else {
                                viewModel.stopIfPlaying(audio.uri)
                                onBack()
                            }
                        
                            if (deleted > 0) viewModel.onAudiosDeleted(listOf(audio.uri))
                            else viewModel.refreshAudios(context)
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
