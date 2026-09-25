/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.ui.feature.player

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
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import com.arslandaim.omegaplayer.data.PlaybackSpeedScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.ui.common.SystemVolumeSyncEffect
import com.arslandaim.omegaplayer.ui.common.applySystemMusicVolume
import com.arslandaim.omegaplayer.ui.common.rememberInitialSystemBrightness
import com.arslandaim.omegaplayer.ui.common.rememberInitialSystemVolume
import com.arslandaim.omegaplayer.ui.common.rememberSystemAudioManager
import com.arslandaim.omegaplayer.ui.common.rememberSystemMaxVolume
import com.arslandaim.omegaplayer.ui.common.setBrightness
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

import com.arslandaim.omegaplayer.util.MediaUtils
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel

private const val TAG = "VideoPlayerScreen"

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
    val isBackgroundPlayEnabled by viewModel.isBackgroundPlayEnabled.collectAsStateWithLifecycle()
    val controlsTimeout by viewModel.controlsTimeout.collectAsStateWithLifecycle(initialValue = 3)
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val sleepTimerActive by viewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val sleepTimerTimeLeft by viewModel.sleepTimerTimeLeft.collectAsStateWithLifecycle()
    val stopAfterCurrent by viewModel.stopAfterCurrent.collectAsStateWithLifecycle()
    val volumeBoostEnabled by viewModel.volumeBoostEnabled.collectAsStateWithLifecycle()

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var currentTracks by remember { mutableStateOf(mediaController?.currentTracks ?: Tracks.EMPTY) }
    var showQueueSheet by remember { mutableStateOf(false) }
    val upNextFullyExpanded by viewModel.upNextFullyExpanded.collectAsStateWithLifecycle(initialValue = false)
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = upNextFullyExpanded)

    val subtitleTextSize by viewModel.subtitleTextSize.collectAsStateWithLifecycle(initialValue = 18)
    val subtitleTextColor by viewModel.subtitleTextColor.collectAsStateWithLifecycle(initialValue = 0)
    val subtitleBgStyle by viewModel.subtitleBgStyle.collectAsStateWithLifecycle(initialValue = 1)
    var subtitleDelaySeconds by remember { mutableFloatStateOf(0f) }

    var isVerticalVideo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var pendingUrisToDelete by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingUrisToDelete.forEach { uri -> viewModel.deleteHistoryItem(uri.toString()) }
            val autoPlayNext = viewModel.autoPlayNext.value
            val hasNext = mediaController?.hasNextMediaItem() == true
            if (hasNext && autoPlayNext) {
                val currentIndex = mediaController?.currentMediaItemIndex ?: -1
                if (currentIndex != -1) {
                    mediaController?.removeMediaItem(currentIndex)
                }
            } else {
                mediaController?.currentMediaItem?.localConfiguration?.uri?.let { viewModel.stopIfPlaying(it) }
                onBack()
            }
            if (pendingUrisToDelete.isNotEmpty()) viewModel.onVideosDeleted(pendingUrisToDelete)
            pendingUrisToDelete = emptyList()
        } else {
            pendingUrisToDelete = emptyList()
        }
    }

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
            try {
                activity?.enterPictureInPictureMode(builder.build())
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Failed to enter picture-in-picture", e)
            }
        }
    }

    val subtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "Failed to persist subtitle URI permission", e)
            }

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
    val playingUri = mediaController?.currentMediaItem?.localConfiguration?.uri?.toString() ?: videoUri
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
    val playerOrientation by viewModel.playerOrientation.collectAsStateWithLifecycle(initialValue = 0)

    val speedScope by viewModel.speedScope.collectAsStateWithLifecycle(initialValue = PlaybackSpeedScope.GLOBAL)
    val globalSpeed by viewModel.globalPlaybackSpeed.collectAsStateWithLifecycle(initialValue = 1.0f)
    val folderSpeedFlow = remember(currentFolder) { viewModel.getFolderSpeed(currentFolder) }
    val folderSpeed by folderSpeedFlow.collectAsStateWithLifecycle(initialValue = 1.0f)
    val effectiveSpeed = if (speedScope == PlaybackSpeedScope.GLOBAL) globalSpeed else (folderSpeed ?: 1.0f)

    val showSystemStatusBar by viewModel.showSystemStatusBar.collectAsStateWithLifecycle(initialValue = true)
    val hudShowClock by viewModel.showPlayerClock.collectAsStateWithLifecycle(initialValue = false)
    val hudShowBattery by viewModel.showPlayerBattery.collectAsStateWithLifecycle(initialValue = false)
    val hudShowMediaInfo by viewModel.showPlayerMediaInfo.collectAsStateWithLifecycle(initialValue = false)
    val hudShowVolume by viewModel.showPlayerVolume.collectAsStateWithLifecycle(initialValue = false)
    val hudShowBrightness by viewModel.showPlayerBrightness.collectAsStateWithLifecycle(initialValue = false)

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
        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to register battery receiver", e)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to unregister battery receiver", e)
            }
        }
    }

    var videoResolution by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isPlaying) {
        activity?.window?.let { window ->
            if (isPlaying) {
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

    SystemVolumeSyncEffect(
        audioManager = audioManager,
        volumeProvider = { volume },
        onVolumeChange = { volume = it },
        onVolumeBoostReset = { viewModel.eqManager.setVolumeBoostScale(1.0f) }
    )
    var isControlsVisible by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var pendingSeekPosition by remember { mutableLongStateOf(-1L) }
    var seekGracePeriod by remember { mutableLongStateOf(0L) }
    var controlsLastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isControlsVisible, controlsLastInteraction, isDraggingSlider, isPlaying, controlsTimeout) {
        if (isControlsVisible && !isDraggingSlider && isPlaying && controlsTimeout > 0) {
            delay(controlsTimeout * 1000L)
            isControlsVisible = false
        }
    }

    var playbackSpeed by remember { mutableFloatStateOf(effectiveSpeed) }
    var isLandscape by rememberSaveable { mutableStateOf(false) }
    var aspectRatio by remember { mutableIntStateOf(0) }

    var repeatMode by remember { mutableIntStateOf(mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    var isShuffle by remember { mutableStateOf(mediaController?.shuffleModeEnabled ?: false) }
    var currentMediaIndexState by remember { mutableIntStateOf(mediaController?.currentMediaItemIndex ?: 0) }
    var hasNext by remember { mutableStateOf(mediaController?.hasNextMediaItem() ?: false) }
    var hasPrevious by remember { mutableStateOf(mediaController?.hasPreviousMediaItem() ?: false) }

    val onSpeedChange: (Float) -> Unit = { speed ->
        playbackSpeed = speed
        mediaController?.setPlaybackSpeed(speed)
        if (speedScope == PlaybackSpeedScope.GLOBAL) {
            viewModel.setGlobalPlaybackSpeed(speed)
        } else {
            viewModel.setFolderPlaybackSpeed(currentFolder, speed)
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

    var showInfoDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackgroundPlay = rememberUpdatedState(isBackgroundPlayEnabled)

    var hasSeekedInitialPosition by remember(videoUri, initialPosition) { mutableStateOf(false) }
    LaunchedEffect(currentMediaIndexState) {
        mediaController?.let { player ->
            if (playbackSpeed != effectiveSpeed) {
                playbackSpeed = effectiveSpeed
                player.setPlaybackSpeed(effectiveSpeed)
            }
        }
    }

    LaunchedEffect(videoUri, mediaController, initialPosition, effectiveSpeed) {
        val player = mediaController ?: return@LaunchedEffect
        val newUri = Uri.parse(videoUri)
        val currentUri = player.currentMediaItem?.localConfiguration?.uri
        val targetPos = if (initialPosition >= 0L) initialPosition else viewModel.getSavedPosition(videoUri)
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
                if (state == Player.STATE_READY) {
                    playbackError = null
                    if (player.videoSize.width > 0 && player.videoSize.height > 0) {
                        videoResolution = "${player.videoSize.width}x${player.videoSize.height}"
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
            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }
            override fun onShuffleModeEnabledChanged(shuffle: Boolean) {
                isShuffle = shuffle
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaIndexState = player.currentMediaItemIndex
                if (mediaItem != null) {
                    val uri = mediaItem.localConfiguration?.uri?.toString() ?: ""
                    if (uri.isNotEmpty() && uri != videoUri && MediaUtils.isAudioMediaItem(mediaItem)) {
                        onAudioTransition(uri)
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
        currentTracks = player.currentTracks
        if (player.videoSize.width > 0 && player.videoSize.height > 0) {
            videoResolution = "${player.videoSize.width}x${player.videoSize.height}"
            isVerticalVideo = player.videoSize.height > player.videoSize.width
        }
        repeatMode = player.repeatMode
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

            viewModel.savePlaybackProgress()
            
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
    val transformState = rememberTransformableState { _, zoomChange, offsetChange, _ ->
        if (!isLocked) {
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            panOffset += offsetChange
        }
    }

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
    
    var showPlayPausePulse by remember { mutableStateOf<Boolean?>(null) }
    var pulseTrigger by remember { mutableIntStateOf(0) }

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

    val videoName = remember(videoUri, videosList, currentVideo) {
        val foundName = currentVideo?.name ?: videosList.find { it.uri.toString() == videoUri }?.name
        if (!foundName.isNullOrBlank()) {
            foundName
        } else {
            var queriedName: String? = null
            try {
                val parsedUri = Uri.parse(videoUri)
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
            } catch (e: RuntimeException) {
                queriedName = null
                Log.w(TAG, "Failed to query video display name", e)
            }
            queriedName ?: videoUri.substringAfterLast("/").substringBeforeLast(".")
        }
    }

    var showMoreMenu by remember { mutableStateOf(false) }

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
        if (!isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
                    }
                    Text(
                        text = videoName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .basicMarquee()
                    )
                    IconButton(onClick = { showQueueSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.queue), tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
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
                                com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
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

                Box(
                    modifier = if (isVerticalVideo) {
                        Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .background(Color.Black)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    }
                ) {
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
                                .sharedBounds(
                                    rememberSharedContentState(key = "video_bounds_$videoUri"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isLocked) {
                                if (!isLocked) {
                                    kotlinx.coroutines.coroutineScope {
                                        launch {
                                            detectTapGestures(
                                                onTap = { offset ->
                                                    val isCenter = offset.x > size.width * 0.2f && offset.x < size.width * 0.8f && offset.y > size.height * 0.2f && offset.y < size.height * 0.8f
                                                    if (isCenter && !isControlsVisible) {
                                                        val p = mediaController
                                                        if (p != null) {
                                                            if (p.isPlaying) p.pause() else p.play()
                                                            showPlayPausePulse = !p.isPlaying
                                                            pulseTrigger++
                                                        }
                                                        isControlsVisible = true
                                                    } else {
                                                        isControlsVisible = !isControlsVisible
                                                    }
                                                    controlsLastInteraction = System.currentTimeMillis()
                                                },
                                                onDoubleTap = { offset ->
                                                    val p = mediaController ?: return@detectTapGestures
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    if (offset.x < size.width / 2) {
                                                        p.seekBack()
                                                        seekBackwardPulse++
                                                        isControlsVisible = true
                                                    } else {
                                                        p.seekForward()
                                                        seekForwardPulse++
                                                        isControlsVisible = true
                                                    }
                                                }
                                            )
                                        }
                                        launch {
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
                                                    if (abs(volume - oldVolume) > 0.05f) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    applySystemMusicVolume(audioManager, volume, maxVolume)
                                                    if (volumeBoostEnabled) {
                                                        viewModel.eqManager.setVolumeBoostScale(if (volume > 1f) volume else 1.0f)
                                                    }
                                                    isVolumeVisible = true
                                                } else {
                                                    val oldBrightness = brightness
                                                    brightness = (brightness - dragAmount.y / size.height).coerceIn(0.01f, 1f)
                                                    if (abs(brightness - oldBrightness) > 0.05f) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    setBrightness(context, brightness)
                                                    isBrightnessVisible = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    )

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

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isControlsVisible && !isLocked,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(48.dp)
                        ) {
                            SeekControlButton(
                                icon = Icons.Default.FastRewind,
                                contentDescription = stringResource(R.string.action_rewind),
                                onClick = { mediaController?.seekBack() },
                                trigger = seekBackwardPulse,
                                consumedTrigger = consumedSeekBackwardPulse,
                                onConsume = { consumedSeekBackwardPulse = it }
                            )
                            CenterPlayPauseButton(
                                isPlaying = isPlaying,
                                onClick = {
                                    if (isPlaying) mediaController?.pause() else mediaController?.play()
                                }
                            )
                            SeekControlButton(
                                icon = Icons.Default.FastForward,
                                contentDescription = stringResource(R.string.action_forward),
                                onClick = { mediaController?.seekForward() },
                                trigger = seekForwardPulse,
                                consumedTrigger = consumedSeekForwardPulse,
                                onConsume = { consumedSeekForwardPulse = it }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(0.2f))

                    Column(modifier = Modifier.fillMaxWidth()) {
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
                            },
                            textsBelow = true
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val nextMode = when (mediaController?.repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                            mediaController?.repeatMode = nextMode
                            repeatMode = nextMode
                        }) {
                            Icon(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = stringResource(R.string.action_repeat),
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(onClick = { mediaController?.seekToPrevious() }, enabled = hasPrevious) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.action_previous), modifier = Modifier.size(36.dp), tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f))
                        }

                        FilledIconButton(
                            onClick = {
                                if (mediaController?.isPlaying == true) mediaController?.pause() else mediaController?.play()
                            },
                            modifier = Modifier.size(76.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(onClick = { mediaController?.seekToNext() }, enabled = hasNext) {
                            Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.action_next), modifier = Modifier.size(36.dp), tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f))
                        }

                        IconButton(onClick = {
                            val nextShuffle = !(mediaController?.shuffleModeEnabled ?: false)
                            mediaController?.shuffleModeEnabled = nextShuffle
                            isShuffle = nextShuffle
                        }) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = stringResource(R.string.action_shuffle),
                                tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playerOrientation == 2) { IconButton(onClick = { isLandscape = true; activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }) { Icon(Icons.Default.Fullscreen, contentDescription = stringResource(R.string.action_fullscreen), tint = Color.White) } }
                        IconButton(onClick = { aspectRatio = (aspectRatio + 1) % 3 }) {
                            Icon(when(aspectRatio) { 1 -> Icons.Default.Fullscreen; 2 -> Icons.Default.AspectRatio; else -> Icons.Default.FitScreen }, contentDescription = stringResource(R.string.action_aspect_ratio), tint = Color.White)
                        }

                        IconButton(onClick = {
                            showSubtitleDialog = true
                        }) {
                            Icon(Icons.Default.Subtitles, contentDescription = stringResource(R.string.subtitles_title), tint = Color.White)
                        }
                        IconButton(onClick = enterPiP) {
                            Icon(Icons.Default.PictureInPictureAlt, contentDescription = stringResource(R.string.action_pip), tint = Color.White)
                        }
                        IconButton(onClick = { isLocked = !isLocked }) {
                            Icon(
                                if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = stringResource(R.string.action_lock),
                                tint = if (isLocked) Color.Red else Color.White
                            )
                        }
                    }
                }
            }
        } else {
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
                        .sharedBounds(
                            rememberSharedContentState(key = "video_bounds_$videoUri"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = panOffset.x,
                            translationY = panOffset.y
                        )
                        .transformable(state = transformState)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked) {
                        if (isLocked) {
                            detectTapGestures(onTap = { isControlsVisible = !isControlsVisible })
                        } else {
                            detectTapGestures(
                                onTap = { offset ->
                                    val isCenter = offset.x > size.width * 0.2f && offset.x < size.width * 0.8f && offset.y > size.height * 0.2f && offset.y < size.height * 0.8f
                                    if (isCenter && !isControlsVisible) {
                                        val p = mediaController
                                        if (p != null) {
                                            if (p.isPlaying) p.pause() else p.play()
                                            showPlayPausePulse = !p.isPlaying
                                            pulseTrigger++
                                        }
                                        isControlsVisible = true
                                    } else {
                                        isControlsVisible = !isControlsVisible
                                    }
                                    controlsLastInteraction = System.currentTimeMillis()
                                },
                                onDoubleTap = { offset ->
                                    val player = mediaController ?: return@detectTapGestures
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (offset.x < size.width / 2) {
                                        player.seekBack()
                                        seekBackwardPulse++
                                        isControlsVisible = true
                                    } else {
                                        player.seekForward()
                                        seekForwardPulse++
                                        isControlsVisible = true
                                    }
                                },
                                onLongPress = {
                                    val player = mediaController ?: return@detectTapGestures
                                    if (player.isPlaying) {
                                        player.pause()
                                        showPlayPausePulse = false
                                    } else {
                                        player.play()
                                        showPlayPausePulse = true
                                    }
                                    pulseTrigger++
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
                    }
                    .pointerInput(isLocked) {
                        if (!isLocked) {
                            detectDragGestures(
                                onDragStart = { },
                                onDragEnd = {
                                    isVolumeVisible = false
                                    isBrightnessVisible = false
                                },
                                onDrag = { change, dragAmount ->
                                    val width = size.width
                                    val height = size.height
                                    if (abs(dragAmount.y) > abs(dragAmount.x)) {
                                        if (change.position.x < width / 2) {
                                            val oldBrightness = brightness
                                            brightness = (brightness - dragAmount.y / height).coerceIn(0.01f, 1f)
                                            if (abs(brightness - oldBrightness) > 0.05f) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            setBrightness(context, brightness)
                                            isBrightnessVisible = true
                                        } else {
                                            val oldVolume = volume
                                            val maxVolMultiplier = if (volumeBoostEnabled) 2f else 1f
                                            volume = (volume - dragAmount.y / height).coerceIn(0f, maxVolMultiplier)
                                            if (abs(volume - oldVolume) > 0.05f) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            applySystemMusicVolume(audioManager, volume, maxVolume)
                                            if (volumeBoostEnabled) {
                                                viewModel.eqManager.setVolumeBoostScale(if (volume > 1f) volume else 1.0f)
                                            }
                                            isVolumeVisible = true
                                        }
                                    }
                                }
                            )
                        }
                    }
            )

            if (isLocked && isControlsVisible) {
                IconButton(
                    onClick = { isLocked = false },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.action_unlock), tint = Color.Red)
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
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(80.dp)
                    ) {
                        SeekControlButton(
                            icon = Icons.Default.FastRewind,
                            contentDescription = stringResource(R.string.action_rewind),
                            onClick = { mediaController?.seekBack() },
                            trigger = seekBackwardPulse,
                            consumedTrigger = consumedSeekBackwardPulse,
                            onConsume = { consumedSeekBackwardPulse = it }
                        )
                        CenterPlayPauseButton(
                            isPlaying = isPlaying,
                            onClick = {
                                if (isPlaying) mediaController?.pause() else mediaController?.play()
                            }
                        )
                        SeekControlButton(
                            icon = Icons.Default.FastForward,
                            contentDescription = stringResource(R.string.action_forward),
                            onClick = { mediaController?.seekForward() },
                            trigger = seekForwardPulse,
                            consumedTrigger = consumedSeekForwardPulse,
                            onConsume = { consumedSeekForwardPulse = it }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                isLandscape = false
                                activity?.requestedOrientation = if (playerOrientation == 0) ActivityInfo.SCREEN_ORIENTATION_USER else if (playerOrientation == 1) ActivityInfo.SCREEN_ORIENTATION_SENSOR else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
                            }
                            Text(
                                text = videoName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .basicMarquee()
                            )
                            IconButton(onClick = { showQueueSheet = true }) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.queue), tint = Color.White)
                            }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
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
                                        com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
                                            context = context,
                                            uris = listOf(targetUri),
                                            deleteLauncher = deleteLauncher,
                                            onRequireInternalPopup = { showDeleteDialog = true }
                                        )
                                    },
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
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val nextMode = when (mediaController?.repeatMode) {
                                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                        else -> Player.REPEAT_MODE_OFF
                                    }
                                    mediaController?.repeatMode = nextMode
                                    repeatMode = nextMode
                                }) {
                                    Icon(
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                            else -> Icons.Default.Repeat
                                        },
                                        contentDescription = stringResource(R.string.action_repeat),
                                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(onClick = {
                                    val nextShuffle = !(mediaController?.shuffleModeEnabled ?: false)
                                    mediaController?.shuffleModeEnabled = nextShuffle
                                    isShuffle = nextShuffle
                                }) {
                                    Icon(
                                        Icons.Default.Shuffle,
                                        contentDescription = stringResource(R.string.action_shuffle),
                                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                                    )
                                }

                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { mediaController?.seekToPrevious() }, enabled = hasPrevious) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.action_previous), modifier = Modifier.size(36.dp), tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f))
                                }
                                FilledIconButton(
                                    onClick = {
                                        if (mediaController?.isPlaying == true) mediaController?.pause() else mediaController?.play()
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
                                IconButton(onClick = { mediaController?.seekToNext() }, enabled = hasNext) {
                                    Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.action_next), modifier = Modifier.size(36.dp), tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f))
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showSubtitleDialog = true }) {
                                    Icon(Icons.Default.Subtitles, contentDescription = stringResource(R.string.subtitles_title), tint = Color.White)
                                }
                                IconButton(onClick = enterPiP) {
                                    Icon(Icons.Default.PictureInPictureAlt, contentDescription = stringResource(R.string.action_pip), tint = Color.White)
                                }
                                IconButton(onClick = { aspectRatio = (aspectRatio + 1) % 3 }) {
                                    Icon(when(aspectRatio) { 1 -> Icons.Default.Fullscreen; 2 -> Icons.Default.AspectRatio; else -> Icons.Default.FitScreen }, contentDescription = stringResource(R.string.action_aspect_ratio), tint = Color.White)
                                }
                                IconButton(onClick = { isLocked = true; isControlsVisible = false }) {
                                    Icon(Icons.Default.LockOpen, contentDescription = stringResource(R.string.action_lock), tint = Color.White)
                                }
                                if (playerOrientation == 2) { IconButton(onClick = { isLandscape = false; activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }) { Icon(Icons.Default.FullscreenExit, contentDescription = stringResource(R.string.action_exit_fullscreen), tint = Color.White) } }
                            }
                        }
                    }
                }
            }
        }

        if (showPlaybackSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = playbackSpeed,
                onDismiss = { showPlaybackSpeedDialog = false },
                onConfirm = { speed, applyTemporarily ->
                    playbackSpeed = speed
                    mediaController?.setPlaybackSpeed(speed)
                    if (!applyTemporarily) {
                        if (speedScope == PlaybackSpeedScope.GLOBAL) {
                            viewModel.setGlobalPlaybackSpeed(speed)
                        } else {
                            viewModel.setFolderPlaybackSpeed(currentFolder, speed)
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

        if (showInfoDialog) {
            val resolvedVideo = currentVideo ?: activeQueueVideos.find { it.uri.toString() == playingUri }
            val displayName = resolvedVideo?.name ?: videoName
            val displayPath = resolvedVideo?.path ?: Uri.parse(playingUri).path ?: playingUri
            val displaySize = if (resolvedVideo != null && resolvedVideo.size > 0L) {
                stringResource(R.string.file_size_mb_format, resolvedVideo.size / (1024 * 1024))
            } else {
                val file = java.io.File(displayPath)
                if (file.exists()) stringResource(R.string.file_size_mb_format, file.length() / (1024 * 1024)) else stringResource(R.string.unknown)
            }
            val displayDuration = if (resolvedVideo != null && resolvedVideo.duration > 0L) {
                formatDuration(resolvedVideo.duration)
            } else {
                formatDuration(duration)
            }
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text(stringResource(R.string.video_info), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(stringResource(R.string.info_name), displayName)
                        InfoRow(stringResource(R.string.info_size), displaySize)
                        InfoRow(stringResource(R.string.info_path), displayPath)
                        InfoRow(stringResource(R.string.info_duration), displayDuration)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) { Text(stringResource(R.string.action_close)) }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        if (showDeleteDialog) {
            val resolvedVideo = currentVideo ?: activeQueueVideos.find { it.uri.toString() == playingUri }
            val displayName = resolvedVideo?.name ?: videoName
            val targetUri = resolvedVideo?.uri ?: Uri.parse(playingUri)
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
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

        if (showPlaylistDialog) {
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { showPlaylistDialog = false },
                onPlaylistSelected = { playlistId ->
                    val currentUri = mediaController?.currentMediaItem?.localConfiguration?.uri?.toString() ?: videoUri
                    viewModel.addToPlaylist(playlistId, currentUri)
                    showPlaylistDialog = false
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
            val queueItems = if (activeQueue.isNotEmpty()) {
                activeQueue
            } else if (activeQueueVideos.isNotEmpty()) {
                activeQueueVideos.map {
                    com.arslandaim.omegaplayer.media.PlaybackQueueItem(
                        uri = it.uri.toString(),
                        title = it.name,
                        duration = it.duration,
                        isVideo = true
                    )
                }
            } else {
                listOf(
                    com.arslandaim.omegaplayer.media.PlaybackQueueItem(
                        uri = videoUri,
                        title = stringResource(R.string.unknown_title),
                        duration = 0L,
                        isVideo = true
                    )
                )
            }

            com.arslandaim.omegaplayer.ui.feature.player.PlaybackQueueSheet(
                queueItems = queueItems,
                currentMediaIndex = currentMediaIndexState,
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
                    }
                }
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
