package com.arslandaim.omegaplayer.ui.feature.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.arslandaim.omegaplayer.util.MediaUtils
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration
import com.arslandaim.omegaplayer.viewmodel.AudioViewModel
import com.arslandaim.omegaplayer.ui.common.SystemVolumeSyncEffect
import com.arslandaim.omegaplayer.ui.common.applySystemMusicVolume
import com.arslandaim.omegaplayer.ui.common.rememberInitialSystemBrightness
import com.arslandaim.omegaplayer.ui.common.rememberInitialSystemVolume
import com.arslandaim.omegaplayer.ui.common.rememberSystemAudioManager
import com.arslandaim.omegaplayer.ui.common.rememberSystemMaxVolume
import com.arslandaim.omegaplayer.ui.common.setBrightness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.activity.compose.BackHandler

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.VolumeUp

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
    val audioManager = rememberSystemAudioManager()
    val maxVolume = rememberSystemMaxVolume(audioManager)
    val initialVolume = rememberInitialSystemVolume(audioManager, maxVolume)
    var volume by remember { mutableFloatStateOf(initialVolume) }

    val initialBrightness = rememberInitialSystemBrightness()
    var brightness by remember { mutableFloatStateOf(initialBrightness) }

    SystemVolumeSyncEffect(
        audioManager = audioManager,
        volumeProvider = { volume },
        onVolumeChange = { volume = it },
        onVolumeBoostReset = { viewModel.eqManager.setVolumeBoostScale(1.0f) }
    )
    
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
        currentAudio?.let { MediaUtils.albumArtUri(it.albumId) }
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
        while (true) {
            if (player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED) {
                val pos = player.currentPosition
                val dur = player.duration.coerceAtLeast(0L)
                if (dur > 0) duration = dur
                if (!isDraggingSlider) {
                    if (pendingSeekPosition >= 0) {
                        if (System.currentTimeMillis() > seekGracePeriod || kotlin.math.abs(pos - pendingSeekPosition) < SEEK_TOLERANCE_MS) {
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
                    Toast.makeText(context, context.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
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
                        applySystemMusicVolume(audioManager, volume, maxVolume)
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
                AudioPlayerTopBar(
                    onBack = onBack,
                    showMoreOptions = showMoreOptions,
                    onMoreOptionsChange = { showMoreOptions = it },
                    stopAfterCurrent = stopAfterCurrent,
                    sleepTimerActive = sleepTimerActive,
                    sleepTimerTimeLeft = sleepTimerTimeLeft,
                    playbackSpeed = playbackSpeed,
                    onShowQueue = { showQueueSheet = true },
                    onAddToPlaylist = { showPlaylistDialog = true },
                    onSleepTimer = { showSleepTimerDialog = true },
                    onPlaybackSpeed = { showPlaybackSpeedDialog = true },
                    onEqualizer = { showEqualizerDialog = true },
                    onInfo = { showInfoDialog = true },
                    onDeleteRequested = {
                        if (currentAudio != null) {
                            pendingUrisToDelete = listOf(currentAudio!!.uri)
                            com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
                                context = context,
                                uris = listOf(currentAudio!!.uri),
                                deleteLauncher = deleteLauncher,
                                onRequireInternalPopup = { showDeleteDialog = true }
                            )
                        }
                    }
                )
            }
        ) { padding ->
            AudioPlayerContent(
                padding = padding,
                controller = controller,
                currentAudio = currentAudio,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                pendingSeekPosition = pendingSeekPosition,
                repeatMode = repeatMode,
                isShuffle = isShuffle,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                artScale = artScale,
                albumArtUri = albumArtUri,
                onSeekPreview = { fraction ->
                    isDraggingSlider = true
                    val target = (fraction * duration).toLong()
                    currentPosition = target
                    pendingSeekPosition = target
                },
                onSeekFinished = {
                    isDraggingSlider = false
                    val target = pendingSeekPosition.coerceAtLeast(0L)
                    seekGracePeriod = System.currentTimeMillis() + SEEK_TOLERANCE_MS
                    controller?.seekTo(target)
                },
                onPlayPauseClick = { if (isPlaying) controller?.pause() else controller?.play() },
                onCycleRepeatClick = {
                    val nextMode = when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    controller?.repeatMode = nextMode
                },
                onToggleShuffleClick = {
                    isShuffle = !isShuffle
                    controller?.shuffleModeEnabled = isShuffle
                }
            )
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
                    title = stringResource(R.string.unknown_title),
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
