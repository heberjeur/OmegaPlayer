package com.mohamedaminelouati.oplayer.ui.feature.player

import android.app.Activity
import androidx.compose.ui.draw.scale
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import android.util.Rational
import android.view.LayoutInflater
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import com.mohamedaminelouati.oplayer.data.PlaybackSpeedScope
import com.mohamedaminelouati.oplayer.data.BrightnessScope
import com.mohamedaminelouati.oplayer.media.PlaybackSource
import kotlinx.coroutines.flow.flowOf
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.ui.CaptionStyleCompat
import android.util.TypedValue
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.annotation.OptIn as AndroidOptIn
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.mohamedaminelouati.oplayer.ui.theme.LocalTextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.maxLines
import com.mohamedaminelouati.oplayer.ui.theme.overflow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mohamedaminelouati.oplayer.R
import com.mohamedaminelouati.oplayer.ui.common.SystemVolumeSyncEffect
import com.mohamedaminelouati.oplayer.ui.common.applySystemMusicVolume
import com.mohamedaminelouati.oplayer.ui.common.rememberInitialSystemBrightness
import com.mohamedaminelouati.oplayer.ui.common.rememberInitialSystemVolume
import com.mohamedaminelouati.oplayer.ui.common.rememberSystemAudioManager
import com.mohamedaminelouati.oplayer.ui.common.rememberSystemMaxVolume
import com.mohamedaminelouati.oplayer.ui.common.setBrightness
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Job
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

import com.mohamedaminelouati.oplayer.util.MediaUtils
import com.mohamedaminelouati.oplayer.util.MediaUtils.formatDuration
import com.mohamedaminelouati.oplayer.viewmodel.VideoViewModel

