/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.viewmodel

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.arslandaim.omegaplayer.data.AudioModel
import com.arslandaim.omegaplayer.data.PlaybackSpeedScope
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.data.Playlist
import com.arslandaim.omegaplayer.data.PlaylistItem
import com.arslandaim.omegaplayer.data.RecentPlayback
import com.arslandaim.omegaplayer.data.repository.PlaybackRepository
import com.arslandaim.omegaplayer.data.ThemePreferences
import com.arslandaim.omegaplayer.domain.usecase.media.GetAudiosUseCase
import com.arslandaim.omegaplayer.domain.usecase.media.GetVideosUseCase
import com.arslandaim.omegaplayer.domain.usecase.playback.GetRecentPlaybackUseCase
import com.arslandaim.omegaplayer.domain.usecase.playback.PlaylistUseCases
import com.arslandaim.omegaplayer.media.PlaybackConnection
import com.arslandaim.omegaplayer.media.PlaybackQueueItem
import com.arslandaim.omegaplayer.util.Resource
import android.content.Context
import com.arslandaim.omegaplayer.data.MediaSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    application: Application,
    private val getAudiosUseCase: GetAudiosUseCase,
    private val playlistUseCases: PlaylistUseCases,
    private val getRecentPlaybackUseCase: GetRecentPlaybackUseCase,
    private val playbackConnection: PlaybackConnection,
    private val playbackRepository: PlaybackRepository,
    private val themePreferences: ThemePreferences
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    val activeAudioUri: StateFlow<String?> = playbackConnection.currentMediaItem
        .map { it?.localConfiguration?.uri?.toString() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying
    val mediaController: StateFlow<MediaController?> = playbackConnection.mediaController
    val activeQueue: StateFlow<List<PlaybackQueueItem>> = playbackConnection.currentQueue

    private val _audioError = MutableStateFlow<String?>(null)
    val audioError: StateFlow<String?> = _audioError.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    val excludedFolders: StateFlow<Set<String>> = themePreferences.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawAudios: Flow<List<AudioModel>> = refreshTrigger
        .flatMapLatest { getAudiosUseCase() }
        .onEach { resource ->
            when (resource) {
                is Resource.Loading -> _isLoading.value = true
                is Resource.Success -> {
                    _isLoading.value = false
                    _audioError.value = null
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    _audioError.value = resource.message
                }
            }
        }
        .map { resource -> if (resource is Resource.Success) resource.data else emptyList() }

    val audios: StateFlow<List<AudioModel>> = combine(rawAudios, excludedFolders) { audioList, excluded ->
        if (excluded.isEmpty()) audioList
        else audioList.filter {
            val folder = File(it.path).parentFile?.name ?: "Internal"
            !excluded.contains(folder)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(MediaSortOrder.DATE_DESC)
    val sortOrder: StateFlow<MediaSortOrder> = _sortOrder.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: MediaSortOrder) {
        _sortOrder.value = order
    }

    val filteredAudios: StateFlow<List<AudioModel>> = combine(audios, searchQuery, sortOrder) { audioList, query, sort ->
        var result = if (query.isBlank()) {
            audioList
        } else {
            audioList.filter { it.name.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
        }
        when (sort) {
            MediaSortOrder.DATE_DESC -> result
            MediaSortOrder.DATE_ASC -> result.reversed()
            MediaSortOrder.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            MediaSortOrder.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
            MediaSortOrder.SIZE_DESC -> result.sortedByDescending { it.size }
            MediaSortOrder.DURATION_DESC -> result.sortedByDescending { it.duration }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistUseCases.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPlayback: StateFlow<List<RecentPlayback>> = getRecentPlaybackUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fullHistory: StateFlow<List<RecentPlayback>> = playbackRepository.getAllRecentPlayback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isHistoryPaused: StateFlow<Boolean> = themePreferences.isHistoryPaused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _sleepTimerActive = MutableStateFlow(false)
    val sleepTimerActive: StateFlow<Boolean> = _sleepTimerActive.asStateFlow()

    private val _sleepTimerTimeLeft = MutableStateFlow(0L)
    val sleepTimerTimeLeft: StateFlow<Long> = _sleepTimerTimeLeft.asStateFlow()

    private val _stopAfterCurrent = MutableStateFlow(false)
    val stopAfterCurrent: StateFlow<Boolean> = _stopAfterCurrent.asStateFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _stopAfterCurrent.value = false
        if (minutes <= 0) {
            _sleepTimerActive.value = false
            _sleepTimerTimeLeft.value = 0
            return
        }

        _sleepTimerActive.value = true
        _sleepTimerTimeLeft.value = minutes * 60 * 1000L
        
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerTimeLeft.value > 0) {
                delay(1000)
                _sleepTimerTimeLeft.value -= 1000
            }
            playbackConnection.pause()
            _sleepTimerActive.value = false
        }
    }

    fun setStopAfterCurrent(enabled: Boolean) {
        _stopAfterCurrent.value = enabled
        if (enabled) {
            _sleepTimerActive.value = true
            _sleepTimerTimeLeft.value = 0
            sleepTimerJob?.cancel()
            
            // Wire up listener to stop when media item changes
            val controller = playbackConnection.mediaController.value ?: return
            controller.addListener(object : androidx.media3.common.Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (reason == androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && _stopAfterCurrent.value) {
                        controller.pause()
                        _stopAfterCurrent.value = false
                        _sleepTimerActive.value = false
                        controller.removeListener(this)
                    }
                }
            })
        } else {
            _sleepTimerActive.value = false
        }
    }

    val folders: StateFlow<Map<String, Int>> = audios
        .map { audioList ->
            audioList.groupBy { File(it.path).parentFile?.name ?: "Internal" }
                .mapValues { it.value.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val audiosInSelectedFolder: StateFlow<List<AudioModel>> = combine(audios, _selectedFolder) { audioList, folder ->
        if (folder == null) emptyList()
        else audioList.filter { (File(it.path).parentFile?.name ?: "Internal") == folder }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistUseCases.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistUseCases.deletePlaylist(playlist)
        }
    }

    fun addToPlaylist(playlistId: Int, uri: String, type: String) {
        viewModelScope.launch {
            playlistUseCases.addToPlaylist(playlistId, uri, type)
        }
    }

    fun removeFromPlaylist(playlistId: Int, uri: String) {
        viewModelScope.launch {
            playlistUseCases.removeFromPlaylist(playlistId, uri)
        }
    }

    fun getPlaylistItems(playlistId: Int): Flow<List<PlaylistItem>> {
        return playlistUseCases.getPlaylistItems(playlistId)
    }

    fun togglePlayPause(audio: AudioModel) {
        val controller = playbackConnection.mediaController.value ?: return
        val currentUri = controller.currentMediaItem?.localConfiguration?.uri?.toString()
        
        if (currentUri == audio.uri.toString()) {
            if (controller.isPlaying) controller.pause() else controller.play()
        } else {
            val folderAudios = audiosInSelectedFolder.value
            val mediaItems = folderAudios.map { audioItem ->
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    audioItem.albumId
                )
                MediaItem.Builder()
                    .setUri(audioItem.uri)
                    .setMediaId(audioItem.id.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(audioItem.name)
                            .setArtist(audioItem.artist)
                            .setAlbumTitle(audioItem.album)
                            .setArtworkUri(albumArtUri)
                            .build()
                    )
                    .build()
            }
            val index = folderAudios.indexOfFirst { it.id == audio.id }.coerceAtLeast(0)
            
            if (mediaItems.isNotEmpty()) {
                setFolderQueue(folderAudios)
                controller.setMediaItems(mediaItems, index, 0L)
                controller.prepare()
                controller.play()
            }
        }
    }

    fun setSelectedFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun playPlaylist(
        playlistItems: List<PlaylistItem>,
        startIndex: Int,
        videos: List<VideoModel>,
        audios: List<AudioModel>
    ) {
        val controller = playbackConnection.mediaController.value ?: return
        if (playlistItems.isEmpty()) return

        val mediaItems = playlistItems.map { item ->
            if (item.mediaType == "video") {
                val video = videos.find { it.uri.toString() == item.mediaUri }
                val title = video?.name ?: item.mediaUri.substringAfterLast("/")
                MediaItem.Builder()
                    .setUri(item.mediaUri)
                    .setMediaId(item.mediaUri)
                    .setMimeType("video/*")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
                            .build()
                    )
                    .build()
            } else {
                val audio = audios.find { it.uri.toString() == item.mediaUri }
                val title = audio?.name ?: item.mediaUri.substringAfterLast("/")
                val albumArtUri = audio?.let {
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it.albumId)
                }
                MediaItem.Builder()
                    .setUri(item.mediaUri)
                    .setMediaId(item.mediaUri)
                    .setMimeType("audio/*")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(audio?.artist ?: "")
                            .setAlbumTitle(audio?.album ?: "")
                            .setArtworkUri(albumArtUri)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }
        }

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
        playbackConnection.setQueue(queueItems)

        val safeIndex = startIndex.coerceIn(0, mediaItems.size - 1)
        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItems(mediaItems, safeIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playHistory(
        historyItems: List<RecentPlayback>,
        startIndex: Int,
        videos: List<VideoModel>,
        audios: List<AudioModel>
    ) {
        val controller = playbackConnection.mediaController.value ?: return
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
        playbackConnection.setQueue(queueItems)

        val mediaItems = historyItems.map { item ->
            val isVideo = item.mediaType == "video"
            if (isVideo) {
                MediaItem.Builder()
                    .setUri(item.uri)
                    .setMediaId(item.uri)
                    .setMimeType("video/*")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(item.name)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
                            .build()
                    )
                    .build()
            } else {
                val audio = audios.find { it.uri.toString() == item.uri }
                val albumArtUri = audio?.let {
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it.albumId)
                }
                MediaItem.Builder()
                    .setUri(item.uri)
                    .setMediaId(item.uri)
                    .setMimeType("audio/*")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(item.name)
                            .setArtist(audio?.artist ?: "")
                            .setAlbumTitle(audio?.album ?: "")
                            .setArtworkUri(albumArtUri)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
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

    fun setFolderQueue(folderAudios: List<AudioModel>) {
        val queueItems = folderAudios.map { audio ->
            PlaybackQueueItem(
                uri = audio.uri.toString(),
                title = audio.name,
                duration = audio.duration,
                isVideo = false,
                artist = audio.artist,
                albumId = audio.albumId
            )
        }
        playbackConnection.setQueue(queueItems)
    }

    fun excludeFolder(folderName: String) {
        viewModelScope.launch {
            themePreferences.addExcludedFolder(folderName)
            val currentUri = playbackConnection.mediaController.value?.currentMediaItem?.localConfiguration?.uri
            if (currentUri != null) {
                val current = audios.value.find { it.uri == currentUri }
                val currentFolder = current?.let { File(it.path).parentFile?.name ?: "Internal" }
                if (currentFolder == folderName) {
                    stopIfPlaying(currentUri)
                }
            }
        }
    }

    fun restoreFolder(folderName: String) {
        viewModelScope.launch {
            themePreferences.removeExcludedFolder(folderName)
        }
    }

    fun clearExcludedFolders() {
        viewModelScope.launch {
            themePreferences.clearExcludedFolders()
        }
    }

    fun stopIfPlaying(uri: Uri) {
        val controller = playbackConnection.mediaController.value ?: return
        val currentUri = controller.currentMediaItem?.localConfiguration?.uri
        if (currentUri == uri) {
            controller.stop()
            controller.clearMediaItems()
        }
    }

    fun stopIfPlaying(uris: List<Uri>) {
        val controller = playbackConnection.mediaController.value ?: return
        val currentUri = controller.currentMediaItem?.localConfiguration?.uri
        if (currentUri != null && uris.contains(currentUri)) {
            controller.stop()
            controller.clearMediaItems()
        }
    }

    fun getAudiosInFolder(folderName: String): List<AudioModel> {
        return audios.value.filter { (File(it.path).parentFile?.name ?: "Internal") == folderName }
    }

    fun fetchAudios(context: Context) {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun refreshAudios(context: Context) {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun savePlaybackProgress() {
        val controller = playbackConnection.mediaController.value ?: return
        val mediaItem = controller.currentMediaItem ?: return
        val currentUri = mediaItem.localConfiguration?.uri?.toString() ?: return
        val audio = audios.value.find { it.uri.toString() == currentUri } ?: return
        
        val position = controller.currentPosition
        val duration = controller.duration
        val name = audio.name
        val artist = audio.artist

        viewModelScope.launch(Dispatchers.IO) {
            if (themePreferences.isHistoryPaused.first()) return@launch
            
            playbackRepository.saveRecentPlayback(
                RecentPlayback(
                    uri = currentUri,
                    position = position,
                    duration = duration,
                    mediaType = "audio",
                    name = name,
                    artist = artist,
                    size = audio.size
                )
            )
        }
    }

    val speedScope: StateFlow<PlaybackSpeedScope> = themePreferences.speedScope
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackSpeedScope.GLOBAL)

    val globalPlaybackSpeed: StateFlow<Float> = themePreferences.globalPlaybackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val showPlayerClock: StateFlow<Boolean> = themePreferences.showPlayerClock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showPlayerBattery: StateFlow<Boolean> = themePreferences.showPlayerBattery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showPlayerMediaInfo: StateFlow<Boolean> = themePreferences.showPlayerMediaInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showPlayerVolume: StateFlow<Boolean> = themePreferences.showPlayerVolume
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showPlayerBrightness: StateFlow<Boolean> = themePreferences.showPlayerBrightness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    suspend fun getSavedPosition(uri: String): Long {
        return playbackRepository.getRecentPlayback(uri)?.position ?: 0L
    }

    fun setSpeedScope(scope: PlaybackSpeedScope) {
        viewModelScope.launch { themePreferences.saveSpeedScope(scope) }
    }

    fun setGlobalPlaybackSpeed(speed: Float) {
        viewModelScope.launch { themePreferences.saveGlobalPlaybackSpeed(speed) }
    }

    fun getFolderSpeed(folderName: String): Flow<Float?> = themePreferences.getFolderSpeed(folderName)

    fun setFolderPlaybackSpeed(folderName: String, speed: Float) {
        viewModelScope.launch { themePreferences.saveFolderPlaybackSpeed(folderName, speed) }
    }

    fun getFolderViewMode(folderKey: String, defaultMode: Int? = null): Flow<Int> =
        themePreferences.getFolderViewMode(folderKey, defaultMode)

    fun setFolderViewMode(folderKey: String, mode: Int) {
        viewModelScope.launch { themePreferences.saveFolderViewMode(folderKey, mode) }
    }

    fun getFolderGridView(folderKey: String, defaultGrid: Boolean = false): Flow<Boolean> =
        themePreferences.getFolderGridView(folderKey, defaultGrid)

    fun setFolderGridView(folderKey: String, isGrid: Boolean) {
        viewModelScope.launch { themePreferences.saveFolderGridView(folderKey, isGrid) }
    }

    fun getFolderSortOrder(folderKey: String, defaultSort: String? = null): Flow<String> =
        themePreferences.getFolderSortOrder(folderKey, defaultSort)

    fun setFolderSortOrder(folderKey: String, sortOrder: String) {
        viewModelScope.launch { themePreferences.saveFolderSortOrder(folderKey, sortOrder) }
    }

    fun togglePlayerClock(show: Boolean) {
        viewModelScope.launch { themePreferences.saveShowPlayerClock(show) }
    }

    fun togglePlayerBattery(show: Boolean) {
        viewModelScope.launch { themePreferences.saveShowPlayerBattery(show) }
    }

    fun togglePlayerMediaInfo(show: Boolean) {
        viewModelScope.launch { themePreferences.saveShowPlayerMediaInfo(show) }
    }

    fun togglePlayerVolume(show: Boolean) {
        viewModelScope.launch { themePreferences.saveShowPlayerVolume(show) }
    }

    fun togglePlayerBrightness(show: Boolean) {
        viewModelScope.launch { themePreferences.saveShowPlayerBrightness(show) }
    }
}
