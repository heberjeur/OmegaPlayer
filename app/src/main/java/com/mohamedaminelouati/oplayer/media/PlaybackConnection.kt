package com.mohamedaminelouati.oplayer.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.mohamedaminelouati.oplayer.service.PlaybackService
import com.mohamedaminelouati.oplayer.util.MediaUtils
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.media3.common.util.UnstableApi
import com.mohamedaminelouati.oplayer.data.RecentPlayback
import com.mohamedaminelouati.oplayer.data.ThemePreferences
import com.mohamedaminelouati.oplayer.data.repository.PlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackRepository: PlaybackRepository,
    private val themePreferences: ThemePreferences
) {
    @androidx.annotation.OptIn(UnstableApi::class)
    private val _mediaController = MutableStateFlow<MediaController?>(null)
    @androidx.annotation.OptIn(UnstableApi::class)
    val mediaController = _mediaController.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem = _currentMediaItem.asStateFlow()

    private val _repeatMode = MutableStateFlow(PlayerRepeatMode.OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _livePlaybackProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val livePlaybackProgress = _livePlaybackProgress.asStateFlow()

    private val _skipSilenceEnabled = MutableStateFlow(false)
    val skipSilenceEnabled = _skipSilenceEnabled.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    init {
        scope.launch {
            themePreferences.skipSilenceEnabled.collect { enabled ->
                _skipSilenceEnabled.value = enabled
            }
        }
    }

    private var repetitionCounter = 0

    @androidx.annotation.OptIn(UnstableApi::class)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    private var isInitializing = false
    private val pendingActions = mutableListOf<() -> Unit>()

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun initializeController(onInitialized: (() -> Unit)? = null) {
        if (_mediaController.value != null) {
            onInitialized?.invoke()
            return
        }
        if (onInitialized != null) {
            pendingActions.add(onInitialized)
        }
        if (isInitializing) return
        isInitializing = true

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controllerFuture?.get() ?: return@addListener
            _mediaController.value = controller
            _isPlaying.value = controller.isPlaying
            _currentMediaItem.value = controller.currentMediaItem
            applyExoRepeatMode(controller, _repeatMode.value)
            if (controller.isPlaying) {
                startProgressUpdates()
            }
            
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                        saveCurrentProgress()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val previousItem = _currentMediaItem.value
                    val previousUri = previousItem?.localConfiguration?.uri?.toString()
                    val currentUri = mediaItem?.localConfiguration?.uri?.toString()
                    _currentMediaItem.value = mediaItem
                    handleMediaItemTransition(previousUri, currentUri, reason)

                    scope.launch {
                        val persistedSkipSilence = themePreferences.skipSilenceEnabled.first()
                        if (_skipSilenceEnabled.value != persistedSkipSilence) {
                            setSkipSilenceEnabled(persistedSkipSilence, persist = false)
                        }
                    }

                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && previousUri != null) {
                        val queueItem = _currentQueue.value.find { it.uri == previousUri }
                        val dur = queueItem?.duration ?: 0L
                        _livePlaybackProgress.value = _livePlaybackProgress.value + (previousUri to 1.0f)
                        saveCompletedItem(previousUri, dur, queueItem)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        val mode = _repeatMode.value
                        if (mode.targetRepeats > 0) {
                            if (repetitionCounter >= mode.targetRepeats) {
                                _repeatMode.value = PlayerRepeatMode.OFF
                                controller.repeatMode = Player.REPEAT_MODE_OFF
                                repetitionCounter = 0
                            } else {
                                repetitionCounter++
                                if (repetitionCounter >= mode.targetRepeats) {
                                    controller.repeatMode = Player.REPEAT_MODE_OFF
                                }
                                controller.seekTo(0L)
                                controller.play()
                                return
                            }
                        }
                        val currentUri = _currentMediaItem.value?.localConfiguration?.uri?.toString()
                        if (currentUri != null) {
                            val dur = controller.duration.coerceAtLeast(0L)
                            val queueItem = _currentQueue.value.find { it.uri == currentUri }
                            _livePlaybackProgress.value = _livePlaybackProgress.value + (currentUri to 1.0f)
                            saveCompletedItem(currentUri, dur, queueItem)
                        }
                    }
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    val count = controller.mediaItemCount
                    if (count == 0) {
                        if (_currentQueue.value.isNotEmpty()) {
                            _currentQueue.value = emptyList()
                        }
                        return
                    }
                    val currentList = _currentQueue.value
                    val uriMap = currentList.associateBy { it.uri }
                    val syncedQueue = (0 until count).mapNotNull { i ->
                        val item = controller.getMediaItemAt(i)
                        val uriStr = item.localConfiguration?.uri?.toString() ?: item.mediaId
                        uriMap[uriStr] ?: currentList.find { Uri.decode(it.uri) == Uri.decode(uriStr) }
                    }
                    if (syncedQueue.isNotEmpty() && syncedQueue != currentList) {
                        _currentQueue.value = syncedQueue
                    }
                }
            })
            
            isInitializing = false
            val actionsToRun = pendingActions.toList()
            pendingActions.clear()
            actionsToRun.forEach { it.invoke() }
        }, MoreExecutors.directExecutor())
    }

    private fun applyExoRepeatMode(controller: MediaController, mode: PlayerRepeatMode) {
        when (mode) {
            PlayerRepeatMode.OFF -> controller.repeatMode = Player.REPEAT_MODE_OFF
            PlayerRepeatMode.ONE,
            PlayerRepeatMode.TWO,
            PlayerRepeatMode.THREE -> controller.repeatMode = Player.REPEAT_MODE_ONE
            PlayerRepeatMode.ALL -> controller.repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    private fun handleMediaItemTransition(previousUri: String?, currentUri: String?, reason: Int) {
        val controller = _mediaController.value ?: return
        val mode = _repeatMode.value
        if (mode == PlayerRepeatMode.ONE || mode == PlayerRepeatMode.TWO || mode == PlayerRepeatMode.THREE) {
            val isRepeat = (previousUri != null && currentUri != null && previousUri == currentUri) ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            if (isRepeat) {
                repetitionCounter++
                if (repetitionCounter >= mode.targetRepeats) {
                    controller.repeatMode = Player.REPEAT_MODE_OFF
                }
            } else {
                if (repetitionCounter >= mode.targetRepeats) {
                    _repeatMode.value = PlayerRepeatMode.OFF
                    controller.repeatMode = Player.REPEAT_MODE_OFF
                    repetitionCounter = 0
                } else {
                    repetitionCounter = 0
                    controller.repeatMode = Player.REPEAT_MODE_ONE
                }
            }
        } else if (mode == PlayerRepeatMode.ALL) {
            controller.repeatMode = Player.REPEAT_MODE_ALL
        } else {
            controller.repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    fun cycleRepeatMode() {
        val nextMode = _repeatMode.value.next()
        setRepeatMode(nextMode)
    }

    fun setRepeatMode(mode: PlayerRepeatMode) {
        _repeatMode.value = mode
        repetitionCounter = 0
        val controller = _mediaController.value ?: return
        applyExoRepeatMode(controller, mode)
        if (mode.isActive && controller.playbackState == Player.STATE_ENDED) {
            controller.seekTo(0L)
            controller.play()
        }
    }

    fun play() {
        val controller = _mediaController.value ?: return
        if (controller.playbackState == Player.STATE_ENDED) {
            controller.seekTo(0L)
        }
        controller.play()
    }
    fun pause() = _mediaController.value?.pause()
    fun stop() = _mediaController.value?.stop()

    fun setSkipSilenceEnabled(enabled: Boolean, persist: Boolean = true) {
        _skipSilenceEnabled.value = enabled
        val controller = _mediaController.value
        if (controller != null) {
            val args = Bundle().apply { putBoolean("enabled", enabled) }
            controller.sendCustomCommand(
                SessionCommand(PlaybackService.ACTION_SET_SKIP_SILENCE, Bundle.EMPTY),
                args
            )
        }
        if (persist) {
            scope.launch {
                themePreferences.saveSkipSilenceEnabled(enabled)
            }
        }
    }

    private val _currentQueue = MutableStateFlow<List<PlaybackQueueItem>>(emptyList())
    val currentQueue = _currentQueue.asStateFlow()

    private val _playbackSource = MutableStateFlow<PlaybackSource>(PlaybackSource.None)
    val playbackSource = _playbackSource.asStateFlow()

    fun setQueue(items: List<PlaybackQueueItem>) {
        _currentQueue.value = items
    }

    fun clearQueue() {
        _currentQueue.value = emptyList()
    }

    fun clearLiveProgress() {
        _livePlaybackProgress.value = emptyMap()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var tickCounter = 0
            while (isActive) {
                val controller = _mediaController.value
                if (controller != null && controller.isPlaying) {
                    val uri = controller.currentMediaItem?.localConfiguration?.uri?.toString()
                    val duration = controller.duration
                    val position = controller.currentPosition
                    if (uri != null && duration > 0L) {
                        val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        _livePlaybackProgress.value = _livePlaybackProgress.value + (uri to progress)
                    }
                    tickCounter++
                    if (tickCounter >= 10) {
                        tickCounter = 0
                        saveCurrentProgress()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun saveCurrentProgress(targetUri: String? = null, overridePosition: Long? = null) {
        val controller = _mediaController.value ?: return
        val currentItem = controller.currentMediaItem ?: return
        val currentUri = targetUri ?: currentItem.localConfiguration?.uri?.toString() ?: return
        val duration = controller.duration
        val position = overridePosition ?: controller.currentPosition
        if (duration <= 0L) return

        scope.launch(Dispatchers.IO) {
            if (!themePreferences.savePositionOnQuit.first()) return@launch
            if (themePreferences.isHistoryPaused.first()) return@launch

            val queueItem = _currentQueue.value.find { it.uri == currentUri }
            val existing = playbackRepository.getRecentPlayback(currentUri)

            val recordToSave = when {
                existing != null -> existing.copy(
                    position = position,
                    duration = if (duration > 0L) duration else existing.duration,
                    lastPlayed = System.currentTimeMillis()
                )
                queueItem != null -> RecentPlayback(
                    uri = currentUri,
                    position = position,
                    duration = if (duration > 0L) duration else queueItem.duration,
                    lastPlayed = System.currentTimeMillis(),
                    mediaType = if (queueItem.isVideo) "video" else "audio",
                    name = queueItem.title,
                    artist = queueItem.artist,
                    size = 0L
                )
                else -> null
            }
            if (recordToSave != null) {
                playbackRepository.saveRecentPlayback(recordToSave)
            }
        }
    }

    private fun saveCompletedItem(uri: String, duration: Long, queueItem: PlaybackQueueItem?) {
        scope.launch(Dispatchers.IO) {
            if (!themePreferences.savePositionOnQuit.first()) return@launch
            if (themePreferences.isHistoryPaused.first()) return@launch

            val existing = playbackRepository.getRecentPlayback(uri)
            val finalDuration = if (duration > 0L) duration else (existing?.duration ?: 0L)
            val recordToSave = when {
                existing != null -> existing.copy(
                    position = finalDuration,
                    duration = finalDuration,
                    lastPlayed = System.currentTimeMillis()
                )
                queueItem != null -> RecentPlayback(
                    uri = uri,
                    position = finalDuration,
                    duration = finalDuration,
                    lastPlayed = System.currentTimeMillis(),
                    mediaType = if (queueItem.isVideo) "video" else "audio",
                    name = queueItem.title,
                    artist = queueItem.artist,
                    size = 0L
                )
                else -> null
            }
            if (recordToSave != null) {
                playbackRepository.saveRecentPlayback(recordToSave)
            }
        }
    }

    fun moveQueueItem(from: Int, to: Int, fallbackItems: List<PlaybackQueueItem>? = null) {
        val current = (_currentQueue.value.ifEmpty { fallbackItems ?: emptyList() }).toMutableList()
        if (from in current.indices && to in current.indices && from != to) {
            val item = current.removeAt(from)
            current.add(to, item)
            _currentQueue.value = current
        }
        _mediaController.value?.moveMediaItem(from, to)
    }

    fun removeQueueItem(index: Int, fallbackItems: List<PlaybackQueueItem>? = null) {
        val current = (_currentQueue.value.ifEmpty { fallbackItems ?: emptyList() }).toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _currentQueue.value = current
        }
        _mediaController.value?.removeMediaItem(index)
    }

    fun removeQueueItems(uris: Collection<String>, fallbackItems: List<PlaybackQueueItem>? = null) {
        if (uris.isEmpty()) return
        val uriSet = uris.map { it.trim() }.toSet()
        val controller = _mediaController.value
        if (controller != null) {
            val count = controller.mediaItemCount
            val indicesToRemove = mutableListOf<Int>()
            for (i in 0 until count) {
                val item = controller.getMediaItemAt(i)
                val uriStr = item.localConfiguration?.uri?.toString() ?: item.mediaId
                if (uriSet.any { u -> u == uriStr || Uri.decode(u) == Uri.decode(uriStr) }) {
                    indicesToRemove.add(i)
                }
            }
            indicesToRemove.sortedDescending().forEach { idx ->
                if (idx in 0 until controller.mediaItemCount) {
                    controller.removeMediaItem(idx)
                }
            }
        }
        val current = (_currentQueue.value.ifEmpty { fallbackItems ?: emptyList() })
        _currentQueue.value = current.filterNot { qItem ->
            uriSet.any { u -> u == qItem.uri || Uri.decode(u) == Uri.decode(qItem.uri) }
        }
    }

    fun playPlaylist(
        playlistItems: List<com.mohamedaminelouati.oplayer.data.PlaylistItem>,
        startIndex: Int,
        videos: List<com.mohamedaminelouati.oplayer.data.VideoModel>,
        audios: List<com.mohamedaminelouati.oplayer.data.AudioModel>,
        startPositionMs: Long = 0L,
        videosByUri: Map<String, com.mohamedaminelouati.oplayer.data.VideoModel>? = null,
        audiosByUri: Map<String, com.mohamedaminelouati.oplayer.data.AudioModel>? = null
    ) {
        if (_mediaController.value == null) {
            initializeController { playPlaylist(playlistItems, startIndex, videos, audios, startPositionMs, videosByUri, audiosByUri) }
            return
        }
        val controller = _mediaController.value ?: return
        if (playlistItems.isEmpty()) return

        val videoIndex = videosByUri ?: videos.associateBy { it.uri.toString() }
        val audioIndex = audiosByUri ?: audios.associateBy { it.uri.toString() }

        val queueItems = playlistItems.map { item ->
            if (item.mediaType == "video") {
                val video = videoIndex[item.mediaUri]
                PlaybackQueueItem(
                    uri = item.mediaUri,
                    title = video?.name ?: item.mediaUri.substringAfterLast("/"),
                    duration = video?.duration ?: 0L,
                    isVideo = true
                )
            } else {
                val audio = audioIndex[item.mediaUri]
                PlaybackQueueItem(
                    uri = item.mediaUri,
                    title = audio?.name ?: item.mediaUri.substringAfterLast("/"),
                    duration = audio?.duration ?: 0L,
                    isVideo = false,
                    artist = audio?.artist,
                    albumId = audio?.albumId
                )
            }
        }
        val playlistId = playlistItems.firstOrNull()?.playlistId ?: -1
        _playbackSource.value = PlaybackSource.Playlist(playlistId)
        setQueue(queueItems)

        val mediaItems = playlistItems.map { item ->
            if (item.mediaType == "video") {
                val video = videoIndex[item.mediaUri]
                val title = video?.name ?: item.mediaUri.substringAfterLast("/")
                MediaItem.Builder()
                    .setUri(item.mediaUri)
                    .setMediaId(item.mediaUri)
                    .setMimeType("video/*")
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist("")
                            .setAlbumTitle("")
                            .setAlbumArtist("")
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO)
                            .build()
                    )
                    .build()
            } else {
                val audio = audioIndex[item.mediaUri]
                val title = audio?.name ?: item.mediaUri.substringAfterLast("/")
                val albumArtUri = audio?.let { MediaUtils.albumArtUri(it.albumId) }
                MediaItem.Builder()
                    .setUri(item.mediaUri)
                    .setMediaId(item.mediaUri)
                    .setMimeType("audio/*")
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist("")
                            .setAlbumTitle("")
                            .setAlbumArtist("")
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }
        }

        if (mediaItems.isEmpty()) return

        val safeIndex = startIndex.coerceIn(0, mediaItems.size - 1)
        val clickedMediaId = mediaItems[safeIndex].mediaId
        if (controller.currentMediaItem?.mediaId == clickedMediaId) {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.playWhenReady = true
            controller.play()
            return
        }

        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, startPositionMs)
        repetitionCounter = 0
        applyExoRepeatMode(controller, _repeatMode.value)
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }

    fun playHistory(
        historyItems: List<com.mohamedaminelouati.oplayer.data.RecentPlayback>,
        startIndex: Int,
        videos: List<com.mohamedaminelouati.oplayer.data.VideoModel>,
        audios: List<com.mohamedaminelouati.oplayer.data.AudioModel>,
        audiosByUri: Map<String, com.mohamedaminelouati.oplayer.data.AudioModel>? = null
    ) {
        if (_mediaController.value == null) {
            initializeController { playHistory(historyItems, startIndex, videos, audios, audiosByUri) }
            return
        }
        val controller = _mediaController.value ?: return
        if (historyItems.isEmpty()) return

        val audioIndex = audiosByUri ?: audios.associateBy { it.uri.toString() }

        val queueItems = historyItems.map { item ->
            val isVideo = item.mediaType == "video"
            val audio = if (!isVideo) audioIndex[item.uri] else null
            PlaybackQueueItem(
                uri = item.uri,
                title = item.name,
                duration = item.duration,
                isVideo = isVideo,
                artist = audio?.artist,
                albumId = audio?.albumId
            )
        }
        _playbackSource.value = PlaybackSource.History
        setQueue(queueItems)

        val mediaItems = historyItems.map { item ->
            val isVideo = item.mediaType == "video"
            if (isVideo) {
                MediaItem.Builder()
                    .setUri(item.uri)
                    .setMediaId(item.uri)
                    .setMimeType("video/*")
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(item.name)
                            .setArtist("")
                            .setAlbumTitle("")
                            .setAlbumArtist("")
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO)
                            .build()
                    )
                    .build()
            } else {
                val audio = audioIndex[item.uri]
                val albumArtUri = audio?.let { MediaUtils.albumArtUri(it.albumId) }
                MediaItem.Builder()
                    .setUri(item.uri)
                    .setMediaId(item.uri)
                    .setMimeType("audio/*")
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(item.name)
                            .setArtist("")
                            .setAlbumTitle("")
                            .setAlbumArtist("")
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }
        }

        if (mediaItems.isEmpty()) return

        val safeIndex = startIndex.coerceIn(0, mediaItems.size - 1)
        val clickedMediaId = mediaItems[safeIndex].mediaId
        if (controller.currentMediaItem?.mediaId == clickedMediaId) {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.playWhenReady = true
            controller.play()
            return
        }

        val initialPos = historyItems[safeIndex].position.coerceAtLeast(0L)
        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, initialPos)
        repetitionCounter = 0
        applyExoRepeatMode(controller, _repeatMode.value)
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }

    fun playVideos(
        videos: List<com.mohamedaminelouati.oplayer.data.VideoModel>,
        startIndex: Int,
        startPositionMs: Long = 0L
    ) {
        if (_mediaController.value == null) {
            initializeController { playVideos(videos, startIndex, startPositionMs) }
            return
        }
        val controller = _mediaController.value ?: return
        if (videos.isEmpty()) return

        val queueItems = videos.map { video ->
            PlaybackQueueItem(
                uri = video.uri.toString(),
                title = video.name,
                duration = video.duration,
                isVideo = true
            )
        }
        _playbackSource.value = PlaybackSource.Folder
        setQueue(queueItems)

        val mediaItems = videos.map { video ->
            MediaItem.Builder()
                .setUri(video.uri)
                .setMediaId(video.uri.toString())
                .setMimeType("video/*")
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(video.name)
                        .setArtist("")
                        .setAlbumTitle("")
                        .setAlbumArtist("")
                        .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO)
                        .build()
                )
                .build()
        }
        val safeIndex = startIndex.coerceIn(0, mediaItems.size - 1)
        val clickedMediaId = mediaItems[safeIndex].mediaId
        if (controller.currentMediaItem?.mediaId == clickedMediaId) {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.playWhenReady = true
            controller.play()
            return
        }

        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, startPositionMs)
        repetitionCounter = 0
        applyExoRepeatMode(controller, _repeatMode.value)
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }

    fun playAudios(
        audios: List<com.mohamedaminelouati.oplayer.data.AudioModel>,
        startIndex: Int,
        startPositionMs: Long = 0L
    ) {
        if (_mediaController.value == null) {
            initializeController { playAudios(audios, startIndex, startPositionMs) }
            return
        }
        val controller = _mediaController.value ?: return
        if (audios.isEmpty()) return

        val queueItems = audios.map { audio ->
            PlaybackQueueItem(
                uri = audio.uri.toString(),
                title = audio.name,
                duration = audio.duration,
                isVideo = false,
                artist = audio.artist,
                albumId = audio.albumId
            )
        }
        _playbackSource.value = PlaybackSource.Folder
        setQueue(queueItems)

        val mediaItems = audios.map { audio ->
            val albumArtUri = MediaUtils.albumArtUri(audio.albumId)
            MediaItem.Builder()
                .setUri(audio.uri)
                .setMediaId(audio.uri.toString())
                .setMimeType("audio/*")
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(audio.name)
                        .setArtist("")
                        .setAlbumTitle("")
                        .setAlbumArtist("")
                        .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                        .build()
                )
                .build()
        }
        val safeIndex = startIndex.coerceIn(0, mediaItems.size - 1)
        val clickedMediaId = mediaItems[safeIndex].mediaId
        if (controller.currentMediaItem?.mediaId == clickedMediaId) {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.playWhenReady = true
            controller.play()
            return
        }

        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, startPositionMs)
        repetitionCounter = 0
        applyExoRepeatMode(controller, _repeatMode.value)
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }

    fun playMixedMedia(
        items: List<com.mohamedaminelouati.oplayer.data.SearchMediaItem>,
        startIndex: Int,
        startPositionMs: Long = 0L
    ) {
        if (_mediaController.value == null) {
            initializeController { playMixedMedia(items, startIndex, startPositionMs) }
            return
        }
        val controller = _mediaController.value ?: return
        if (items.isEmpty()) return

        val queueItems = items.map { item ->
            val audio = (item as? com.mohamedaminelouati.oplayer.data.SearchMediaItem.Audio)?.model
            PlaybackQueueItem(
                uri = item.uri.toString(),
                title = item.name,
                duration = item.duration,
                isVideo = item.isVideo,
                artist = audio?.artist,
                albumId = audio?.albumId
            )
        }
        _playbackSource.value = PlaybackSource.Folder
        setQueue(queueItems)

        val mediaItems = items.map { item ->
            if (item.isVideo) {
                MediaItem.Builder()
                    .setUri(item.uri)
                    .setMediaId(item.uri.toString())
                    .setMimeType("video/*")
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(item.name)
                            .setArtist("")
                            .setAlbumTitle("")
                            .setAlbumArtist("")
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO)
                            .build()
                    )
                    .build()
            } else {
                val audio = (item as? com.mohamedaminelouati.oplayer.data.SearchMediaItem.Audio)?.model
                MediaItem.Builder()
                    .setUri(item.uri)
                    .setMediaId(item.uri.toString())
                    .setMimeType("audio/*")
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(item.name)
                            .setArtist(audio?.artist ?: "")
                            .setAlbumTitle(audio?.album ?: "")
                            .setAlbumArtist("")
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }
        }
        val safeIndex = startIndex.coerceIn(0, mediaItems.size - 1)
        val clickedMediaId = mediaItems[safeIndex].mediaId
        if (controller.currentMediaItem?.mediaId == clickedMediaId) {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.playWhenReady = true
            controller.play()
            return
        }

        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, startPositionMs)
        repetitionCounter = 0
        applyExoRepeatMode(controller, _repeatMode.value)
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }
}

data class PlaybackQueueItem(
    val uri: String,
    val title: String,
    val duration: Long,
    val isVideo: Boolean,
    val artist: String? = null,
    val albumId: Long? = null
)

sealed class PlaybackSource {
    object None : PlaybackSource()
    object Folder : PlaybackSource()
    object History : PlaybackSource()
    data class Playlist(val playlistId: Int) : PlaybackSource()
}