@AndroidOptIn(UnstableApi::class)
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoUri: String, 
    from: String? = null,
    viewModel: VideoViewModel,
    isDarkTheme: Boolean, 
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialPosition: Long = -1L,
    isInPiPMode: Boolean = false,
    onBack: () -> Unit,
    onAudioTransition: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val activity = context as? Activity
    val hazeState = remember { HazeState() }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }
    
    val mediaController by viewModel.mediaController.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isBackgroundPlayEnabled by viewModel.isBackgroundPlayEnabled.collectAsStateWithLifecycle()
    val controlsTimeout by viewModel.controlsTimeout.collectAsStateWithLifecycle(initialValue = 3)
    val seekIncrement by viewModel.seekIncrement.collectAsStateWithLifecycle(initialValue = 10)
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val sleepTimerActive by viewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val sleepTimerTimeLeft by viewModel.sleepTimerTimeLeft.collectAsStateWithLifecycle()
    val stopAfterCurrent by viewModel.stopAfterCurrent.collectAsStateWithLifecycle()
    val volumeBoostEnabled by viewModel.volumeBoostEnabled.collectAsStateWithLifecycle()
    val skipSilenceEnabled by viewModel.skipSilenceEnabled.collectAsStateWithLifecycle()
    val savePositionOnQuit by viewModel.savePositionOnQuit.collectAsStateWithLifecycle(initialValue = true)
    val closeAfterQueueEnds by viewModel.closeAfterQueueEnds.collectAsStateWithLifecycle(initialValue = false)

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var targetPlaylistUri by remember { mutableStateOf<String?>(null) }
    var targetPlaylistUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var currentTracks by remember { mutableStateOf(mediaController?.currentTracks ?: Tracks.EMPTY) }
    var showQueueSheet by remember { mutableStateOf(false) }
    val upNextFullyExpanded by viewModel.upNextFullyExpanded.collectAsStateWithLifecycle(initialValue = true)
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = upNextFullyExpanded)

    val subtitleTextSize by viewModel.subtitleTextSize.collectAsStateWithLifecycle(initialValue = 18)
    val subtitleTextColor by viewModel.subtitleTextColor.collectAsStateWithLifecycle(initialValue = 0)
    val subtitleBgStyle by viewModel.subtitleBgStyle.collectAsStateWithLifecycle(initialValue = 1)
    var subtitleDelaySeconds by remember { mutableFloatStateOf(0f) }

    var isVerticalVideo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var pendingUrisToDelete by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var queueItemsPendingDelete by remember { mutableStateOf<List<com.mohamedaminelouati.oplayer.media.PlaybackQueueItem>>(emptyList()) }

    val enterPiP: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val player = mediaController
            val videoSize = player?.videoSize
            val rational = if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
                val aspect = videoSize.width.toFloat() / videoSize.height.toFloat()
                if (aspect in 0.45f..2.35f) Rational(videoSize.width, videoSize.height) else Rational(16, 9)
            } else Rational(16, 9)

            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(rational)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(!isBackgroundPlayEnabled)
            }
            activity?.enterPictureInPictureMode(builder.build())
        }
    }

    val subtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val player = mediaController ?: return@rememberLauncherForActivityResult
            val currentItem = player.currentMediaItem ?: return@rememberLauncherForActivityResult
            val mimeType = if (uri.toString().endsWith(".vtt", ignoreCase = true)) {
                MimeTypes.TEXT_VTT
            } else if (uri.toString().endsWith(".ass", ignoreCase = true) || uri.toString().endsWith(".ssa", ignoreCase = true)) {
                MimeTypes.TEXT_SSA
            } else {
                MimeTypes.APPLICATION_SUBRIP
            }
            val subConfig = MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mimeType)
                .setLanguage(Locale.getDefault().language)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            val newItem = currentItem.buildUpon()
                .setSubtitleConfigurations(listOf(subConfig))
                .build()
            val pos = player.currentPosition
            val isPlayingNow = player.isPlaying
            player.setMediaItem(newItem, pos)
            player.prepare()
            if (isPlayingNow) player.play()
            Toast.makeText(context, context.getString(R.string.subtitles_loaded_success), Toast.LENGTH_SHORT).show()
        }
    }

    val videosList by viewModel.videos.collectAsStateWithLifecycle()
    var currentUriState by remember(videoUri) { mutableStateOf(videoUri) }
    val playingUri = currentUriState
    val currentVideo = remember(playingUri, videosList) {
        viewModel.getCurrentVideo(playingUri) ?: videosList.find { it.uri.toString() == playingUri }
    }

    val currentFolder = remember(currentVideo, playingUri) {
        val path = currentVideo?.path ?: videosList.find { it.uri.toString() == playingUri }?.path
        if (!path.isNullOrBlank()) {
            java.io.File(path).parentFile?.absolutePath ?: ""
        } else {
            ""
        }
    }

    val folderVideos = remember(currentFolder, videosList) {
        viewModel.getVideosInFolder(currentFolder)
    }
    val activeQueueVideos = remember(folderVideos, videosList) {
        if (folderVideos.isNotEmpty()) folderVideos else videosList
    }
    val activeQueue by viewModel.activeQueue.collectAsStateWithLifecycle()
    val queueItems = remember(activeQueue, activeQueueVideos, videoUri) {
        if (activeQueue.isNotEmpty()) {
            activeQueue
        } else if (activeQueueVideos.isNotEmpty()) {
            activeQueueVideos.map {
                com.mohamedaminelouati.oplayer.media.PlaybackQueueItem(
                    uri = it.uri.toString(),
                    title = it.name,
                    duration = it.duration,
                    isVideo = true
                )
            }
        } else {
            listOf(
                com.mohamedaminelouati.oplayer.media.PlaybackQueueItem(
                    uri = videoUri,
                    title = "",
                    duration = 0L,
                    isVideo = true
                )
            )
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedUriStrings = pendingUrisToDelete.map { it.toString() }.toSet()
            deletedUriStrings.forEach { uriStr -> viewModel.deleteHistoryItem(uriStr) }
            val isCurrentDeleted = deletedUriStrings.contains(playingUri)
            val hasRemainingItems = queueItems.any { !deletedUriStrings.contains(it.uri) }
            if (isCurrentDeleted) {
                val autoPlayNext = viewModel.autoPlayNext.value
                if (hasRemainingItems && autoPlayNext) {
                    viewModel.removeQueueItems(deletedUriStrings, queueItems)
                } else {
                    mediaController?.currentMediaItem?.localConfiguration?.uri?.let { viewModel.stopIfPlaying(it) }
                    viewModel.removeQueueItems(deletedUriStrings, queueItems)
                    onBack()
                }
            } else {
                viewModel.removeQueueItems(deletedUriStrings, queueItems)
            }
            if (pendingUrisToDelete.isNotEmpty()) viewModel.onVideosDeleted(pendingUrisToDelete)
            pendingUrisToDelete = emptyList()
            queueItemsPendingDelete = emptyList()
        } else {
            pendingUrisToDelete = emptyList()
            queueItemsPendingDelete = emptyList()
        }
    }
    val playerOrientation by viewModel.playerOrientation.collectAsStateWithLifecycle(initialValue = 0)

    val speedScope by viewModel.speedScope.collectAsStateWithLifecycle(initialValue = PlaybackSpeedScope.GLOBAL)
    val globalSpeed by viewModel.globalPlaybackSpeed.collectAsStateWithLifecycle(initialValue = 1.0f)
    val folderSpeedFlow = remember(currentFolder) { viewModel.getFolderSpeed(currentFolder) }
    val folderSpeed by folderSpeedFlow.collectAsStateWithLifecycle(initialValue = 1.0f)

    val playbackSource by viewModel.playbackSource.collectAsStateWithLifecycle(initialValue = PlaybackSource.None)
    val isHistory = com.mohamedaminelouati.oplayer.media.PlaybackSpeedResolver.isHistorySource(from, playbackSource)
    val playlistId = com.mohamedaminelouati.oplayer.media.PlaybackSpeedResolver.resolvePlaylistId(from, playbackSource)

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
        folderSpeed = folderSpeed ?: 1.0f
    )

    val showSystemStatusBar by viewModel.showSystemStatusBar.collectAsStateWithLifecycle(initialValue = true)
    val hudShowClock by viewModel.showPlayerClock.collectAsStateWithLifecycle(initialValue = false)
    val hudShowBattery by viewModel.showPlayerBattery.collectAsStateWithLifecycle(initialValue = false)
    val hudShowMediaInfo by viewModel.showPlayerMediaInfo.collectAsStateWithLifecycle(initialValue = false)
    val hudShowVolume by viewModel.showPlayerVolume.collectAsStateWithLifecycle(initialValue = false)
    val hudShowBrightness by viewModel.showPlayerBrightness.collectAsStateWithLifecycle(initialValue = false)
    val gestureVolumeEnabled by viewModel.gestureVolumeEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gestureBrightnessEnabled by viewModel.gestureBrightnessEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gesturePinchToZoomEnabled by viewModel.gesturePinchToZoomEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gestureSeekForwardEnabled by viewModel.gestureSeekForwardEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gestureSeekBackwardEnabled by viewModel.gestureSeekBackwardEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gesturePlayPauseEnabled by viewModel.gesturePlayPauseEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gestureLongPressSpeedEnabled by viewModel.gestureLongPressSpeedEnabled.collectAsStateWithLifecycle(initialValue = true)
    val gestureTapControlsEnabled by viewModel.gestureTapControlsEnabled.collectAsStateWithLifecycle(initialValue = true)

    val audioManager = rememberSystemAudioManager()
    val maxVolume = rememberSystemMaxVolume(audioManager)

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

    var videoResolution by remember { mutableStateOf<String?>(null) }

    val keepScreenOnWhenPaused by viewModel.keepScreenOnWhenPaused.collectAsStateWithLifecycle(initialValue = false)
    val brightnessScope by viewModel.brightnessScope.collectAsStateWithLifecycle(initialValue = BrightnessScope.NONE)
    val globalBrightness by viewModel.globalBrightness.collectAsStateWithLifecycle(initialValue = -1f)
    val folderBrightnessFlow = remember(currentFolder) {
        if (currentFolder.isNotEmpty()) viewModel.getFolderBrightness(currentFolder) else flowOf(null)
    }
    val folderBrightness by folderBrightnessFlow.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(isPlaying, keepScreenOnWhenPaused) {
        activity?.window?.let { window ->
            if (isPlaying || keepScreenOnWhenPaused) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val initialVolume = rememberInitialSystemVolume(audioManager, maxVolume)
    var volume by remember { mutableFloatStateOf(initialVolume) }

    val initialBrightness = rememberInitialSystemBrightness()
    var brightness by remember { mutableFloatStateOf(initialBrightness) }

    LaunchedEffect(brightnessScope, globalBrightness, folderBrightness) {
        val targetBrightness = when (brightnessScope) {
            BrightnessScope.GLOBAL -> if (globalBrightness > 0f) globalBrightness else null
            BrightnessScope.PER_FOLDER -> if ((folderBrightness ?: -1f) > 0f) folderBrightness else null
            BrightnessScope.NONE -> null
        }
        if (targetBrightness != null && targetBrightness > 0f) {
            brightness = targetBrightness
            setBrightness(context, targetBrightness)
        }
    }

    SystemVolumeSyncEffect(
        audioManager = audioManager,
        volumeProvider = { volume },
        onVolumeChange = { volume = it },
        onVolumeBoostReset = { viewModel.eqManager.setVolumeBoostScale(1.0f) }
    )
    var isControlsVisible by remember { mutableStateOf(true) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var seekBarTopY by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var pendingSeekPosition by remember { mutableLongStateOf(-1L) }
    var seekGracePeriod by remember { mutableLongStateOf(0L) }
    var controlsLastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val isAnyDialogShown = showSleepTimerDialog || showPlaybackSpeedDialog || showPlaylistDialog ||
            showEqualizerDialog || showSubtitleDialog || showInfoDialog || showDeleteDialog || showQueueSheet || showMoreMenu

    LaunchedEffect(isControlsVisible, controlsLastInteraction, isDraggingSlider, isPlaying, controlsTimeout, isAnyDialogShown) {
        if (isControlsVisible && !isDraggingSlider && isPlaying && controlsTimeout > 0 && !isAnyDialogShown) {
            delay(controlsTimeout * 1000L)
            isControlsVisible = false
        }
    }

    var playbackSpeed by remember { mutableFloatStateOf(effectiveSpeed) }
    var isLandscape by rememberSaveable { mutableStateOf(false) }
    var aspectRatio by remember { mutableIntStateOf(0) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val truncation = LocalTextTruncationMode.current

    var isShuffle by remember { mutableStateOf(mediaController?.shuffleModeEnabled ?: false) }
    var currentMediaIndexState by remember { mutableIntStateOf(mediaController?.currentMediaItemIndex ?: 0) }
    var hasNext by remember { mutableStateOf(mediaController?.hasNextMediaItem() ?: false) }
    var hasPrevious by remember { mutableStateOf(mediaController?.hasPreviousMediaItem() ?: false) }

    val onSpeedChange: (Float) -> Unit = { speed ->
        playbackSpeed = speed
        mediaController?.setPlaybackSpeed(speed)
        when {
            speedScope == PlaybackSpeedScope.GLOBAL -> viewModel.setGlobalPlaybackSpeed(speed)
            isHistory -> viewModel.setHistoryPlaybackSpeed(speed)
            playlistId != null -> viewModel.setPlaylistPlaybackSpeed(playlistId, speed)
            else -> viewModel.setFolderPlaybackSpeed(currentFolder, speed)
        }
    }

    BackHandler(enabled = true) {
        if (isLocked) {
            Toast.makeText(context, context.getString(R.string.unlock_player_prompt), Toast.LENGTH_SHORT).show()
        } else if (isLandscape) {
            isLandscape = false
            activity?.requestedOrientation = if (playerOrientation == 0) ActivityInfo.SCREEN_ORIENTATION_USER else if (playerOrientation == 1) ActivityInfo.SCREEN_ORIENTATION_SENSOR else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onBack()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackgroundPlay = rememberUpdatedState(isBackgroundPlayEnabled)
    val coroutineScope = rememberCoroutineScope()
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }
    var tapJob by remember { mutableStateOf<Job?>(null) }

    var hasSeekedInitialPosition by remember(videoUri, initialPosition) { mutableStateOf(false) }
    LaunchedEffect(currentMediaIndexState, effectiveSpeed) {
        mediaController?.let { player ->
            if (playbackSpeed != effectiveSpeed) {
                playbackSpeed = effectiveSpeed
                player.setPlaybackSpeed(effectiveSpeed)
            }
        }
    }

    val currentCloseAfterQueueEnds by rememberUpdatedState(closeAfterQueueEnds)
    val currentOnBack by rememberUpdatedState(onBack)
    var playbackState by remember { mutableIntStateOf(mediaController?.playbackState ?: Player.STATE_IDLE) }

    LaunchedEffect(playbackState, closeAfterQueueEnds) {
        if (playbackState == Player.STATE_ENDED && closeAfterQueueEnds) {
            onBack()
        }
    }

    LaunchedEffect(videoUri, mediaController, initialPosition, effectiveSpeed) {
        val player = mediaController ?: return@LaunchedEffect
        val newUri = Uri.parse(videoUri)
        val currentUri = player.currentMediaItem?.localConfiguration?.uri
        val targetPos = if (initialPosition >= 0L) {
            initialPosition
        } else if (savePositionOnQuit) {
            viewModel.getSavedPosition(videoUri)
        } else 0L
        if (from == "intent" || from == null) {
            if (currentUri != newUri) {
                playbackError = null
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(MediaItem.fromUri(newUri))
                player.prepare()
                if (targetPos > 0L) {
                    player.seekTo(targetPos)
                }
                hasSeekedInitialPosition = true
            } else {
                if (!hasSeekedInitialPosition && initialPosition >= 0L) {
                    player.seekTo(initialPosition)
                    hasSeekedInitialPosition = true
                }
            }
        } else {
            if (currentUri == newUri) {
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
        player.setPlaybackSpeed(effectiveSpeed)
        playbackSpeed = effectiveSpeed
        player.playWhenReady = true
    }

    DisposableEffect(mediaController) {
        val player = mediaController ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val errorType = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> context.getString(R.string.error_file_not_found)
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> context.getString(R.string.error_network)
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> context.getString(R.string.error_decoder)
                    else -> context.getString(R.string.error_unexpected)
                }
                playbackError = "$errorType: ${error.message}"

                
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                    error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT) {
                    player.prepare()
                    player.play()
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    playbackError = null
                    if (player.videoSize.width > 0 && player.videoSize.height > 0) {
                        videoResolution = "${player.videoSize.width}x${player.videoSize.height}"
                    }
                } else if (state == Player.STATE_ENDED) {
                    if (currentCloseAfterQueueEnds) {
                        currentOnBack()
                    }
                }
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoResolution = "${videoSize.width}x${videoSize.height}"
                    isVerticalVideo = videoSize.height > videoSize.width
                }
            }
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
            override fun onShuffleModeEnabledChanged(shuffle: Boolean) {
                isShuffle = shuffle
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaIndexState = player.currentMediaItemIndex
                val currentUri = mediaItem?.localConfiguration?.uri?.toString()
                if (currentUri != null && currentUri.isNotEmpty()) {
                    currentUriState = currentUri
                    if (currentUri != videoUri && MediaUtils.isAudioMediaItem(mediaItem)) {
                        onAudioTransition(currentUri)
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
        currentTracks = player.currentTracks
        if (player.videoSize.width > 0 && player.videoSize.height > 0) {
            videoResolution = "${player.videoSize.width}x${player.videoSize.height}"
            isVerticalVideo = player.videoSize.height > player.videoSize.width
        }
        isShuffle = player.shuffleModeEnabled
        onDispose {
            player.removeListener(listener)
        }
    }

    DisposableEffect(showSystemStatusBar) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (showSystemStatusBar) {
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
        
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.show(WindowInsetsCompat.Type.systemBars())
            controller?.isAppearanceLightStatusBars = !isDarkTheme
            controller?.isAppearanceLightNavigationBars = !isDarkTheme
            
            val layoutParams = activity?.window?.attributes
            layoutParams?.screenBrightness = -1f
            activity?.window?.attributes = layoutParams

            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (savePositionOnQuit) {
                viewModel.savePlaybackProgress()
            }
            
            if (!currentBackgroundPlay.value) {
                mediaController?.pause()
            }
        }
    }
    
    var seekForwardPulse by remember { mutableIntStateOf(0) }
    var seekBackwardPulse by remember { mutableIntStateOf(0) }
    var consumedSeekForwardPulse by remember { mutableIntStateOf(0) }
    var consumedSeekBackwardPulse by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isZoomIndicatorVisible by remember { mutableStateOf(false) }

    var isFastSeekingActive by remember { mutableStateOf(false) }
    var wasPlayingBeforeFastForward by remember { mutableStateOf(false) }

    var isVolumeVisible by remember { mutableStateOf(false) }
    var isBrightnessVisible by remember { mutableStateOf(false) }

    var hardwareVolumeTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.volumeKeyEvents.collect { keyCode ->
            val maxVolMultiplier = if (volumeBoostEnabled) 2f else 1f
            val step = 0.066f
            val oldVolume = volume
            if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
                volume = (volume + step).coerceIn(0f, maxVolMultiplier)
            } else if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
                volume = (volume - step).coerceIn(0f, maxVolMultiplier)
            }
            if (abs(volume - oldVolume) > 0.01f) {
                applySystemMusicVolume(audioManager, volume, maxVolume)
                if (volumeBoostEnabled) {
                    viewModel.eqManager.setVolumeBoostScale(if (volume > 1f) volume else 1.0f)
                }
                isVolumeVisible = true
                hardwareVolumeTrigger++
            }
        }
    }

    LaunchedEffect(hardwareVolumeTrigger) {
        if (hardwareVolumeTrigger > 0) {
            delay(2000L)
            isVolumeVisible = false
        }
    }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    
    var activeCenterAction by remember { mutableStateOf(CenterActionType.NONE) }
    var centerActionTrigger by remember { mutableLongStateOf(0L) }
    var playPausePulse by remember { mutableIntStateOf(0) }
    var seekStreak by remember { mutableIntStateOf(0) }
    var lastSeekTimestamp by remember { mutableLongStateOf(0L) }
    var lastSeekDirection by remember { mutableIntStateOf(0) }
    var lastSeekAmountSeconds by remember { mutableIntStateOf(10) }

    val performSeek: (Int) -> Unit = performSeek@{ direction ->
        val player = mediaController ?: return@performSeek
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
        controlsLastInteraction = now
    }

    LaunchedEffect(centerActionTrigger) {
        if (centerActionTrigger > 0L) {
            delay(850L)
            activeCenterAction = CenterActionType.NONE
        }
    }

    LaunchedEffect(mediaController) {
        while (true) {
            mediaController?.let { player ->
                val pos = player.currentPosition
                val dur = player.duration
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
            delay(200.milliseconds)
        }
    }

    val videoName = remember(playingUri, videosList, currentVideo) {
        val foundName = currentVideo?.name ?: videosList.find { it.uri.toString() == playingUri }?.name
        if (!foundName.isNullOrBlank()) {
            foundName
        } else {
            var queriedName: String? = null
            val parsedUri = Uri.parse(playingUri)
            if (parsedUri.scheme == "content") {
                context.contentResolver.query(parsedUri, arrayOf(MediaStore.Video.Media.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val col = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                        if (col != -1) {
                            queriedName = cursor.getString(col)
                        }
                    }
                }
            }
            queriedName ?: playingUri.substringAfterLast("/").substringBeforeLast(".")
        }
    }

    if (isInPiPMode) {
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.player_view, null) as PlayerView
                view.apply {
                    player = mediaController
                    useController = false
                }
            },
            update = { playerView ->
                playerView.player = mediaController
                playerView.useController = false
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        with(sharedTransitionScope) {
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx).inflate(R.layout.player_view, null) as PlayerView
                    view.apply {
                        player = mediaController
                    }
                },
                update = { playerView ->
                    playerView.player = mediaController
                    playerView.resizeMode = when (aspectRatio) {
                        1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    val fgColor = when (subtitleTextColor) {
                        1 -> android.graphics.Color.YELLOW
                        2 -> android.graphics.Color.CYAN
                        else -> android.graphics.Color.WHITE
                    }
                    val bgColor = when (subtitleBgStyle) {
                        2 -> android.graphics.Color.BLACK
                        1 -> android.graphics.Color.argb(140, 0, 0, 0)
                        else -> android.graphics.Color.TRANSPARENT
                    }
                    val style = CaptionStyleCompat(
                        fgColor,
                        bgColor,
                        android.graphics.Color.TRANSPARENT,
                        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        android.graphics.Color.BLACK,
                        null
                    )
                    playerView.subtitleView?.setStyle(style)
                    playerView.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleTextSize.toFloat())
                },
                onRelease = { playerView ->
                    if (!currentBackgroundPlay.value) {
                        playerView.player = null
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .sharedBounds(
                        rememberSharedContentState(key = "video_bounds_$videoUri"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                    .clipToBounds()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = panOffset.x,
                        translationY = panOffset.y
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked, isRtl) {
                    if (isLocked) {
                        detectTapGestures(onTap = {
                            isControlsVisible = !isControlsVisible
                            controlsLastInteraction = System.currentTimeMillis()
                        })
                    } else {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downTime = System.currentTimeMillis()
                                val downPos = down.position
                                val limitY = if (seekBarTopY < size.height) seekBarTopY else (size.height * if (isLandscape) 0.8f else 0.75f)

                                var isMultiTouch = false
                                var isHoldingLongPress = false
                                var isDraggingVertical = false
                                var hasMovedPastSlop = false
                                var activeDragType = 0

                                val isBrightnessZone = if (isRtl) (downPos.x > size.width * 0.75f && gestureBrightnessEnabled) else (downPos.x < size.width * 0.25f && gestureBrightnessEnabled)
                                val isVolumeZone = if (isRtl) (downPos.x < size.width * 0.25f && gestureVolumeEnabled) else (downPos.x > size.width * 0.75f && gestureVolumeEnabled)
                                val canAdjustVertical = (isBrightnessZone || isVolumeZone) && downPos.y < limitY && scale <= 1.05f
                                if (canAdjustVertical) {
                                    activeDragType = if (isBrightnessZone) 1 else 2
                                }

                                var isLongPressTimedOut = false
                                val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                                while (System.currentTimeMillis() - downTime < longPressTimeout) {
                                    val remainingTime = longPressTimeout - (System.currentTimeMillis() - downTime)
                                    val event = withTimeoutOrNull(remainingTime) {
                                        awaitPointerEvent()
                                    }
                                    if (event == null) {
                                        isLongPressTimedOut = true
                                        val isFastForwardZone = if (isRtl) (downPos.x <= size.width * 0.5f) else (downPos.x >= size.width * 0.5f)
                                        if (gestureLongPressSpeedEnabled && scale <= 1.05f && downPos.y < limitY && isFastForwardZone) {
                                            isHoldingLongPress = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val player = mediaController
                                            if (player != null) {
                                                isFastSeekingActive = true
                                                wasPlayingBeforeFastForward = player.isPlaying
                                                player.setPlaybackSpeed(if (playbackSpeed >= 2.0f) 3.0f else 2.0f)
                                                if (!player.isPlaying) {
                                                    player.play()
                                                }
                                            }
                                        }
                                        break
                                    }

                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.size >= 2) {
                                        isMultiTouch = true
                                        break
                                    }
                                    if (pressed.isEmpty()) {
                                        break
                                    }
                                    val pointer = pressed[0]
                                    val delta = pointer.position - downPos
                                    val dist = (delta.x * delta.x + delta.y * delta.y)
                                    if (dist > viewConfiguration.touchSlop * viewConfiguration.touchSlop) {
                                        hasMovedPastSlop = true
                                        if (gesturePinchToZoomEnabled && scale > 1.05f) {
                                            panOffset += (pointer.position - pointer.previousPosition)
                                            pointer.consume()
                                        } else if (canAdjustVertical && abs(delta.y) > abs(delta.x)) {
                                            isDraggingVertical = true
                                            pointer.consume()
                                        }
                                        break
                                    }
                                }

                                while (isHoldingLongPress) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.size >= 2) {
                                        mediaController?.setPlaybackSpeed(playbackSpeed)
                                        if (!wasPlayingBeforeFastForward) {
                                            mediaController?.pause()
                                        }
                                        isFastSeekingActive = false
                                        isHoldingLongPress = false
                                        isMultiTouch = true
                                        break
                                    } else if (pressed.size == 1) {
                                        pressed[0].consume()
                                    } else {
                                        mediaController?.setPlaybackSpeed(playbackSpeed)
                                        if (!wasPlayingBeforeFastForward) {
                                            mediaController?.pause()
                                        }
                                        isFastSeekingActive = false
                                        isHoldingLongPress = false
                                        break
                                    }
                                }

                                while (isDraggingVertical) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.size >= 2) {
                                        if (activeDragType == 1) {
                                            when (brightnessScope) {
                                                BrightnessScope.GLOBAL -> viewModel.saveGlobalBrightness(brightness)
                                                BrightnessScope.PER_FOLDER -> if (currentFolder.isNotEmpty()) viewModel.saveFolderBrightness(currentFolder, brightness)
                                                BrightnessScope.NONE -> {}
                                            }
                                        }
                                        isDraggingVertical = false
                                        isVolumeVisible = false
                                        isBrightnessVisible = false
                                        isMultiTouch = true
                                        break
                                    } else if (pressed.size == 1) {
                                        val pointer = pressed[0]
                                        pointer.consume()
                                        val dragDeltaY = pointer.position.y - pointer.previousPosition.y
                                        val height = size.height
                                        if (activeDragType == 1) {
                                            val oldBrightness = brightness
                                            brightness = (brightness - dragDeltaY / height).coerceIn(0.01f, 1f)
                                            if (abs(brightness - oldBrightness) > 0.05f) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            setBrightness(context, brightness)
                                            isBrightnessVisible = true
                                        } else if (activeDragType == 2) {
                                            val oldVolume = volume
                                            val maxVolMultiplier = if (volumeBoostEnabled) 2f else 1f
                                            volume = (volume - dragDeltaY / height).coerceIn(0f, maxVolMultiplier)
                                            if (abs(volume - oldVolume) > 0.05f) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            applySystemMusicVolume(audioManager, volume, maxVolume)
                                            if (volumeBoostEnabled) {
                                                viewModel.eqManager.setVolumeBoostScale(if (volume > 1f) volume else 1.0f)
                                            }
                                            isVolumeVisible = true
                                        }
                                    } else {
                                        if (activeDragType == 1) {
                                            when (brightnessScope) {
                                                BrightnessScope.GLOBAL -> viewModel.saveGlobalBrightness(brightness)
                                                BrightnessScope.PER_FOLDER -> if (currentFolder.isNotEmpty()) viewModel.saveFolderBrightness(currentFolder, brightness)
                                                BrightnessScope.NONE -> {}
                                            }
                                        }
                                        isDraggingVertical = false
                                        isVolumeVisible = false
                                        isBrightnessVisible = false
                                        break
                                    }
                                }

                                if (gesturePinchToZoomEnabled && hasMovedPastSlop && scale > 1.05f) {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.size >= 2) {
                                            isMultiTouch = true
                                            break
                                        } else if (pressed.size == 1) {
                                            val pointer = pressed[0]
                                            val panDelta = pointer.position - pointer.previousPosition
                                            panOffset += panDelta
                                            pointer.consume()
                                        } else {
                                            break
                                        }
                                    }
                                }

                                if (isMultiTouch) {
                                    if (gesturePinchToZoomEnabled) {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            if (pressed.size >= 2) {
                                                val p1 = pressed[0]
                                                val p2 = pressed[1]
                                                val curDist = kotlin.math.hypot(p1.position.x - p2.position.x, p1.position.y - p2.position.y)
                                                val prevDist = kotlin.math.hypot(p1.previousPosition.x - p2.previousPosition.x, p1.previousPosition.y - p2.previousPosition.y)
                                                if (prevDist > 0f && curDist > 0f) {
                                                    val zoomRatio = curDist / prevDist
                                                    val newScale = (scale * zoomRatio).coerceIn(1f, 5f)
                                                    scale = newScale
                                                    if (newScale <= 1.02f) {
                                                        scale = 1f
                                                        panOffset = Offset.Zero
                                                    } else {
                                                        val curCentroid = (p1.position + p2.position) / 2f
                                                        val prevCentroid = (p1.previousPosition + p2.previousPosition) / 2f
                                                        val panDelta = curCentroid - prevCentroid
                                                        panOffset = (panOffset * zoomRatio) + panDelta
                                                    }
                                                }
                                                isZoomIndicatorVisible = true
                                                p1.consume()
                                                p2.consume()
                                            } else {
                                                isZoomIndicatorVisible = false
                                                if (scale <= 1.02f) {
                                                    scale = 1f
                                                    panOffset = Offset.Zero
                                                }
                                                if (pressed.isEmpty()) {
                                                    break
                                                }
                                            }
                                        }
                                    } else {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            if (pressed.isEmpty()) break
                                        }
                                    }
                                }

                                if (!isMultiTouch && !hasMovedPastSlop && !isHoldingLongPress && !isDraggingVertical && !isLongPressTimedOut) {
                                    val now = System.currentTimeMillis()
                                    val dist = (downPos.x - lastTapPos.x) * (downPos.x - lastTapPos.x) + (downPos.y - lastTapPos.y) * (downPos.y - lastTapPos.y)
                                    if (now - lastTapTime < 350L && dist < 14400f) {
                                        tapJob?.cancel()
                                        tapJob = null
                                        lastTapTime = 0L

                                        val player = mediaController
                                        if (player != null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val isRewindTap = if (isRtl) (downPos.x > size.width * 0.75f) else (downPos.x < size.width * 0.25f)
                                            val isForwardTap = if (isRtl) (downPos.x < size.width * 0.25f) else (downPos.x > size.width * 0.75f)
                                            if (isRewindTap) {
                                                if (gestureSeekBackwardEnabled) {
                                                    performSeek(-1)
                                                }
                                            } else if (isForwardTap) {
                                                if (gestureSeekForwardEnabled) {
                                                    performSeek(1)
                                                }
                                            } else {
                                                if (gesturePlayPauseEnabled) {
                                                    if (player.isPlaying) {
                                                        player.pause()
                                                    } else {
                                                        if (player.playbackState == Player.STATE_ENDED) {
                                                            player.seekTo(0L)
                                                        }
                                                        player.play()
                                                    }
                                                    playPausePulse++
                                                    activeCenterAction = CenterActionType.PLAY_PAUSE
                                                    centerActionTrigger = System.currentTimeMillis()
                                                    controlsLastInteraction = System.currentTimeMillis()
                                                }
                                            }
                                        }
                                    } else {
                                        lastTapTime = now
                                        lastTapPos = downPos
                                        tapJob?.cancel()
                                        tapJob = coroutineScope.launch {
                                            delay(350L)
                                            if (gestureTapControlsEnabled) {
                                                isControlsVisible = !isControlsVisible
                                                controlsLastInteraction = System.currentTimeMillis()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        )

        if (isLocked && isControlsVisible) {
            IconButton(
                onClick = {
                    isLocked = false
                    controlsLastInteraction = System.currentTimeMillis()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.action_unlock), tint = MaterialTheme.colorScheme.primary)
            }
        }

        VerticalIndicator(
            value = volume / if (volumeBoostEnabled) 2f else 1f,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            visible = isVolumeVisible && !isLocked,
            text = (volume * 100).toInt().toString(),
            color = if (volume > 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        
        VerticalIndicator(
            value = brightness,
            icon = Icons.Default.BrightnessMedium,
            visible = isBrightnessVisible && !isLocked,
            text = (brightness * 100).toInt().toString(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        AnimatedVisibility(
            visible = isFastSeekingActive && !isLocked,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (isLandscape) 84.dp else 96.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.speed_fast_forward_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isZoomIndicatorVisible && !isFastSeekingActive && !isLocked,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (isLandscape) 84.dp else 96.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isControlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = if (isLandscape) 24.dp else 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (isLandscape) {
                                isLandscape = false
                                activity?.requestedOrientation = if (playerOrientation == 0) ActivityInfo.SCREEN_ORIENTATION_USER else if (playerOrientation == 1) ActivityInfo.SCREEN_ORIENTATION_SENSOR else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
                        }
                        Text(
                            text = videoName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = truncation.maxLines,
                            overflow = truncation.overflow,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = {
                            showQueueSheet = true
                            controlsLastInteraction = System.currentTimeMillis()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.queue), tint = Color.White)
                        }
                        Box {
                            IconButton(onClick = {
                                showMoreMenu = true
                                controlsLastInteraction = System.currentTimeMillis()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more), tint = Color.White)
                            }
                            PlayerDropdownMenu(
                                expanded = showMoreMenu,
                                onDismiss = { showMoreMenu = false },
                                playbackSpeed = playbackSpeed,
                                sleepTimerActive = sleepTimerActive,
                                sleepTimerTimeLeft = sleepTimerTimeLeft,
                                onPlaylistClick = { showPlaylistDialog = true },
                                onSleepTimerClick = { showSleepTimerDialog = true },
                                onSpeedClick = { showPlaybackSpeedDialog = true },
                                onEqualizerClick = { showEqualizerDialog = true },
                                onInfoClick = { showInfoDialog = true },
                                onDeleteClick = { 
                                    val resolvedVideo = currentVideo ?: activeQueueVideos.find { it.uri.toString() == playingUri }
                                    val targetUri = resolvedVideo?.uri ?: Uri.parse(playingUri)
                                    pendingUrisToDelete = listOf(targetUri)
                                    com.mohamedaminelouati.oplayer.util.MediaUtils.requestMediaDelete(
                                        context = context,
                                        uris = listOf(targetUri),
                                        deleteLauncher = deleteLauncher,
                                        onRequireInternalPopup = { showDeleteDialog = true }
                                    )
                                }
                            )
                        }
                    }

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
                        resolution = videoResolution
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = if (isLandscape) 24.dp else 16.dp, vertical = 8.dp)
                        .onGloballyPositioned { coordinates ->
                            if (coordinates.isAttached) {
                                seekBarTopY = coordinates.positionInRoot().y
                            }
                        }
                ) {
                    PlayerSlider(
                        videoUri = playingUri,
                        currentPosition = currentPosition,
                        duration = duration,
                        pendingSeekPosition = pendingSeekPosition,
                        isDraggingSlider = isDraggingSlider,
                        onValueChange = { fraction ->
                            isDraggingSlider = true
                            val target = (fraction * duration).toLong()
                            currentPosition = target
                            pendingSeekPosition = target
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            val target = pendingSeekPosition.coerceAtLeast(0L)
                            seekGracePeriod = System.currentTimeMillis() + SEEK_TOLERANCE_MS
                            mediaController?.seekTo(target)
                            controlsLastInteraction = System.currentTimeMillis()
                        },
                        textsBelow = false
                    )

                    if (isLandscape) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RepeatControlButton(
                                    repeatMode = repeatMode,
                                    onClick = {
                                        viewModel.cycleRepeatMode()
                                        controlsLastInteraction = System.currentTimeMillis()
                                    }
                                )
                                IconButton(onClick = {
                                    val nextShuffle = !(mediaController?.shuffleModeEnabled ?: false)
                                    mediaController?.shuffleModeEnabled = nextShuffle
                                    isShuffle = nextShuffle
                                    controlsLastInteraction = System.currentTimeMillis()
                                }) {
                                    Icon(
                                        Icons.Default.Shuffle,
                                        contentDescription = stringResource(R.string.action_shuffle),
                                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    mediaController?.seekToPrevious()
                                    controlsLastInteraction = System.currentTimeMillis()
                                }, enabled = hasPrevious) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.action_previous), modifier = Modifier.size(36.dp), tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f))
                                }
                                FilledIconButton(
                                    onClick = {
                                        if (mediaController?.isPlaying == true) {
                                            mediaController?.pause()
                                        } else {
                                            if (mediaController?.playbackState == Player.STATE_ENDED) {
                                                mediaController?.seekTo(0L)
                                            }
                                            mediaController?.play()
                                        }
                                        controlsLastInteraction = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    mediaController?.seekToNext()
                                    controlsLastInteraction = System.currentTimeMillis()
                                }, enabled = hasNext) {
                                    Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.action_next), modifier = Modifier.size(36.dp), tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f))
                                }
                            }

                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    showSubtitleDialog = true
                                    controlsLastInteraction = System.currentTimeMillis()
                                }) {
                                    Icon(Icons.Default.Subtitles, contentDescription = stringResource(R.string.subtitles_title), tint = Color.White)
                                }
                                IconButton(onClick = enterPiP) {
                                    Icon(Icons.Default.PictureInPictureAlt, contentDescription = stringResource(R.string.action_pip), tint = Color.White)
                                }
                                IconButton(onClick = {
                                    aspectRatio = (aspectRatio + 1) % 3
                                    controlsLastInteraction = System.currentTimeMillis()
                                }) {
                                    Icon(when(aspectRatio) { 1 -> Icons.Default.Fullscreen; 2 -> Icons.Default.AspectRatio; else -> Icons.Default.FitScreen }, contentDescription = stringResource(R.string.action_aspect_ratio), tint = Color.White)
                                }
                                IconButton(onClick = {
                                    isLocked = true
                                    controlsLastInteraction = System.currentTimeMillis()
                                }) {
                                    Icon(Icons.Default.LockOpen, contentDescription = stringResource(R.string.action_lock), tint = Color.White)
                                }
                                if (playerOrientation == 2) {
                                    IconButton(onClick = {
                                        isLandscape = false
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }) {
                                        Icon(Icons.Default.FullscreenExit, contentDescription = stringResource(R.string.action_exit_fullscreen), tint = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RepeatControlButton(
                                repeatMode = repeatMode,
                                onClick = {
                                    viewModel.cycleRepeatMode()
                                    controlsLastInteraction = System.currentTimeMillis()
                                }
                            )

                            IconButton(onClick = {
                                mediaController?.seekToPrevious()
                                controlsLastInteraction = System.currentTimeMillis()
                            }, enabled = hasPrevious) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.action_previous), modifier = Modifier.size(36.dp), tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f))
                            }

                            FilledIconButton(
                                onClick = {
                                    if (mediaController?.isPlaying == true) {
                                        mediaController?.pause()
                                    } else {
                                        if (mediaController?.playbackState == Player.STATE_ENDED) {
                                            mediaController?.seekTo(0L)
                                        }
                                        mediaController?.play()
                                    }
                                    controlsLastInteraction = System.currentTimeMillis()
                                },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(onClick = {
                                mediaController?.seekToNext()
                                controlsLastInteraction = System.currentTimeMillis()
                            }, enabled = hasNext) {
                                Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.action_next), modifier = Modifier.size(36.dp), tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f))
                            }

                            IconButton(onClick = {
                                val nextShuffle = !(mediaController?.shuffleModeEnabled ?: false)
                                mediaController?.shuffleModeEnabled = nextShuffle
                                isShuffle = nextShuffle
                                controlsLastInteraction = System.currentTimeMillis()
                            }) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = stringResource(R.string.action_shuffle),
                                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                isLandscape = true
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }) {
                                Icon(Icons.Default.Fullscreen, contentDescription = stringResource(R.string.action_fullscreen), tint = Color.White)
                            }
                            IconButton(onClick = {
                                aspectRatio = (aspectRatio + 1) % 3
                                controlsLastInteraction = System.currentTimeMillis()
                            }) {
                                Icon(when(aspectRatio) { 1 -> Icons.Default.Fullscreen; 2 -> Icons.Default.AspectRatio; else -> Icons.Default.FitScreen }, contentDescription = stringResource(R.string.action_aspect_ratio), tint = Color.White)
                            }
                            IconButton(onClick = {
                                showSubtitleDialog = true
                                controlsLastInteraction = System.currentTimeMillis()
                            }) {
                                Icon(Icons.Default.Subtitles, contentDescription = stringResource(R.string.subtitles_title), tint = Color.White)
                            }
                            IconButton(onClick = enterPiP) {
                                Icon(Icons.Default.PictureInPictureAlt, contentDescription = stringResource(R.string.action_pip), tint = Color.White)
                            }
                            IconButton(onClick = {
                                isLocked = true
                                controlsLastInteraction = System.currentTimeMillis()
                            }) {
                                Icon(Icons.Default.LockOpen, contentDescription = stringResource(R.string.action_lock), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        CenterPlaybackControlsRow(
            isPlaying = isPlaying,
            activeCenterAction = if (isLocked) CenterActionType.NONE else activeCenterAction,
            isCenterControlsVisible = false,
            playPausePulse = playPausePulse,
            seekBackwardPulse = seekBackwardPulse,
            seekForwardPulse = seekForwardPulse,
            consumedSeekBackwardPulse = consumedSeekBackwardPulse,
            consumedSeekForwardPulse = consumedSeekForwardPulse,
            onConsumeSeekBackward = { consumedSeekBackwardPulse = it },
            onConsumeSeekForward = { consumedSeekForwardPulse = it },
            lastSeekAmountSeconds = lastSeekAmountSeconds,
            onRewindClick = { performSeek(-1) },
            onForwardClick = { performSeek(1) },
            onPlayPauseClick = {
                if (isPlaying) {
                    mediaController?.pause()
                } else {
                    if (mediaController?.playbackState == Player.STATE_ENDED) {
                        mediaController?.seekTo(0L)
                    }
                    mediaController?.play()
                }
                playPausePulse++
                activeCenterAction = CenterActionType.PLAY_PAUSE
                centerActionTrigger = System.currentTimeMillis()
                controlsLastInteraction = System.currentTimeMillis()
            },
            modifier = Modifier.align(Alignment.Center),
            spacing = if (isLandscape) 80.dp else 48.dp
        )

        if (showPlaybackSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = playbackSpeed,
                skipSilenceEnabled = skipSilenceEnabled,
                onDismiss = { showPlaybackSpeedDialog = false },
                onConfirm = { speed, applyTemporarily, skipSilence ->
                    playbackSpeed = speed
                    mediaController?.setPlaybackSpeed(speed)
                    viewModel.setSkipSilenceEnabled(skipSilence, persist = !applyTemporarily)
                    if (!applyTemporarily) {
                        when {
                            speedScope == PlaybackSpeedScope.GLOBAL -> viewModel.setGlobalPlaybackSpeed(speed)
                            isHistory -> viewModel.setHistoryPlaybackSpeed(speed)
                            playlistId != null -> viewModel.setPlaylistPlaybackSpeed(playlistId, speed)
                            else -> viewModel.setFolderPlaybackSpeed(currentFolder, speed)
                        }
                    }
                    showPlaybackSpeedDialog = false
                }
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

        var detailedInfo by remember { mutableStateOf<com.mohamedaminelouati.oplayer.util.DetailedMediaInfo?>(null) }
        
        LaunchedEffect(showInfoDialog) {
            if (showInfoDialog) {
                val resolvedVideo = currentVideo ?: activeQueueVideos.find { it.uri.toString() == playingUri }
                val displayName = resolvedVideo?.name ?: videoName
                val displayPath = resolvedVideo?.path ?: Uri.parse(playingUri).path ?: playingUri
                val displaySize = resolvedVideo?.size ?: java.io.File(displayPath).let { if (it.exists()) it.length() else 0L }
                val displayDuration = if (resolvedVideo != null && resolvedVideo.duration > 0L) resolvedVideo.duration else duration
                val targetUri = resolvedVideo?.uri ?: Uri.parse(playingUri)
                
                detailedInfo = com.mohamedaminelouati.oplayer.util.extractDetailedMediaInfo(
                    context = context,
                    uri = targetUri,
                    fileName = displayName,
                    path = displayPath,
                    sizeBytes = displaySize,
                    durationMs = displayDuration
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

        if (showDeleteDialog) {
            val resolvedVideo = currentVideo ?: activeQueueVideos.find { it.uri.toString() == playingUri }
            val displayName = resolvedVideo?.name ?: videoName
            val targetUri = resolvedVideo?.uri ?: Uri.parse(playingUri)
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text(stringResource(R.string.delete_video_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.delete_media_confirm, displayName)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                viewModel.deleteHistoryItem(targetUri.toString())
                                viewModel.stopIfPlaying(targetUri)
                                val deleted = withContext(Dispatchers.IO) {
                                    context.contentResolver.delete(targetUri, null, null)
                                }
                                if (deleted > 0) viewModel.onVideosDeleted(listOf(targetUri))
                                else viewModel.refreshVideos(context)
                                onBack()
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
                title = { Text(stringResource(R.string.delete_video_title), fontWeight = FontWeight.Bold) },
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
                                val isCurrentDeleted = deletedUriStrings.contains(playingUri)
                                if (isCurrentDeleted) {
                                    viewModel.stopIfPlaying(Uri.parse(playingUri))
                                }
                                withContext(Dispatchers.IO) {
                                    urisToDelete.forEach { u ->
                                        try {
                                            context.contentResolver.delete(u, null, null)
                                        } catch (e: Exception) {
                                            android.util.Log.e("VideoPlayerScreen", "Failed to delete uri: $u", e)
                                        }
                                    }
                                }
                                val hasRemainingItems = queueItems.any { !deletedUriStrings.contains(it.uri) }
                                if (isCurrentDeleted) {
                                    val autoPlayNext = viewModel.autoPlayNext.value
                                    if (hasRemainingItems && autoPlayNext) {
                                        viewModel.removeQueueItems(deletedUriStrings, queueItems)
                                    } else {
                                        viewModel.removeQueueItems(deletedUriStrings, queueItems)
                                        onBack()
                                    }
                                } else {
                                    viewModel.removeQueueItems(deletedUriStrings, queueItems)
                                }
                                viewModel.onVideosDeleted(urisToDelete)
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
                        listOfNotNull(targetPlaylistUri ?: (mediaController?.currentMediaItem?.localConfiguration?.uri?.toString() ?: videoUri))
                    }
                    urisToAdd.forEach { uri ->
                        viewModel.addToPlaylist(playlistId, uri)
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

        if (showEqualizerDialog) {
            AudioEqualizerDialog(
                eqManager = viewModel.eqManager,
                onDismiss = { showEqualizerDialog = false }
            )
        }

        if (showQueueSheet) {
            com.mohamedaminelouati.oplayer.ui.feature.player.PlaybackQueueSheet(
                queueItems = queueItems,
                currentMediaIndex = currentMediaIndexState,
                isPlaying = isPlaying,
                sheetState = queueSheetState,
                onDismiss = { showQueueSheet = false },
                onItemClick = { index ->
                    showQueueSheet = false
                    val item = queueItems[index]
                    val isCurrent = index == currentMediaIndexState
                    if (!isCurrent) {
                        val qIndex = queueItems.indexOfFirst { it.uri == item.uri }
                        if (qIndex != -1 && qIndex < (mediaController?.mediaItemCount ?: 0)) {
                            mediaController?.seekToDefaultPosition(qIndex)
                        } else {
                            mediaController?.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(item.uri)))
                            mediaController?.prepare()
                        }
                        mediaController?.play()
                    } else {
                        if (mediaController?.isPlaying != true) {
                            mediaController?.play()
                        }
                    }
                },
                onItemRemove = { index ->
                    viewModel.removeQueueItem(index, queueItems)
                },
                onItemsRemove = { items ->
                    viewModel.removeQueueItems(items.map { it.uri }, queueItems)
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

        if (showSubtitleDialog) {
            SubtitleDialog(
                tracks = currentTracks,
                subtitleTextSize = subtitleTextSize,
                onSubtitleTextSizeChange = { viewModel.setSubtitleTextSize(it) },
                subtitleTextColor = subtitleTextColor,
                onSubtitleTextColorChange = { viewModel.setSubtitleTextColor(it) },
                subtitleBgStyle = subtitleBgStyle,
                onSubtitleBgStyleChange = { viewModel.setSubtitleBgStyle(it) },
                subtitleDelay = subtitleDelaySeconds,
                onSubtitleDelayChange = { delta ->
                    val newDelay = (subtitleDelaySeconds + delta).coerceIn(-10f, 10f)
                    subtitleDelaySeconds = newDelay
                },
                onDismiss = { showSubtitleDialog = false },
                onDisableSubtitles = {
                    mediaController?.trackSelectionParameters = mediaController?.trackSelectionParameters
                        ?.buildUpon()
                        ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        ?.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        ?.build() ?: return@SubtitleDialog
                },
                onSelectTrack = { group, trackIndex ->
                    mediaController?.trackSelectionParameters = mediaController?.trackSelectionParameters
                        ?.buildUpon()
                        ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        ?.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                        ?.build() ?: return@SubtitleDialog
                },
                onLoadExternal = {
                    subtitleLauncher.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                }
            )
        }

        if (playbackError != null) {
            PlaybackErrorOverlay(
                error = playbackError!!,
                onRetry = {
                    playbackError = null
                    mediaController?.prepare()
                    mediaController?.play()
                }
            )
        }
    }
}
