/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.SeekParameters
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import com.arslandaim.omegaplayer.data.MediaSortOrder
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.data.PlaybackSpeedScope
import com.arslandaim.omegaplayer.data.RecentPlayback
import com.arslandaim.omegaplayer.data.Playlist
import com.arslandaim.omegaplayer.domain.usecase.media.GetVideosUseCase
import com.arslandaim.omegaplayer.domain.usecase.playback.PlaylistUseCases
import com.arslandaim.omegaplayer.domain.usecase.playback.GetRecentPlaybackUseCase
import com.arslandaim.omegaplayer.data.repository.PlaybackRepository
import com.arslandaim.omegaplayer.data.ThemePreferences
import com.arslandaim.omegaplayer.media.PlaybackConnection
import com.arslandaim.omegaplayer.service.PlaybackService
import com.arslandaim.omegaplayer.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    application: Application,
    private val getVideosUseCase: GetVideosUseCase,
    private val playlistUseCases: PlaylistUseCases,
    private val getRecentPlaybackUseCase: GetRecentPlaybackUseCase,
    private val playbackRepository: PlaybackRepository,
    private val playbackConnection: PlaybackConnection,
    private val themePreferences: ThemePreferences
) : AndroidViewModel(application) {

    val playlists: StateFlow<List<Playlist>> = playlistUseCases.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToPlaylist(playlistId: Int, videoUri: String) {
        viewModelScope.launch {
            playlistUseCases.addToPlaylist(playlistId, videoUri, "video")
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistUseCases.createPlaylist(name)
        }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val activeVideoUri: StateFlow<String?> = playbackConnection.currentMediaItem
        .map { it?.localConfiguration?.uri?.toString() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isBackgroundPlayEnabled = MutableStateFlow(true)
    val isBackgroundPlayEnabled: StateFlow<Boolean> = _isBackgroundPlayEnabled.asStateFlow()

    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying
    val mediaController: StateFlow<androidx.media3.session.MediaController?> = playbackConnection.mediaController

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _videoError = MutableStateFlow<String?>(null)
    val videoError: StateFlow<String?> = _videoError.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    val excludedFolders: StateFlow<Set<String>> = themePreferences.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val showRecentHistoryOnHome: StateFlow<Boolean> = themePreferences.showRecentHistoryOnHome
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showHistoryTab: StateFlow<Boolean> = themePreferences.showHistoryTab
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawVideos: Flow<List<VideoModel>> = refreshTrigger
        .flatMapLatest { getVideosUseCase() }
        .onEach { resource ->
            when (resource) {
                is Resource.Loading -> _isLoading.value = true
                is Resource.Success -> {
                    _isLoading.value = false
                    _videoError.value = null
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    _videoError.value = resource.message
                }
            }
        }
        .map { resource -> if (resource is Resource.Success) resource.data else emptyList() }

    val videos: StateFlow<List<VideoModel>> = combine(rawVideos, excludedFolders) { videoList, excluded ->
        if (excluded.isEmpty()) videoList
        else videoList.filter {
            val folder = File(it.path).parentFile?.name ?: "Internal"
            !excluded.contains(folder)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPlayback: StateFlow<List<RecentPlayback>> = getRecentPlaybackUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fullHistory: StateFlow<List<RecentPlayback>> = playbackRepository.getAllRecentPlayback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isHistoryPaused: StateFlow<Boolean> = themePreferences.isHistoryPaused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleHistoryPause(paused: Boolean) {
        viewModelScope.launch {
            themePreferences.saveHistoryPaused(paused)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            playbackRepository.clearAllRecentPlayback()
        }
    }

    private val _sleepTimerActive = MutableStateFlow(false)
    val sleepTimerActive: StateFlow<Boolean> = _sleepTimerActive.asStateFlow()

    private val _sleepTimerTimeLeft = MutableStateFlow(0L)
    val sleepTimerTimeLeft: StateFlow<Long> = _sleepTimerTimeLeft.asStateFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
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
            playbackConnection.mediaController.value?.pause()
            _sleepTimerActive.value = false
        }
    }

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

    val filteredVideos: StateFlow<List<VideoModel>> = combine(videos, searchQuery, sortOrder) { videoList, query, sort ->
        var result = if (query.isBlank()) {
            videoList
        } else {
            videoList.filter { it.name.contains(query, ignoreCase = true) }
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

    val folders: StateFlow<Map<String, Int>> = videos
        .map { videoList ->
            videoList.groupBy { File(it.path).parentFile?.name ?: "Internal" }
                .mapValues { it.value.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val videosInSelectedFolder: StateFlow<List<VideoModel>> = combine(videos, _selectedFolder) { videoList, folder ->
        if (folder == null) emptyList()
        else videoList.filter { (File(it.path).parentFile?.name ?: "Internal") == folder }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @androidx.annotation.OptIn(UnstableApi::class)
    fun toggleBackgroundPlay(context: Context, enabled: Boolean) {
        _isBackgroundPlayEnabled.value = enabled
        val intent = Intent(context.applicationContext, PlaybackService::class.java)
        if (enabled) {
            ContextCompat.startForegroundService(context.applicationContext, intent)
        } else {
            context.applicationContext.stopService(intent)
        }
    }

    fun toggleShowRecentHistoryOnHome(show: Boolean) {
        viewModelScope.launch { themePreferences.saveShowRecentHistoryOnHome(show) }
    }

    fun toggleShowHistoryTab(show: Boolean) {
        viewModelScope.launch { themePreferences.saveShowHistoryTab(show) }
    }

    fun setSelectedFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun getCurrentVideo(targetUri: String? = null): VideoModel? {
        val uri = targetUri ?: activeVideoUri.value ?: return null
        return videos.value.find { it.uri.toString() == uri }
    }

    fun excludeFolder(folderName: String) {
        viewModelScope.launch {
            themePreferences.addExcludedFolder(folderName)
            val currentUri = playbackConnection.mediaController.value?.currentMediaItem?.localConfiguration?.uri
            if (currentUri != null) {
                val current = videos.value.find { it.uri == currentUri }
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

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCleared() {
        super.onCleared()
    }

    fun stopIfPlaying(uri: Uri) {
        playbackConnection.mediaController.value?.let { player ->
            val currentUri = player.currentMediaItem?.localConfiguration?.uri
            if (currentUri == uri) {
                player.stop()
                player.clearMediaItems()
            }
        }
    }

    fun stopIfPlaying(uris: List<Uri>) {
        playbackConnection.mediaController.value?.let { player ->
            val currentUri = player.currentMediaItem?.localConfiguration?.uri
            if (currentUri != null && uris.contains(currentUri)) {
                player.stop()
                player.clearMediaItems()
            }
        }
    }



    fun getVideosInFolder(folderName: String): List<VideoModel> {
        return videos.value.filter { (File(it.path).parentFile?.name ?: "Internal") == folderName }
    }

    fun fetchVideos(context: Context) {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun refreshVideos(context: Context) {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearVideoCache(context: Context, videoId: Long) {
        val imageLoader = context.imageLoader
        val key = "thumb_$videoId"
        imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(key))
        imageLoader.diskCache?.remove(key)
    }

    fun savePlaybackProgress() {
        val player = playbackConnection.mediaController.value ?: return
        val mediaItem = player.currentMediaItem ?: return
        val currentUri = mediaItem.localConfiguration?.uri?.toString() ?: return
        val video = videos.value.find { it.uri.toString() == currentUri } ?: return
        
        val position = player.currentPosition
        val duration = player.duration
        val name = video.name

        viewModelScope.launch {
            if (themePreferences.isHistoryPaused.first()) return@launch

            playbackRepository.saveRecentPlayback(
                RecentPlayback(
                    uri = currentUri,
                    position = position,
                    duration = duration,
                    mediaType = "video",
                    name = name
                )
            )
        }
    }

    suspend fun getSavedPosition(uri: String): Long {
        return playbackRepository.getRecentPlayback(uri)?.position ?: 0L
    }

    fun setSpeedScope(scope: PlaybackSpeedScope) {
        viewModelScope.launch { themePreferences.saveSpeedScope(scope) }
    }

    fun setGlobalPlaybackSpeed(speed: Float) {
        viewModelScope.launch { themePreferences.saveGlobalPlaybackSpeed(speed) }
    }

    fun getFolderSpeed(folderName: String): Flow<Float?> = themePreferences.getFolderPlaybackSpeed(folderName)

    fun setFolderPlaybackSpeed(folderName: String, speed: Float) {
        viewModelScope.launch { themePreferences.saveFolderPlaybackSpeed(folderName, speed) }
    }

    fun getFolderGridView(folderKey: String, defaultGrid: Boolean = true): Flow<Boolean> =
        themePreferences.getFolderGridView(folderKey, defaultGrid)

    fun setFolderGridView(folderKey: String, isGrid: Boolean) {
        viewModelScope.launch { themePreferences.saveFolderGridView(folderKey, isGrid) }
    }

    fun getFolderSortOrder(folderKey: String, defaultSort: String = MediaSortOrder.DATE_DESC.name): Flow<String> =
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
