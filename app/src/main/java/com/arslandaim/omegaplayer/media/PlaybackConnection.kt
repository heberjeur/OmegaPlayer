package com.arslandaim.omegaplayer.media

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.arslandaim.omegaplayer.service.PlaybackService
import com.arslandaim.omegaplayer.util.StartupTrace
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @androidx.annotation.OptIn(UnstableApi::class)
    private val _mediaController = MutableStateFlow<MediaController?>(null)
    @androidx.annotation.OptIn(UnstableApi::class)
    val mediaController = _mediaController.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem = _currentMediaItem.asStateFlow()

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
        StartupTrace.mark("playback controller connect requested")
        controllerFuture?.addListener({
            val controller = controllerFuture?.get() ?: return@addListener
            _mediaController.value = controller
            _isPlaying.value = controller.isPlaying
            _currentMediaItem.value = controller.currentMediaItem
            StartupTrace.mark("playback controller connected")
            
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentMediaItem.value = mediaItem
                }
            })
            
            isInitializing = false
            val actionsToRun = pendingActions.toList()
            pendingActions.clear()
            actionsToRun.forEach { it.invoke() }
        }, MoreExecutors.directExecutor())
    }

    fun play() = _mediaController.value?.play()
    fun pause() = _mediaController.value?.pause()
    fun stop() = _mediaController.value?.stop()

    private val _currentQueue = MutableStateFlow<List<PlaybackQueueItem>>(emptyList())
    val currentQueue = _currentQueue.asStateFlow()

    fun setQueue(items: List<PlaybackQueueItem>) {
        _currentQueue.value = items
    }

    fun clearQueue() {
        _currentQueue.value = emptyList()
    }

    fun playPlaylist(
        playlistItems: List<com.arslandaim.omegaplayer.data.PlaylistItem>,
        startIndex: Int,
        videos: List<com.arslandaim.omegaplayer.data.VideoModel>,
        audios: List<com.arslandaim.omegaplayer.data.AudioModel>,
        startPositionMs: Long = 0L,
        videosByUri: Map<String, com.arslandaim.omegaplayer.data.VideoModel>? = null,
        audiosByUri: Map<String, com.arslandaim.omegaplayer.data.AudioModel>? = null
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
                val albumArtUri = audio?.let {
                    android.content.ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), it.albumId)
                }
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
            controller.playWhenReady = true
            controller.play()
            return
        }

        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, startPositionMs)
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }

    fun playHistory(
        historyItems: List<com.arslandaim.omegaplayer.data.RecentPlayback>,
        startIndex: Int,
        videos: List<com.arslandaim.omegaplayer.data.VideoModel>,
        audios: List<com.arslandaim.omegaplayer.data.AudioModel>,
        audiosByUri: Map<String, com.arslandaim.omegaplayer.data.AudioModel>? = null
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
                val albumArtUri = audio?.let {
                    android.content.ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), it.albumId)
                }
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
            controller.playWhenReady = true
            controller.play()
            return
        }

        val initialPos = historyItems[safeIndex].position.coerceAtLeast(0L)
        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, initialPos)
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }

    fun playVideos(
        videos: List<com.arslandaim.omegaplayer.data.VideoModel>,
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
            controller.playWhenReady = true
            controller.play()
            return
        }

        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, startPositionMs)
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }

    fun playAudios(
        audios: List<com.arslandaim.omegaplayer.data.AudioModel>,
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
        setQueue(queueItems)

        val mediaItems = audios.map { audio ->
            val albumArtUri = android.content.ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), audio.albumId)
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
            controller.playWhenReady = true
            controller.play()
            return
        }

        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, startPositionMs)
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
