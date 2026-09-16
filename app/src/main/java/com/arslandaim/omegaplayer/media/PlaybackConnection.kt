package com.arslandaim.omegaplayer.media

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.arslandaim.omegaplayer.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        initializeController()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get() ?: return@addListener
                _mediaController.value = controller
                _isPlaying.value = controller.isPlaying
                _currentMediaItem.value = controller.currentMediaItem
                
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentMediaItem.value = mediaItem
                    }
                })
            } catch (e: Exception) {
                Log.e("PlaybackConnection", "Failed to connect to MediaController", e)
            }
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
        audios: List<com.arslandaim.omegaplayer.data.AudioModel>
    ) {
        val controller = _mediaController.value ?: return
        if (playlistItems.isEmpty()) return

        val queueItems = playlistItems.map { item ->
            if (item.mediaType == "video") {
                val video = videos.find { it.uri.toString() == item.mediaUri }
                PlaybackQueueItem(
                    uri = item.mediaUri,
                    title = video?.name ?: item.mediaUri.substringAfterLast("/"),
                    duration = video?.duration ?: 0L,
                    isVideo = true
                )
            } else {
                val audio = audios.find { it.uri.toString() == item.mediaUri }
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
                val video = videos.find { it.uri.toString() == item.mediaUri }
                val title = video?.name ?: item.mediaUri.substringAfterLast("/")
                MediaItem.Builder()
                    .setUri(item.mediaUri)
                    .setMediaId(item.mediaUri)
                    .setMimeType("video/*")
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(title)
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO)
                            .build()
                    )
                    .build()
            } else {
                val audio = audios.find { it.uri.toString() == item.mediaUri }
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
                            .setArtist(audio?.artist ?: "")
                            .setAlbumTitle(audio?.album ?: "")
                            .setArtworkUri(albumArtUri)
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }
        }

        val safeIndex = startIndex.coerceIn(0, mediaItems.size - 1)
        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playHistory(
        historyItems: List<com.arslandaim.omegaplayer.data.RecentPlayback>,
        startIndex: Int,
        videos: List<com.arslandaim.omegaplayer.data.VideoModel>,
        audios: List<com.arslandaim.omegaplayer.data.AudioModel>
    ) {
        val controller = _mediaController.value ?: return
        if (historyItems.isEmpty()) return

        val queueItems = historyItems.map { item ->
            val isVideo = item.mediaType == "video"
            val audio = if (!isVideo) audios.find { it.uri.toString() == item.uri } else null
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
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO)
                            .build()
                    )
                    .build()
            } else {
                val audio = audios.find { it.uri.toString() == item.uri }
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
                            .setArtist(audio?.artist ?: "")
                            .setAlbumTitle(audio?.album ?: "")
                            .setArtworkUri(albumArtUri)
                            .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }
        }

        val safeIndex = startIndex.coerceIn(0, mediaItems.size - 1)
        val initialPos = historyItems[safeIndex].position.coerceAtLeast(0L)
        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, initialPos)
        controller.prepare()
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
