/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.ui.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
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
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arslandaim.omegaplayer.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

import com.arslandaim.omegaplayer.util.MediaUtils
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel

@AndroidOptIn(UnstableApi::class)
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoUri: String, 
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
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val activity = context as? Activity
    val hazeState = remember { HazeState() }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }
    
    val mediaController by viewModel.mediaController.collectAsStateWithLifecycle()
    val isBackgroundPlayEnabled by viewModel.isBackgroundPlayEnabled.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val sleepTimerActive by viewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val sleepTimerTimeLeft by viewModel.sleepTimerTimeLeft.collectAsStateWithLifecycle()

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var currentTracks by remember { mutableStateOf(mediaController?.currentTracks ?: Tracks.EMPTY) }
    var showQueueSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState()

    val subtitleTextSize by viewModel.subtitleTextSize.collectAsStateWithLifecycle(initialValue = 18)
    val subtitleTextColor by viewModel.subtitleTextColor.collectAsStateWithLifecycle(initialValue = 0)
    val subtitleBgStyle by viewModel.subtitleBgStyle.collectAsStateWithLifecycle(initialValue = 1)
    var subtitleDelaySeconds by remember { mutableFloatStateOf(0f) }

    var isVerticalVideo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mediaController?.currentMediaItem?.localConfiguration?.uri?.let { viewModel.stopIfPlaying(it) }
            viewModel.refreshVideos(context)
            onBack()
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
                builder.setAutoEnterEnabled(true)
            }
            try {
                activity?.enterPictureInPictureMode(builder.build())
            } catch (_: Exception) {}
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
            } catch (_: Exception) {}

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
            java.io.File(path).parentFile?.name ?: "Internal"
        } else {
            "Internal"
        }
    }

    val folderVideos = remember(currentFolder, videosList) {
        viewModel.getVideosInFolder(currentFolder)
    }
    val activeQueueVideos = remember(folderVideos, videosList) {
        if (folderVideos.isNotEmpty()) folderVideos else videosList
    }
    val activeQueue by viewModel.activeQueue.collectAsStateWithLifecycle()

    LaunchedEffect(folderVideos, activeQueue) {
        if (activeQueue.isEmpty() && folderVideos.isNotEmpty()) {
            viewModel.setFolderQueue(folderVideos)
        }
    }

    val speedScope by viewModel.speedScope.collectAsStateWithLifecycle(initialValue = PlaybackSpeedScope.GLOBAL)
    val globalSpeed by viewModel.globalPlaybackSpeed.collectAsStateWithLifecycle(initialValue = 1.0f)
    val folderSpeedFlow = remember(currentFolder) { viewModel.getFolderSpeed(currentFolder) }
    val folderSpeed by folderSpeedFlow.collectAsStateWithLifecycle(initialValue = 1.0f)
    val effectiveSpeed = if (speedScope == PlaybackSpeedScope.GLOBAL) globalSpeed else (folderSpeed ?: 1.0f)

    val hudShowClock by viewModel.showPlayerClock.collectAsStateWithLifecycle(initialValue = true)
    val hudShowBattery by viewModel.showPlayerBattery.collectAsStateWithLifecycle(initialValue = true)
    val hudShowMediaInfo by viewModel.showPlayerMediaInfo.collectAsStateWithLifecycle(initialValue = true)
    val hudShowVolume by viewModel.showPlayerVolume.collectAsStateWithLifecycle(initialValue = true)
    val hudShowBrightness by viewModel.showPlayerBrightness.collectAsStateWithLifecycle(initialValue = true)

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember(audioManager) { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

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
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
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

    val initialVolume = remember(audioManager, maxVolume) {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)
        if (maxVolume > 0) (current.toFloat() / maxVolume).coerceIn(0f, 1f) else 0.5f
    }
    var volume by remember { mutableFloatStateOf(initialVolume) }

    val initialBrightness = remember(context, activity) {
        val winBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        if (winBrightness >= 0.01f) {
            winBrightness.coerceIn(0.01f, 1f)
        } else {
            try {
                val sysBrightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                (sysBrightness / 255f).coerceIn(0.01f, 1f)
            } catch (_: Exception) {
                0.5f
            }
        }
    }
    var brightness by remember { mutableFloatStateOf(initialBrightness) }

    val updateVolumeFromSystem: () -> Unit = {
        audioManager?.let { am ->
            val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) {
                volume = (cur.toFloat() / max).coerceIn(0f, 1f)
            }
        }
    }

    DisposableEffect(context, audioManager) {
        updateVolumeFromSystem()
        val contentObserver = object : android.database.ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                updateVolumeFromSystem()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            contentObserver
        )
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                updateVolumeFromSystem()
            }
        }
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.contentResolver.unregisterContentObserver(contentObserver)
            context.unregisterReceiver(receiver)
        }
    }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    var playbackSpeed by remember { mutableFloatStateOf(effectiveSpeed) }
    var isLandscape by rememberSaveable { mutableStateOf(false) }
    var aspectRatio by remember { mutableIntStateOf(0) }
    var isHardwareAccelerated by rememberSaveable { mutableStateOf(true) }

    var repeatMode by remember { mutableIntStateOf(mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    var isShuffle by remember { mutableStateOf(mediaController?.shuffleModeEnabled ?: false) }

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
            Toast.makeText(context, "Unlock the player to exit", Toast.LENGTH_SHORT).show()
        } else if (isLandscape) {
            isLandscape = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onBack()
        }
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackgroundPlay = rememberUpdatedState(isBackgroundPlayEnabled)

    var hasSeekedInitialPosition by remember(videoUri, initialPosition) { mutableStateOf(false) }

    LaunchedEffect(videoUri, mediaController, activeQueueVideos, initialPosition, effectiveSpeed) {
        val player = mediaController ?: return@LaunchedEffect
        val newUri = Uri.parse(videoUri)
        val currentUri = player.currentMediaItem?.localConfiguration?.uri
        val targetPos = if (initialPosition >= 0L) initialPosition else viewModel.getSavedPosition(videoUri)
        if (currentUri != newUri) {
            playbackError = null
            var matchedIndex = -1
            for (i in 0 until player.mediaItemCount) {
                if (player.getMediaItemAt(i).localConfiguration?.uri == newUri) {
                    matchedIndex = i
                    break
                }
            }
            if (matchedIndex != -1) {
                player.seekTo(matchedIndex, targetPos.coerceAtLeast(0L))
            } else if (activeQueueVideos.isNotEmpty()) {
                val mediaItems = activeQueueVideos.map { videoItem ->
                    MediaItem.Builder()
                        .setUri(videoItem.uri)
                        .setMediaId(videoItem.id.toString())
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(videoItem.name)
                                .build()
                        )
                        .build()
                }
                val index = activeQueueVideos.indexOfFirst { it.uri.toString() == videoUri }.coerceAtLeast(0)
                player.setMediaItems(mediaItems, index, targetPos.coerceAtLeast(0L))
                player.prepare()
            } else {
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(MediaItem.fromUri(newUri))
                player.prepare()
                if (targetPos > 0L) {
                    player.seekTo(targetPos)
                }
            }
            hasSeekedInitialPosition = true
        } else {
            if (!hasSeekedInitialPosition && targetPos > 0L) {
                player.seekTo(targetPos)
                hasSeekedInitialPosition = true
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
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "File not found"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network error"
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Decoder error"
                    else -> "Unexpected error"
                }
                playbackError = "$errorType: ${error.message}"
                Log.e("PlayerScreen", "ExoPlayer Error ($errorType): ${error.message}", error)
                
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
                if (mediaItem != null) {
                    val uri = mediaItem.localConfiguration?.uri?.toString() ?: ""
                    if (uri.isNotEmpty() && uri != videoUri && MediaUtils.isAudioMediaItem(mediaItem)) {
                        onAudioTransition(uri)
                    }
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

    DisposableEffect(lifecycleOwner, mediaController) {
        val player = mediaController
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (!currentBackgroundPlay.value && !isInPiPMode) {
                    player?.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
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
    
    // UI HUD States
    var showSeekForwardAnimation by remember { mutableStateOf(false) }
    var showSeekBackwardAnimation by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        if (!isLocked) {
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            panOffset += offsetChange
        }
    }

    var isVolumeVisible by remember { mutableStateOf(false) }
    var isBrightnessVisible by remember { mutableStateOf(false) }
    var isSeekHUDVisible by remember { mutableStateOf(false) }
    var seekOffsetHUD by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    var showPlayPausePulse by remember { mutableStateOf<Boolean?>(null) } // null: none, true: play, false: pause
    var pulseTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(mediaController) {
        while (true) {
            mediaController?.let {
                currentPosition = it.currentPosition
                duration = it.duration
            }
            delay(500.milliseconds)
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
            } catch (e: Exception) {
                Log.e("PlayerScreen", "Error resolving video name", e)
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
                            onSpeedChange = onSpeedChange,
                            onEqualizerClick = { showEqualizerDialog = true },
                            onInfoClick = { showInfoDialog = true },
                            onDeleteClick = { showDeleteDialog = true }
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
                                    detectTapGestures(
                                        onTap = {
                                            val p = mediaController ?: return@detectTapGestures
                                            if (p.isPlaying) p.pause() else p.play()
                                        },
                                        onDoubleTap = { offset ->
                                            val p = mediaController ?: return@detectTapGestures
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (offset.x < size.width / 2) {
                                                p.seekBack()
                                                showSeekBackwardAnimation = true
                                            } else {
                                                p.seekForward()
                                                showSeekForwardAnimation = true
                                            }
                                        }
                                    )
                                }
                            }
                            .pointerInput(isLocked) {
                                if (!isLocked) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val isRightSide = change.position.x > size.width / 2
                                        if (isRightSide) {
                                            val oldVolume = volume
                                            volume = (volume - dragAmount.y / size.height).coerceIn(0f, 1f)
                                            if (abs(volume - oldVolume) > 0.05f) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            audioManager?.let { am ->
                                                val targetVol = kotlin.math.round(volume * maxVolume).toInt()
                                                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                            }
                                        } else {
                                            val oldBrightness = brightness
                                            brightness = (brightness - dragAmount.y / size.height).coerceIn(0.01f, 1f)
                                            if (abs(brightness - oldBrightness) > 0.05f) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            setBrightness(context, brightness)
                                        }
                                    }
                                }
                            }
                    )

                    SeekAnimationOverlay(
                        isVisible = showSeekBackwardAnimation,
                        isForward = false,
                        onAnimationFinished = { showSeekBackwardAnimation = false }
                    )
                    SeekAnimationOverlay(
                        isVisible = showSeekForwardAnimation,
                        isForward = true,
                        onAnimationFinished = { showSeekForwardAnimation = false }
                    )
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
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                            onValueChange = { mediaController?.seekTo((it * duration).toLong()) },
                            modifier = Modifier.fillMaxWidth(),
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
                            Text(formatTime(currentPosition), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(formatTime(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
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
                                contentDescription = "Repeat",
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(onClick = { mediaController?.seekToPreviousMediaItem() }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp), tint = Color.White)
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
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(onClick = { mediaController?.seekToNextMediaItem() }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp), tint = Color.White)
                        }

                        IconButton(onClick = {
                            val nextShuffle = !(mediaController?.shuffleModeEnabled ?: false)
                            mediaController?.shuffleModeEnabled = nextShuffle
                            isShuffle = nextShuffle
                        }) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
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
                        IconButton(onClick = {
                            isLandscape = true
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                        }
                        IconButton(onClick = { aspectRatio = (aspectRatio + 1) % 3 }) {
                            Icon(when(aspectRatio) { 1 -> Icons.Default.Fullscreen; 2 -> Icons.Default.AspectRatio; else -> Icons.Default.FitScreen }, contentDescription = "Aspect Ratio", tint = Color.White)
                        }
                        IconButton(onClick = {
                            isHardwareAccelerated = !isHardwareAccelerated
                            val mode = if (isHardwareAccelerated) "Hardware" else "Software"
                            Toast.makeText(context, "$mode Decoding Active", Toast.LENGTH_SHORT).show()
                        }) {
                            Text(
                                text = if (isHardwareAccelerated) "HW" else "SW",
                                color = if (isHardwareAccelerated) Color(0xFF4CAF50) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
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
                                contentDescription = "Lock",
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
                                onTap = { isControlsVisible = !isControlsVisible },
                                onDoubleTap = { offset ->
                                    val player = mediaController ?: return@detectTapGestures
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (offset.x < size.width / 2) {
                                        player.seekBack()
                                        showSeekBackwardAnimation = true
                                    } else {
                                        player.seekForward()
                                        showSeekForwardAnimation = true
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
                                onDragStart = { 
                                    isSeekHUDVisible = false
                                    seekOffsetHUD = 0 
                                },
                                onDragEnd = {
                                    if (isSeekHUDVisible) {
                                        mediaController?.let { it.seekTo(it.currentPosition + seekOffsetHUD) }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    isSeekHUDVisible = false
                                    isVolumeVisible = false
                                    isBrightnessVisible = false
                                },
                                onDrag = { change, dragAmount ->
                                    val width = size.width
                                    val height = size.height
                                    if (abs(dragAmount.x) > abs(dragAmount.y) && !isVolumeVisible && !isBrightnessVisible) {
                                        isSeekHUDVisible = true
                                        val oldOffset = seekOffsetHUD
                                        seekOffsetHUD += (dragAmount.x * 100).toLong()
                                        if (abs(seekOffsetHUD / 5000) != abs(oldOffset / 5000)) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    } else if (!isSeekHUDVisible) {
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
                                            volume = (volume - dragAmount.y / height).coerceIn(0f, 1f)
                                            if (abs(volume - oldVolume) > 0.05f) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            audioManager?.let { am ->
                                                val targetVol = kotlin.math.round(volume * maxVolume).toInt()
                                                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
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
                    Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = Color.Red)
                }
            }

            SeekHUD(isVisible = isSeekHUDVisible, offset = seekOffsetHUD, currentPosition = currentPosition)

            SeekAnimationOverlay(
                isVisible = showSeekBackwardAnimation,
                isForward = false,
                onAnimationFinished = { showSeekBackwardAnimation = false }
            )
            SeekAnimationOverlay(
                isVisible = showSeekForwardAnimation,
                isForward = true,
                onAnimationFinished = { showSeekForwardAnimation = false }
            )

            VerticalIndicator(
                value = volume,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                visible = isVolumeVisible && !isLocked,
                color = Color(0xFF81C784),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
            
            VerticalIndicator(
                value = brightness,
                icon = Icons.Default.BrightnessMedium,
                visible = isBrightnessVisible && !isLocked,
                color = Color(0xFFFFD54F),
                modifier = Modifier.align(Alignment.CenterStart)
            )

            PlayPausePulse(
                isPlay = showPlayPausePulse ?: true,
                trigger = pulseTrigger,
                visible = showPlayPausePulse != null
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
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
                                    onSpeedChange = onSpeedChange,
                                    onEqualizerClick = { showEqualizerDialog = true },
                                    onInfoClick = { showInfoDialog = true },
                                    onDeleteClick = { showDeleteDialog = true }
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
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                onValueChange = { mediaController?.seekTo((it * duration).toLong()) },
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Text(formatTime(duration), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

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
                                        contentDescription = "Repeat",
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
                                        contentDescription = "Shuffle",
                                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(onClick = {
                                    isHardwareAccelerated = !isHardwareAccelerated
                                    val mode = if (isHardwareAccelerated) "Hardware" else "Software"
                                    Toast.makeText(context, "$mode Decoding Active", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text(
                                        text = if (isHardwareAccelerated) "HW" else "SW",
                                        color = if (isHardwareAccelerated) Color(0xFF4CAF50) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(onClick = { mediaController?.seekToPreviousMediaItem() }) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp), tint = Color.White)
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
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                IconButton(onClick = { mediaController?.seekToNextMediaItem() }) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp), tint = Color.White)
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
                                    Icon(when(aspectRatio) { 1 -> Icons.Default.Fullscreen; 2 -> Icons.Default.AspectRatio; else -> Icons.Default.FitScreen }, contentDescription = "Aspect Ratio", tint = Color.White)
                                }
                                IconButton(onClick = { isLocked = true; isControlsVisible = false }) {
                                    Icon(Icons.Default.LockOpen, contentDescription = "Lock", tint = Color.White)
                                }
                                IconButton(onClick = {
                                    isLandscape = false
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }) {
                                    Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text("Sleep Timer") },
                text = {
                    var minutes by remember { mutableIntStateOf(30) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${minutes} minutes")
                        Slider(
                            value = minutes.toFloat(),
                            onValueChange = { minutes = it.toInt() },
                            valueRange = 0f..120f,
                            steps = 23
                        )
                        if (sleepTimerActive) {
                            TextButton(onClick = { 
                                viewModel.setSleepTimer(0)
                                showSleepTimerDialog = false
                            }) {
                                Text("Turn Off", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showSleepTimerDialog = false }) { Text("Cancel") }
                            Button(onClick = { 
                                viewModel.setSleepTimer(minutes)
                                showSleepTimerDialog = false
                            }) { Text("Set") }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }



        if (showInfoDialog) {
            val resolvedVideo = currentVideo ?: activeQueueVideos.find { it.uri.toString() == playingUri }
            val displayName = resolvedVideo?.name ?: videoName
            val displayPath = resolvedVideo?.path ?: Uri.parse(playingUri).path ?: playingUri
            val displaySize = if (resolvedVideo != null && resolvedVideo.size > 0L) {
                "${resolvedVideo.size / (1024 * 1024)} MB"
            } else {
                val file = java.io.File(displayPath)
                if (file.exists()) "${file.length() / (1024 * 1024)} MB" else "Unknown"
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(targetUri))
                                deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                            } else {
                                viewModel.stopIfPlaying(targetUri)
                                context.contentResolver.delete(targetUri, null, null)
                                viewModel.refreshVideos(context)
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
                onDismiss = { showEqualizerDialog = false }
            )
        }

        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = queueSheetState,
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
                    val queueItems = if (activeQueue.isNotEmpty()) {
                        activeQueue
                    } else {
                        activeQueueVideos.map {
                            com.arslandaim.omegaplayer.media.PlaybackQueueItem(
                                uri = it.uri.toString(),
                                title = it.name,
                                duration = it.duration,
                                isVideo = true
                            )
                        }
                    }
                    LazyColumn {
                        items(queueItems) { item ->
                            val currentUri = mediaController?.currentMediaItem?.localConfiguration?.uri?.toString() ?: videoUri
                            val isCurrent = item.uri == currentUri
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
                                                .data(Uri.parse(item.uri))
                                                .videoFrameMillis(1000)
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
                                modifier = Modifier.clickable {
                                    showQueueSheet = false
                                    val index = queueItems.indexOfFirst { it.uri == item.uri }
                                    if (index != -1 && index < (mediaController?.mediaItemCount ?: 0)) {
                                        mediaController?.seekToDefaultPosition(index)
                                    } else {
                                        mediaController?.setMediaItem(MediaItem.fromUri(Uri.parse(item.uri)))
                                        mediaController?.prepare()
                                    }
                                    mediaController?.play()
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
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
                    mediaController?.let { player ->
                        val target = (player.currentPosition - (delta * 1000).toLong()).coerceAtLeast(0L)
                        player.seekTo(target)
                    }
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

@Composable
fun SubtitleDialog(
    tracks: Tracks,
    subtitleTextSize: Int,
    onSubtitleTextSizeChange: (Int) -> Unit,
    subtitleTextColor: Int,
    onSubtitleTextColorChange: (Int) -> Unit,
    subtitleBgStyle: Int,
    onSubtitleBgStyleChange: (Int) -> Unit,
    subtitleDelay: Float,
    onSubtitleDelayChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onDisableSubtitles: () -> Unit,
    onSelectTrack: (Tracks.Group, Int) -> Unit,
    onLoadExternal: () -> Unit
) {
    val textTrackGroups = remember(tracks) {
        tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    }
    val hasTracks = textTrackGroups.isNotEmpty()
    val isAnySelected = textTrackGroups.any { it.isSelected }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                stringResource(R.string.subtitles_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDisableSubtitles()
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isAnySelected,
                        onClick = {
                            onDisableSubtitles()
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.subtitles_off),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (!isAnySelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (!hasTracks) {
                    Text(
                        stringResource(R.string.subtitles_none_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                } else {
                    textTrackGroups.forEach { group ->
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val isSelected = group.isTrackSelected(i)
                            val trackName = format.label
                                ?: format.language?.uppercase(Locale.getDefault())
                                ?: "Track ${i + 1}"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectTrack(group, i)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectTrack(group, i)
                                        onDismiss()
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    trackName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLoadExternal()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.subtitles_load_external))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Text(
                    stringResource(R.string.section_subtitles),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    stringResource(R.string.subtitle_size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sizeRow1 = listOf(
                        14 to stringResource(R.string.size_small),
                        18 to stringResource(R.string.size_normal)
                    )
                    val sizeRow2 = listOf(
                        22 to stringResource(R.string.size_large),
                        26 to stringResource(R.string.size_extra_large)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizeRow1.forEach { (size, label) ->
                            val isSelected = (subtitleTextSize == size)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSubtitleTextSizeChange(size) },
                                label = { Text(label, fontSize = 12.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizeRow2.forEach { (size, label) ->
                            val isSelected = (subtitleTextSize == size)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSubtitleTextSizeChange(size) },
                                label = { Text(label, fontSize = 12.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.subtitle_color),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colorList = listOf(
                        0 to stringResource(R.string.color_white),
                        1 to stringResource(R.string.color_yellow),
                        2 to stringResource(R.string.color_cyan)
                    )
                    colorList.forEach { (col, label) ->
                        val isSelected = (subtitleTextColor == col)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSubtitleTextColorChange(col) },
                            label = { Text(label, fontSize = 12.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Text(
                    stringResource(R.string.subtitle_background),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val bgRow1 = listOf(
                        0 to stringResource(R.string.bg_transparent),
                        2 to stringResource(R.string.bg_black)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bgRow1.forEach { (bg, label) ->
                            val isSelected = (subtitleBgStyle == bg)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSubtitleBgStyleChange(bg) },
                                label = { Text(label, fontSize = 12.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    val isSemiSelected = (subtitleBgStyle == 1)
                    FilterChip(
                        selected = isSemiSelected,
                        onClick = { onSubtitleBgStyleChange(1) },
                        label = { Text(stringResource(R.string.bg_semi_transparent), fontSize = 12.sp, maxLines = 1) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                Text(
                    stringResource(R.string.subtitle_delay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSubtitleDelayChange(-0.5f) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("-0.5s")
                    }
                    Text(
                        stringResource(R.string.delay_format, subtitleDelay),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = { onSubtitleDelayChange(0.5f) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+0.5s")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
fun PlaybackErrorOverlay(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.playback_error),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = durationMs / (1000 * 60 * 60)
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

@Composable
fun PlayerTopHUD(
    showClock: Boolean,
    currentTime: String,
    showBattery: Boolean,
    batteryPercentage: Int,
    showVolume: Boolean,
    volumePercentage: Int,
    showBrightness: Boolean,
    brightnessPercentage: Int,
    showMediaInfo: Boolean,
    resolution: String?,
    modifier: Modifier = Modifier
) {
    val itemsVisible = (showClock && currentTime.isNotEmpty()) ||
            showBattery ||
            showVolume ||
            showBrightness ||
            (showMediaInfo && !resolution.isNullOrBlank())

    if (!itemsVisible) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showClock && currentTime.isNotEmpty()) {
            HudBadge(
                icon = Icons.Default.AccessTime,
                text = currentTime
            )
        }
        if (showBattery) {
            HudBadge(
                icon = Icons.Default.BatteryChargingFull,
                text = "$batteryPercentage%"
            )
        }
        if (showVolume) {
            HudBadge(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                text = "$volumePercentage%"
            )
        }
        if (showBrightness) {
            HudBadge(
                icon = Icons.Default.BrightnessMedium,
                text = "$brightnessPercentage%"
            )
        }
        if (showMediaInfo && !resolution.isNullOrBlank()) {
            HudBadge(
                icon = Icons.Default.HighQuality,
                text = resolution
            )
        }
    }
}

@Composable
private fun HudBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlayerDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    playbackSpeed: Float,
    sleepTimerActive: Boolean,
    sleepTimerTimeLeft: Long,
    onPlaylistClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onEqualizerClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_add_to_playlist)) },
            onClick = {
                onDismiss()
                onPlaylistClick()
            },
            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
        )
        DropdownMenuItem(
            text = {
                val text = if (sleepTimerActive) stringResource(R.string.sleep_timer_format, formatTime(sleepTimerTimeLeft))
                else stringResource(R.string.sleep_timer)
                Text(text)
            },
            onClick = {
                onDismiss()
                onSleepTimerClick()
            },
            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = if (sleepTimerActive) MaterialTheme.colorScheme.primary else Color.White) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.playback_speed_format, playbackSpeed.toString())) },
            onClick = {
                val nextSpeed = when (playbackSpeed) {
                    1.0f -> 1.25f
                    1.25f -> 1.5f
                    1.5f -> 2.0f
                    2.0f -> 0.75f
                    else -> 1.0f
                }
                onSpeedChange(nextSpeed)
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.equalizer)) },
            onClick = {
                onDismiss()
                onEqualizerClick()
            },
            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_information)) },
            onClick = {
                onDismiss()
                onInfoClick()
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                onDeleteClick()
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}

@Composable
fun SeekHUD(isVisible: Boolean, offset: Long, currentPosition: Long) {
    AnimatedVisibility(
        visible = isVisible, 
        enter = fadeIn() + scaleIn(), 
        exit = fadeOut() + scaleOut()
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.Center) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f), 
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val targetTime = (currentPosition + offset).coerceAtLeast(0)
                    Text(
                        text = formatTime(targetTime),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = (if (offset >= 0) "+" else "") + "${offset / 1000}s",
                        color = if (offset >= 0) Color(0xFF81C784) else Color(0xFFE57373),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SeekAnimationOverlay(
    isVisible: Boolean,
    isForward: Boolean,
    onAnimationFinished: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        exit = fadeOut() + scaleOut(targetScale = 1.2f),
        modifier = Modifier.fillMaxSize()
    ) {
        LaunchedEffect(isVisible) {
            if (isVisible) {
                delay(600)
                onAnimationFinished()
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(bottom = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(100.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "seek_arrows")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Icon(
                        imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp).graphicsLayer(alpha = alpha)
                    )
                    Text(
                        text = if (isForward) "+10s" else "-10s",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalIndicator(
    value: Float, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    visible: Boolean, 
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(targetValue = value)
    AnimatedVisibility(
        visible = visible, 
        enter = fadeIn() + slideInHorizontally { if (modifier.toString().contains("CenterStart")) -20 else 20 }, 
        exit = fadeOut() + slideOutHorizontally { if (modifier.toString().contains("CenterStart")) -20 else 20 }, 
        modifier = modifier.fillMaxHeight().padding(vertical = 180.dp, horizontal = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            modifier = Modifier
                .width(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(vertical = 12.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)), 
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedValue)
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
fun PlayPausePulse(isPlay: Boolean, trigger: Int, visible: Boolean) {
    var isAnimVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            isAnimVisible = true
            delay(500)
            isAnimVisible = false
        }
    }

    AnimatedVisibility(
        visible = isAnimVisible,
        enter = scaleIn(initialScale = 0.5f) + fadeIn(),
        exit = scaleOut(targetScale = 1.5f) + fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlay) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun setBrightness(context: Context, brightness: Float) {
    val activity = context as? Activity ?: return
    val layoutParams = activity.window.attributes
    layoutParams.screenBrightness = brightness
    activity.window.attributes = layoutParams
}
