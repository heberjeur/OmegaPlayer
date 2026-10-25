package com.mohamedaminelouati.oplayer.ui.feature.player

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
import com.mohamedaminelouati.oplayer.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.mohamedaminelouati.oplayer.util.MediaUtils
import com.mohamedaminelouati.oplayer.viewmodel.AudioViewModel
import com.mohamedaminelouati.oplayer.media.PlaybackSource
import java.io.File
import com.mohamedaminelouati.oplayer.ui.common.SystemVolumeSyncEffect
import com.mohamedaminelouati.oplayer.ui.common.applySystemMusicVolume
import com.mohamedaminelouati.oplayer.ui.common.rememberInitialSystemBrightness
import com.mohamedaminelouati.oplayer.ui.common.rememberInitialSystemVolume
import com.mohamedaminelouati.oplayer.ui.common.rememberSystemAudioManager
import com.mohamedaminelouati.oplayer.ui.common.rememberSystemMaxVolume
import com.mohamedaminelouati.oplayer.ui.common.setBrightness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.activity.compose.BackHandler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    var targetPlaylistUri by remember { mutableStateOf<String?>(null) }
    var targetPlaylistUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    
    val isAnyDialogShown = showMoreOptions || showSleepTimerDialog || showPlaylistDialog || 
                           showQueueSheet || showDeleteDialog || showEqualizerDialog || showInfoDialog || showPlaybackSpeedDialog

    BackHandler(enabled = !isAnyDialogShown, onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val upNextFullyExpanded by viewModel.upNextFullyExpanded.collectAsStateWithLifecycle(initialValue = true)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = upNextFullyExpanded)

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val volumeBoostEnabled by viewModel.volumeBoostEnabled.collectAsStateWithLifecycle(initialValue = false)
    val skipSilenceEnabled by viewModel.skipSilenceEnabled.collectAsStateWithLifecycle(initialValue = false)
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
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val sleepTimerActive by viewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val sleepTimerTimeLeft by viewModel.sleepTimerTimeLeft.collectAsStateWithLifecycle()
    val stopAfterCurrent by viewModel.stopAfterCurrent.collectAsStateWithLifecycle()
    val savePositionOnQuit by viewModel.savePositionOnQuit.collectAsStateWithLifecycle(initialValue = true)
    val closeAfterQueueEnds by viewModel.closeAfterQueueEnds.collectAsStateWithLifecycle(initialValue = false)
    val showSystemStatusBar by viewModel.showSystemStatusBar.collectAsStateWithLifecycle(initialValue = true)
    val hudShowClock by viewModel.showPlayerClock.collectAsStateWithLifecycle(initialValue = false)
    val hudShowBattery by viewModel.showPlayerBattery.collectAsStateWithLifecycle(initialValue = false)
    val hudShowMediaInfo by viewModel.showPlayerMediaInfo.collectAsStateWithLifecycle(initialValue = false)
    val hudShowVolume by viewModel.showPlayerVolume.collectAsStateWithLifecycle(initialValue = false)
    val hudShowBrightness by viewModel.showPlayerBrightness.collectAsStateWithLifecycle(initialValue = false)
    val playerOrientation by viewModel.playerOrientation.collectAsStateWithLifecycle(initialValue = 0)
    val keepScreenOnWhenPaused by viewModel.keepScreenOnWhenPaused.collectAsStateWithLifecycle(initialValue = false)
    val brightnessScope by viewModel.brightnessScope.collectAsStateWithLifecycle(initialValue = com.mohamedaminelouati.oplayer.data.BrightnessScope.NONE)
    val globalBrightness by viewModel.globalBrightness.collectAsStateWithLifecycle(initialValue = -1f)
    val controlsTimeout by viewModel.controlsTimeout.collectAsStateWithLifecycle(initialValue = 3)
    val gestureVolumeEnabled by viewModel.gestureVolumeEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gestureBrightnessEnabled by viewModel.gestureBrightnessEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gestureSeekForwardEnabled by viewModel.gestureSeekForwardEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gestureSeekBackwardEnabled by viewModel.gestureSeekBackwardEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gesturePlayPauseEnabled by viewModel.gesturePlayPauseEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gestureTapControlsEnabled by viewModel.gestureTapControlsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val seekIncrement by viewModel.seekIncrement.collectAsStateWithLifecycle(initialValue = 10)
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()

    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeString = format.format(Date())
            delay(1000)
        }
    }

    var batteryPercentage by remember { mutableIntStateOf(100) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryPercentage = (level * 100) / scale
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    DisposableEffect(showSystemStatusBar) {
        val window = activity?.window
        if (window != null) {
            val windowController = WindowCompat.getInsetsController(window, view)
            windowController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (showSystemStatusBar) {
                windowController.hide(WindowInsetsCompat.Type.navigationBars())
                windowController.show(WindowInsetsCompat.Type.statusBars())
            } else {
                windowController.hide(WindowInsetsCompat.Type.systemBars())
            }
            windowController.isAppearanceLightStatusBars = false
            windowController.isAppearanceLightNavigationBars = false
        }
        
        onDispose {
            val windowController = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
            windowController?.show(WindowInsetsCompat.Type.systemBars())
            windowController?.isAppearanceLightStatusBars = !isDarkTheme
            windowController?.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    LaunchedEffect(playerOrientation) {
        activity?.requestedOrientation = if (playerOrientation == 0) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER else if (playerOrientation == 1) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    val globalAudios by viewModel.audios.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val audios by viewModel.audiosInSelectedFolder.collectAsStateWithLifecycle()
    
    val speedScope by viewModel.speedScope.collectAsStateWithLifecycle()
    val globalSpeed by viewModel.globalPlaybackSpeed.collectAsStateWithLifecycle()

    var currentAudio by remember { mutableStateOf(globalAudios.find { it.uri.toString() == audioUri }) }

    val playbackSource by viewModel.playbackSource.collectAsStateWithLifecycle(initialValue = com.mohamedaminelouati.oplayer.media.PlaybackSource.None)
    val isHistory = com.mohamedaminelouati.oplayer.media.PlaybackSpeedResolver.isHistorySource(from, playbackSource)
    val playlistId = com.mohamedaminelouati.oplayer.media.PlaybackSpeedResolver.resolvePlaylistId(from, playbackSource)

    val currentAudioFolder = remember(currentAudio, selectedFolder) {
        selectedFolder ?: currentAudio?.path?.let { File(it).parentFile?.absolutePath } ?: ""
    }
    val folderSpeedFlow = remember(currentAudioFolder) {
        if (currentAudioFolder.isNotEmpty()) viewModel.getFolderSpeed(currentAudioFolder) else flowOf(null)
    }
    val folderSpeed by folderSpeedFlow.collectAsStateWithLifecycle(initialValue = null)

    val historySpeed by viewModel.getHistorySpeed().collectAsStateWithLifecycle(initialValue = globalSpeed)
    val playlistSpeedFlow = remember(playlistId) {
        if (playlistId != null) viewModel.getPlaylistSpeed(playlistId) else flowOf(null)
    }
    val playlistSpeed by playlistSpeedFlow.collectAsStateWithLifecycle(initialValue = null)

    val effectiveSpeed = com.mohamedaminelouati.oplayer.media.PlaybackSpeedResolver.resolveEffectiveSpeed(
        scope = speedScope,
        globalSpeed = globalSpeed,
        isHistory = isHistory,
        historySpeed = historySpeed,
        playlistId = playlistId,
        playlistSpeed = playlistSpeed,
        folderSpeed = folderSpeed ?: globalSpeed
    )

    val folderBrightnessFlow = remember(currentAudioFolder) {
        if (currentAudioFolder.isNotEmpty()) viewModel.getFolderBrightness(currentAudioFolder) else flowOf(null)
    }
    val folderBrightness by folderBrightnessFlow.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(brightnessScope, globalBrightness, folderBrightness) {
        val targetBrightness = when (brightnessScope) {
            com.mohamedaminelouati.oplayer.data.BrightnessScope.GLOBAL -> if (globalBrightness > 0f) globalBrightness else null
            com.mohamedaminelouati.oplayer.data.BrightnessScope.PER_FOLDER -> if ((folderBrightness ?: -1f) > 0f) folderBrightness else null
            com.mohamedaminelouati.oplayer.data.BrightnessScope.NONE -> null
        }
        if (targetBrightness != null && targetBrightness > 0f) {
            brightness = targetBrightness
            setBrightness(context, targetBrightness)
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
    val queueItems = remember(activeQueue, activeQueueAudios, audioUri) {
        if (activeQueue.isNotEmpty()) {
            activeQueue
        } else if (activeQueueAudios.isNotEmpty()) {
            activeQueueAudios.map {
                com.mohamedaminelouati.oplayer.media.PlaybackQueueItem(
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
                com.mohamedaminelouati.oplayer.media.PlaybackQueueItem(
                    uri = audioUri,
                    title = "",
                    duration = 0L,
                    isVideo = false
                )
            )
        }
    }

    var pendingUrisToDelete by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var queueItemsPendingDelete by remember { mutableStateOf<List<com.mohamedaminelouati.oplayer.media.PlaybackQueueItem>>(emptyList()) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val deletedUriStrings = pendingUrisToDelete.map { it.toString() }.toSet()
            deletedUriStrings.forEach { uri -> viewModel.deleteHistoryItem(uri) }
            val currentTarget = currentAudio?.uri?.toString() ?: audioUri
            val isCurrent = deletedUriStrings.contains(currentTarget)
            val hasRemainingItems = queueItems.any { !deletedUriStrings.contains(it.uri) }
            if (isCurrent) {
                val autoPlayNext = viewModel.autoPlayNext.value
                if (hasRemainingItems && autoPlayNext) {
                    viewModel.removeQueueItems(deletedUriStrings)
                } else {
                    currentAudio?.let { viewModel.stopIfPlaying(it.uri) }
                    viewModel.removeQueueItems(deletedUriStrings)
                    onBack()
                }
            } else {
                viewModel.removeQueueItems(deletedUriStrings)
            }
            if (pendingUrisToDelete.isNotEmpty()) viewModel.onAudiosDeleted(pendingUrisToDelete)
            pendingUrisToDelete = emptyList()
            queueItemsPendingDelete = emptyList()
        } else {
            pendingUrisToDelete = emptyList()
            queueItemsPendingDelete = emptyList()
        }
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

    LaunchedEffect(isPlaying, keepScreenOnWhenPaused) {
        activity?.window?.let { window ->
            if (isPlaying || keepScreenOnWhenPaused) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    var activeCenterAction by remember { mutableStateOf(CenterActionType.NONE) }
    var centerActionTrigger by remember { mutableLongStateOf(0L) }
    var playPausePulse by remember { mutableIntStateOf(0) }
    var seekBackwardPulse by remember { mutableIntStateOf(0) }
    var seekForwardPulse by remember { mutableIntStateOf(0) }
    var consumedSeekBackwardPulse by remember { mutableIntStateOf(0) }
    var consumedSeekForwardPulse by remember { mutableIntStateOf(0) }
    var seekStreak by remember { mutableIntStateOf(0) }
    var lastSeekTimestamp by remember { mutableLongStateOf(0L) }
    var lastSeekDirection by remember { mutableIntStateOf(0) }
    var lastSeekAmountSeconds by remember { mutableIntStateOf(10) }

    val performSeek: (Int) -> Unit = performSeek@{ direction ->
        val player = controller ?: return@performSeek
        val now = System.currentTimeMillis()
        if (lastSeekDirection == direction && now - lastSeekTimestamp < 1200L) {
            seekStreak = (seekStreak + 1).coerceAtMost(5)
        } else {
            seekStreak = 0
        }
        lastSeekDirection = direction
        lastSeekTimestamp = now

        val multiplier = 1 shl seekStreak
        val jumpSeconds = seekIncrement * multiplier
        lastSeekAmountSeconds = jumpSeconds
        val jumpMs = jumpSeconds * 1000L

        if (direction < 0) {
            val target = (player.currentPosition - jumpMs).coerceAtLeast(0L)
            seekGracePeriod = now + SEEK_TOLERANCE_MS
            player.seekTo(target)
            seekBackwardPulse++
            activeCenterAction = CenterActionType.REWIND
        } else {
            val dur = player.duration.coerceAtLeast(0L)
            val target = if (dur > 0L) (player.currentPosition + jumpMs).coerceAtMost(dur) else player.currentPosition + jumpMs
            seekGracePeriod = now + SEEK_TOLERANCE_MS
            player.seekTo(target)
            seekForwardPulse++
            activeCenterAction = CenterActionType.FORWARD
        }
        centerActionTrigger = now
    }

    LaunchedEffect(centerActionTrigger) {
        if (centerActionTrigger > 0L) {
            delay(850L)
            activeCenterAction = CenterActionType.NONE
        }
    }

    var hasSeekedInitialPosition by remember(currentUriState, initialPosition) { mutableStateOf(false) }

    LaunchedEffect(currentMediaIndexState, effectiveSpeed) {
        controller?.let { player ->
            if (playbackSpeed != effectiveSpeed) {
                playbackSpeed = effectiveSpeed
                player.setPlaybackSpeed(effectiveSpeed)
            }
        }
    }
    val currentCloseAfterQueueEnds by rememberUpdatedState(closeAfterQueueEnds)
    val currentOnBack by rememberUpdatedState(onBack)
    var playbackState by remember { mutableIntStateOf(controller?.playbackState ?: Player.STATE_IDLE) }

    LaunchedEffect(playbackState, closeAfterQueueEnds) {
        if (playbackState == Player.STATE_ENDED && closeAfterQueueEnds) {
            onBack()
        }
    }

    DisposableEffect(controller, activeQueueAudios, globalAudios) {
        val player = controller ?: return@DisposableEffect onDispose {}
        
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                duration = player.duration.coerceAtLeast(0L)
                if (state == Player.STATE_ENDED && currentCloseAfterQueueEnds) {
                    currentOnBack()
                }
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
                    currentMediaIndexState = player.currentMediaItemIndex
                    hasNext = player.hasNextMediaItem()
                    hasPrevious = player.hasPreviousMediaItem()
                }
            }
        }
        player.addListener(listener)
        
        isPlaying = player.isPlaying
        duration = player.duration.coerceAtLeast(0L)
        isShuffle = player.shuffleModeEnabled
        player.setPlaybackSpeed(effectiveSpeed)
        playbackSpeed = effectiveSpeed
        
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(controller, currentUriState, initialPosition) {
        val player = controller ?: return@LaunchedEffect
        val targetPos = if (initialPosition >= 0L) {
            initialPosition
        } else if (savePositionOnQuit) {
            viewModel.getSavedPosition(currentUriState)
        } else 0L
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
                if (!hasSeekedInitialPosition && initialPosition >= 0L) {
                    player.seekTo(initialPosition)
                    hasSeekedInitialPosition = true
                }
            }
        } else {
            currentAudio = activeQueueAudios.find { it.uri.toString() == currentUriState } ?: globalAudios.find { it.uri.toString() == currentUriState }
            if (currentUri == currentUriState) {
                if (!hasSeekedInitialPosition && initialPosition >= 0L) {
                    player.seekTo(initialPosition)
                    hasSeekedInitialPosition = true
                }
            } else {
                if (!hasSeekedInitialPosition && targetPos > 0L) {
                    player.seekTo(targetPos)
                    hasSeekedInitialPosition = true
                }
            }
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
            skipSilenceEnabled = skipSilenceEnabled,
            onDismiss = { showPlaybackSpeedDialog = false },
            onConfirm = { speed, applyTemporarily, skipSilence ->
                playbackSpeed = speed
                controller?.setPlaybackSpeed(speed)
                viewModel.setSkipSilenceEnabled(skipSilence, persist = !applyTemporarily)
                if (!applyTemporarily) {
                    when {
                        speedScope == com.mohamedaminelouati.oplayer.data.PlaybackSpeedScope.GLOBAL -> viewModel.setGlobalPlaybackSpeed(speed)
                        isHistory -> viewModel.setHistoryPlaybackSpeed(speed)
                        playlistId != null -> viewModel.setPlaylistPlaybackSpeed(playlistId, speed)
                        else -> if (currentAudioFolder.isNotEmpty()) viewModel.setFolderPlaybackSpeed(currentAudioFolder, speed)
                    }
                }
                showPlaybackSpeedDialog = false
            }
        )
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = {
                showPlaylistDialog = false
                targetPlaylistUri = null
                targetPlaylistUris = emptyList()
            },
            onPlaylistSelected = { playlistId ->
                val urisToAdd = if (targetPlaylistUris.isNotEmpty()) {
                    targetPlaylistUris
                } else {
                    listOfNotNull(targetPlaylistUri ?: currentAudio?.uri?.toString())
                }
                urisToAdd.forEach { uri ->
                    viewModel.addToPlaylist(playlistId, uri, "audio")
                }
                if (urisToAdd.isNotEmpty()) {
                    Toast.makeText(context, context.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                }
                showPlaylistDialog = false
                targetPlaylistUri = null
                targetPlaylistUris = emptyList()
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            if (savePositionOnQuit) {
                viewModel.savePlaybackProgress()
            }
        }
    }

    var activeDragType by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (offset.x < size.width * 0.25f && gestureBrightnessEnabled) {
                            activeDragType = 1
                        } else if (offset.x > size.width * 0.75f && gestureVolumeEnabled) {
                            activeDragType = 2
                        } else {
                            activeDragType = 0
                        }
                    },
                    onDragEnd = {
                        if (activeDragType == 1) {
                            when (brightnessScope) {
                                com.mohamedaminelouati.oplayer.data.BrightnessScope.GLOBAL -> viewModel.saveGlobalBrightness(brightness)
                                com.mohamedaminelouati.oplayer.data.BrightnessScope.PER_FOLDER -> if (currentAudioFolder.isNotEmpty()) viewModel.saveFolderBrightness(currentAudioFolder, brightness)
                                com.mohamedaminelouati.oplayer.data.BrightnessScope.NONE -> {}
                            }
                        }
                        activeDragType = 0
                        isVolumeVisible = false
                        isBrightnessVisible = false
                    },
                    onDragCancel = {
                        activeDragType = 0
                        isVolumeVisible = false
                        isBrightnessVisible = false
                    }
                ) { change, dragAmount ->
                    if (activeDragType == 2) {
                        change.consume()
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
                    } else if (activeDragType == 1) {
                        change.consume()
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
                Column(modifier = Modifier.fillMaxWidth()) {
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
                                com.mohamedaminelouati.oplayer.util.MediaUtils.requestMediaDelete(
                                    context = context,
                                    uris = listOf(currentAudio!!.uri),
                                    deleteLauncher = deleteLauncher,
                                    onRequireInternalPopup = { showDeleteDialog = true }
                                )
                            }
                        }
                    )
                    PlayerTopHUD(
                        showClock = hudShowClock,
                        currentTime = currentTimeString,
                        showBattery = hudShowBattery,
                        batteryPercentage = batteryPercentage,
                        showVolume = hudShowVolume,
                        volumePercentage = (volume * 100).toInt(),
                        showBrightness = hudShowBrightness,
                        brightnessPercentage = (brightness * 100).toInt(),
                        showMediaInfo = hudShowMediaInfo,
                        resolution = currentAudio?.let { audio ->
                            val ext = audio.name.substringAfterLast('.', "").uppercase()
                            if (ext.isNotEmpty()) ext else "AUDIO"
                        } ?: "AUDIO",
                        mediaIcon = Icons.Default.MusicNote
                    )
                }
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
                onCycleRepeatClick = { viewModel.cycleRepeatMode() },
                onToggleShuffleClick = {
                    isShuffle = !isShuffle
                    controller?.shuffleModeEnabled = isShuffle
                },
                activeCenterAction = activeCenterAction,
                playPausePulse = playPausePulse,
                seekBackwardPulse = seekBackwardPulse,
                seekForwardPulse = seekForwardPulse,
                consumedSeekBackwardPulse = consumedSeekBackwardPulse,
                consumedSeekForwardPulse = consumedSeekForwardPulse,
                onConsumeSeekBackward = { consumedSeekBackwardPulse = it },
                onConsumeSeekForward = { consumedSeekForwardPulse = it },
                lastSeekAmountSeconds = lastSeekAmountSeconds,
                onRewindClick = { if (gestureSeekBackwardEnabled) performSeek(-1) },
                onForwardClick = { if (gestureSeekForwardEnabled) performSeek(1) },
                onPlayPauseCenterClick = {
                    if (gesturePlayPauseEnabled) {
                        if (isPlaying) controller?.pause() else controller?.play()
                        playPausePulse++
                        activeCenterAction = CenterActionType.PLAY_PAUSE
                        centerActionTrigger = System.currentTimeMillis()
                    }
                },
                controlsTimeout = controlsTimeout,
                gestureTapControlsEnabled = gestureTapControlsEnabled
            )
        }

        VerticalIndicator(
            value = volume / if (volumeBoostEnabled) 2f else 1f,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            visible = isVolumeVisible,
            text = (volume * 100).toInt().toString(),
            color = if (volume > 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
        
        VerticalIndicator(
            value = brightness,
            icon = Icons.Default.BrightnessMedium,
            visible = isBrightnessVisible,
            text = (brightness * 100).toInt().toString(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )
    }

    if (showQueueSheet) {
        com.mohamedaminelouati.oplayer.ui.feature.player.PlaybackQueueSheet(
            queueItems = queueItems,
            currentMediaIndex = currentMediaIndexState,
            isPlaying = isPlaying,
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
                } else {
                    if (controller?.isPlaying != true) {
                        controller?.play()
                    }
                }
            },
            onItemRemove = { index ->
                viewModel.removeQueueItem(index, queueItems)
            },
            onItemsRemove = { items ->
                viewModel.removeQueueItems(items.map { it.uri })
            },
            onItemMove = { from, to ->
                viewModel.moveQueueItem(from, to, queueItems)
            },
            onItemAddToPlaylist = { item ->
                targetPlaylistUri = item.uri
                targetPlaylistUris = listOf(item.uri)
                showPlaylistDialog = true
            },
            onItemsAddToPlaylist = { items ->
                targetPlaylistUris = items.map { it.uri }
                showPlaylistDialog = true
            },
            onItemDeleteFromDevice = { item ->
                val uri = Uri.parse(item.uri)
                pendingUrisToDelete = listOf(uri)
                com.mohamedaminelouati.oplayer.util.MediaUtils.requestMediaDelete(
                    context = context,
                    uris = listOf(uri),
                    deleteLauncher = deleteLauncher,
                    onRequireInternalPopup = { queueItemsPendingDelete = listOf(item) }
                )
            },
            onItemsDeleteFromDevice = { items ->
                val uris = items.map { Uri.parse(it.uri) }
                pendingUrisToDelete = uris
                com.mohamedaminelouati.oplayer.util.MediaUtils.requestMediaDelete(
                    context = context,
                    uris = uris,
                    deleteLauncher = deleteLauncher,
                    onRequireInternalPopup = { queueItemsPendingDelete = items }
                )
            },
            isFullyExpanded = upNextFullyExpanded
        )
    }

    var detailedInfo by remember { mutableStateOf<com.mohamedaminelouati.oplayer.util.DetailedMediaInfo?>(null) }
    
    LaunchedEffect(showInfoDialog, currentAudio) {
        if (showInfoDialog && currentAudio != null) {
            val audio = currentAudio!!
            val displaySize = java.io.File(audio.path).let { if (it.exists()) it.length() else 0L }
            detailedInfo = com.mohamedaminelouati.oplayer.util.extractDetailedMediaInfo(
                context = context,
                uri = audio.uri,
                fileName = audio.name,
                path = audio.path,
                sizeBytes = displaySize,
                durationMs = audio.duration
            )
        } else {
            detailedInfo = null
        }
    }

    if (showInfoDialog && detailedInfo != null) {
        com.mohamedaminelouati.oplayer.ui.feature.library.components.MediaInfoDialog(
            info = detailedInfo!!,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showDeleteDialog && currentAudio != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.primary) },
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.delete_from_device), color = Color.White)
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

    if (queueItemsPendingDelete.isNotEmpty()) {
        val itemsToDelete = queueItemsPendingDelete
        val isSingle = itemsToDelete.size == 1
        AlertDialog(
            onDismissRequest = { queueItemsPendingDelete = emptyList() },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete_audio_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isSingle) {
                        stringResource(R.string.delete_media_from_device_confirm, itemsToDelete.first().title)
                    } else {
                        stringResource(R.string.delete_selected_confirm, itemsToDelete.size)
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val items = itemsToDelete
                        queueItemsPendingDelete = emptyList()
                        scope.launch {
                            val urisToDelete = items.map { Uri.parse(it.uri) }
                            val deletedUriStrings = items.map { it.uri }.toSet()
                            deletedUriStrings.forEach { uriStr -> viewModel.deleteHistoryItem(uriStr) }
                            val currentTarget = currentAudio?.uri?.toString() ?: audioUri
                            val isCurrentDeleted = deletedUriStrings.contains(currentTarget)
                            if (isCurrentDeleted) {
                                viewModel.stopIfPlaying(Uri.parse(currentTarget))
                            }
                            withContext(Dispatchers.IO) {
                                urisToDelete.forEach { u ->
                                    try {
                                        context.contentResolver.delete(u, null, null)
                                    } catch (e: Exception) {
                                        android.util.Log.e("AudioPlayerScreen", "Failed to delete uri: $u", e)
                                    }
                                }
                            }
                            val hasRemainingItems = queueItems.any { !deletedUriStrings.contains(it.uri) }
                            if (isCurrentDeleted) {
                                val autoPlayNext = viewModel.autoPlayNext.value
                                if (hasRemainingItems && autoPlayNext) {
                                    viewModel.removeQueueItems(deletedUriStrings)
                                } else {
                                    viewModel.removeQueueItems(deletedUriStrings)
                                    onBack()
                                }
                            } else {
                                viewModel.removeQueueItems(deletedUriStrings)
                            }
                            viewModel.onAudiosDeleted(urisToDelete)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.delete_from_device), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { queueItemsPendingDelete = emptyList() }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
