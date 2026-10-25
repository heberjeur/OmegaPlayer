package com.mohamedaminelouati.oplayer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import com.mohamedaminelouati.oplayer.data.MediaSortOrder
import com.mohamedaminelouati.oplayer.data.VideoModel
import com.mohamedaminelouati.oplayer.data.PlaybackSpeedScope
import com.mohamedaminelouati.oplayer.data.BrightnessScope
import com.mohamedaminelouati.oplayer.data.RecentPlayback
import com.mohamedaminelouati.oplayer.data.Playlist
import com.mohamedaminelouati.oplayer.data.SearchScope
import com.mohamedaminelouati.oplayer.domain.usecase.media.GetVideosUseCase
import com.mohamedaminelouati.oplayer.domain.usecase.media.SyncMediaUseCase
import com.mohamedaminelouati.oplayer.domain.usecase.playback.PlaylistUseCases
import com.mohamedaminelouati.oplayer.domain.usecase.playback.GetRecentPlaybackUseCase
import com.mohamedaminelouati.oplayer.data.repository.PlaybackRepository
import com.mohamedaminelouati.oplayer.data.ThemePreferences
import com.mohamedaminelouati.oplayer.data.SecurityLockType
import com.mohamedaminelouati.oplayer.util.SecurityUtils
import com.mohamedaminelouati.oplayer.data.AudioModel
import com.mohamedaminelouati.oplayer.data.PlaylistItem
import com.mohamedaminelouati.oplayer.media.PlaybackConnection
import com.mohamedaminelouati.oplayer.media.PlaybackQueueItem
import com.mohamedaminelouati.oplayer.media.PlaybackSource
import com.mohamedaminelouati.oplayer.media.PlayerRepeatMode
import com.mohamedaminelouati.oplayer.media.SleepTimerController
import com.mohamedaminelouati.oplayer.service.PlaybackService
import com.mohamedaminelouati.oplayer.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    application: Application,
    private val getVideosUseCase: GetVideosUseCase,
    private val syncMediaUseCase: SyncMediaUseCase,
    private val playlistUseCases: PlaylistUseCases,
    private val getRecentPlaybackUseCase: GetRecentPlaybackUseCase,
    private val playbackRepository: PlaybackRepository,
    private val mediaRepository: com.mohamedaminelouati.oplayer.data.repository.MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val themePreferences: ThemePreferences,
    val eqManager: com.mohamedaminelouati.oplayer.media.EqManager
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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val activeVideoUri: StateFlow<String?> = playbackConnection.currentMediaItem
        .map { it?.localConfiguration?.uri?.toString() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isBackgroundPlayEnabled = MutableStateFlow(true)
    val isBackgroundPlayEnabled: StateFlow<Boolean> = _isBackgroundPlayEnabled.asStateFlow()

    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying
    val mediaController: StateFlow<androidx.media3.session.MediaController?> = playbackConnection.mediaController
    val repeatMode: StateFlow<PlayerRepeatMode> = playbackConnection.repeatMode
    val activeQueue: StateFlow<List<PlaybackQueueItem>> = playbackConnection.currentQueue
    val playbackSource: StateFlow<PlaybackSource> = playbackConnection.playbackSource

    fun cycleRepeatMode() = playbackConnection.cycleRepeatMode()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _videoError = MutableStateFlow<String?>(null)

    private val _volumeKeyEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val volumeKeyEvents = _volumeKeyEvents.asSharedFlow()

    fun dispatchVolumeKeyEvent(keyCode: Int) {
        _volumeKeyEvents.tryEmit(keyCode)
    }
    val videoError: StateFlow<String?> = _videoError.asStateFlow()

    val excludedFolders: StateFlow<Set<String>> = themePreferences.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val showRecentHistoryOnHome: StateFlow<Boolean> = themePreferences.showRecentHistoryOnHome
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showHistoryTab: StateFlow<Boolean> = themePreferences.showHistoryTab
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val speedScope: StateFlow<PlaybackSpeedScope> = themePreferences.speedScope
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackSpeedScope.GLOBAL)

    val globalPlaybackSpeed: StateFlow<Float> = themePreferences.globalPlaybackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val showPlayerClock: StateFlow<Boolean> = themePreferences.showPlayerClock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showPlayerBattery: StateFlow<Boolean> = themePreferences.showPlayerBattery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showPlayerMediaInfo: StateFlow<Boolean> = themePreferences.showPlayerMediaInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showPlayerVolume: StateFlow<Boolean> = themePreferences.showPlayerVolume
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showPlayerBrightness: StateFlow<Boolean> = themePreferences.showPlayerBrightness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val playerOrientation: StateFlow<Int> = themePreferences.playerOrientation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val upNextFullyExpanded: StateFlow<Boolean> = themePreferences.upNextFullyExpanded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultPlaybackSpeed: StateFlow<Float> = themePreferences.defaultPlaybackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val defaultViewMode: StateFlow<Int> = themePreferences.defaultViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val defaultSortOrder: StateFlow<String> = themePreferences.defaultSortOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaSortOrder.DATE_DESC.name)

    val defaultSearchScope: StateFlow<SearchScope> = themePreferences.defaultSearchScope
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchScope.CURRENT_TYPE)

    val volumeBoostEnabled: StateFlow<Boolean> = themePreferences.volumeBoostEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val audioNormalizationEnabled: StateFlow<Boolean> = themePreferences.audioNormalizationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val audioNormalizationDb: StateFlow<Float> = themePreferences.audioNormalizationDb
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6.0f)

    val skipSilenceEnabled: StateFlow<Boolean> = playbackConnection.skipSilenceEnabled

    val savePositionOnQuit: StateFlow<Boolean> = themePreferences.savePositionOnQuit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val closeAfterQueueEnds: StateFlow<Boolean> = themePreferences.closeAfterQueueEnds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val brightnessScope: StateFlow<BrightnessScope> = themePreferences.brightnessScope
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrightnessScope.NONE)

    val globalBrightness: StateFlow<Float> = themePreferences.globalBrightness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1f)

    val keepScreenOnWhenPaused: StateFlow<Boolean> = themePreferences.keepScreenOnWhenPaused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val gestureVolumeEnabled: StateFlow<Boolean> = themePreferences.gestureVolumeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gestureBrightnessEnabled: StateFlow<Boolean> = themePreferences.gestureBrightnessEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gesturePinchToZoomEnabled: StateFlow<Boolean> = themePreferences.gesturePinchToZoomEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gestureSeekForwardEnabled: StateFlow<Boolean> = themePreferences.gestureSeekForwardEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gestureSeekBackwardEnabled: StateFlow<Boolean> = themePreferences.gestureSeekBackwardEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gesturePlayPauseEnabled: StateFlow<Boolean> = themePreferences.gesturePlayPauseEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gestureLongPressSpeedEnabled: StateFlow<Boolean> = themePreferences.gestureLongPressSpeedEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gestureTapControlsEnabled: StateFlow<Boolean> = themePreferences.gestureTapControlsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val securityLockType: StateFlow<SecurityLockType> = themePreferences.securityLockType
        .stateIn(viewModelScope, SharingStarted.Eagerly, SecurityLockType.NONE)

    val securityPinHash: StateFlow<String> = themePreferences.securityPinHash
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val securityPatternHash: StateFlow<String> = themePreferences.securityPatternHash
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val securityAppLockEnabled: StateFlow<Boolean> = themePreferences.securityAppLockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val securityLockedFolders: StateFlow<Set<String>> = themePreferences.securityLockedFolders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val securityLockedMedia: StateFlow<Set<String>> = themePreferences.securityLockedMedia
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleVolumeBoost(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveVolumeBoostEnabled(enabled)
        }
    }

    fun toggleAudioNormalization(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveAudioNormalizationEnabled(enabled)
        }
    }

    fun setAudioNormalizationDb(db: Float) {
        viewModelScope.launch {
            themePreferences.saveAudioNormalizationDb(db)
        }
    }

    fun setSavePositionOnQuit(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveSavePositionOnQuit(enabled)
        }
    }

    fun setSecurityLockType(type: SecurityLockType) {
        viewModelScope.launch { themePreferences.saveSecurityLockType(type) }
    }

    fun setSecurityPin(pin: String) {
        viewModelScope.launch {
            val hash = SecurityUtils.hashSecret(pin)
            themePreferences.saveSecurityPinHash(hash)
        }
    }

    fun setSecurityPattern(pattern: String) {
        viewModelScope.launch {
            val hash = SecurityUtils.hashSecret(pattern)
            themePreferences.saveSecurityPatternHash(hash)
        }
    }

    fun setSecurityAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveSecurityAppLockEnabled(enabled) }
    }

    fun toggleFolderLock(path: String) {
        viewModelScope.launch { themePreferences.toggleFolderLock(path) }
    }

    fun unlockFolder(path: String) {
        viewModelScope.launch { themePreferences.unlockFolder(path) }
    }

    fun toggleMediaLock(uri: String) {
        viewModelScope.launch { themePreferences.toggleMediaLock(uri) }
    }

    fun unlockMedia(uri: String) {
        viewModelScope.launch { themePreferences.unlockMedia(uri) }
    }

    fun clearAllLocks() {
        viewModelScope.launch { themePreferences.clearAllLocks() }
    }

    fun isFolderLocked(path: String): Boolean = securityLockedFolders.value.contains(path)

    fun isMediaLocked(uri: String): Boolean = securityLockedMedia.value.contains(uri)

    fun isFolderProtected(path: String): Boolean {
        if (securityLockType.value == SecurityLockType.NONE) return false
        val locked = securityLockedFolders.value
        if (locked.isEmpty()) return false
        return locked.any { path == it || path.startsWith("$it/") }
    }

    fun isMediaProtected(uri: String, parentFolderPath: String? = null): Boolean {
        if (securityLockType.value == SecurityLockType.NONE) return false
        val lockedMedia = securityLockedMedia.value
        if (lockedMedia.contains(uri)) return true
        if (!parentFolderPath.isNullOrEmpty()) {
            val lockedFolders = securityLockedFolders.value
            if (lockedFolders.isNotEmpty() && lockedFolders.any { parentFolderPath == it || parentFolderPath.startsWith("$it/") }) {
                return true
            }
        }
        return false
    }

    fun verifyPin(pin: String): Boolean {
        val hash = securityPinHash.value.ifEmpty {
            runBlocking { themePreferences.securityPinHash.first() }
        }
        return SecurityUtils.verifySecret(pin, hash)
    }

    fun verifyPattern(pattern: String): Boolean {
        val hash = securityPatternHash.value.ifEmpty {
            runBlocking { themePreferences.securityPatternHash.first() }
        }
        return SecurityUtils.verifySecret(pattern, hash)
    }

    fun setCloseAfterQueueEnds(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.saveCloseAfterQueueEnds(enabled)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawVideos: Flow<List<VideoModel>> = getVideosUseCase()
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
        .map { resource -> if (resource is Resource.Success) resource.data ?: emptyList() else emptyList() }

    val videos: StateFlow<List<VideoModel>> = combine(rawVideos, excludedFolders) { videoList, excluded ->
        if (excluded.isEmpty()) videoList
        else videoList.filter {
            val folder = File(it.path).parentFile?.name ?: "Internal"
            !excluded.contains(folder)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosByUri: StateFlow<Map<String, VideoModel>> = videos
        .map { list -> list.associateBy { it.uri.toString() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recentPlayback: StateFlow<List<RecentPlayback>> = getRecentPlaybackUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val fullHistory: StateFlow<List<RecentPlayback>> = playbackRepository.getAllRecentPlayback()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playbackProgressMap: StateFlow<Map<String, Float>> = combine(
        playbackRepository.getAllRecentPlayback(),
        playbackConnection.livePlaybackProgress
    ) { recentList, liveProgressMap ->
        val map = recentList.associate { recent ->
            val ratio = if (recent.duration > 0L) {
                (recent.position.toFloat() / recent.duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
            recent.uri to ratio
        }.toMutableMap()
        map.putAll(liveProgressMap)
        map
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
            playbackConnection.clearLiveProgress()
        }
    }

    fun resetAllPlaybackPositions() {
        viewModelScope.launch {
            playbackRepository.resetAllPlaybackPositions()
            playbackConnection.clearLiveProgress()
        }
    }

    fun deleteHistoryItem(uri: String) {
        viewModelScope.launch {
            playbackRepository.deleteRecentPlayback(uri)
        }
    }

    private val sleepTimer = SleepTimerController(viewModelScope) { playbackConnection.mediaController.value?.pause() }
    val sleepTimerActive: StateFlow<Boolean> = sleepTimer.active
    val sleepTimerTimeLeft: StateFlow<Long> = sleepTimer.timeLeftMillis

    fun setSleepTimer(minutes: Int) {
        sleepTimer.start(minutes)
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
            MediaSortOrder.SIZE_ASC -> result.sortedBy { it.size }
            MediaSortOrder.SIZE_DESC -> result.sortedByDescending { it.size }
            MediaSortOrder.DURATION_ASC -> result.sortedBy { it.duration }
            MediaSortOrder.DURATION_DESC -> result.sortedByDescending { it.duration }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val folders: StateFlow<Map<String, Int>> = videos
        .map { videoList ->
            videoList.groupBy { File(it.path).parentFile?.name ?: "Internal" }
                .mapValues { it.value.size }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val videosInSelectedFolder: StateFlow<List<VideoModel>> = combine(videos, _selectedFolder) { videoList, folder ->
        if (folder == null) emptyList()
        else videoList.filter { File(it.path).parentFile?.absolutePath == folder }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
        stopIfPlaying(listOf(uri))
    }

    fun stopIfPlaying(uris: List<Uri>) {
        playbackConnection.mediaController.value?.let { player ->
            val currentUri = player.currentMediaItem?.localConfiguration?.uri
            val isCurrentDeleted = currentUri != null && uris.contains(currentUri)

            if (isCurrentDeleted && !autoPlayNext.value) {
                player.stop()
                player.clearMediaItems()
            } else {
                for (i in player.mediaItemCount - 1 downTo 0) {
                    val itemUri = player.getMediaItemAt(i).localConfiguration?.uri
                    if (itemUri != null && uris.contains(itemUri)) {
                        player.removeMediaItem(i)
                    }
                }
                if (player.mediaItemCount == 0) {
                    player.stop()
                    player.clearMediaItems()
                }
            }
        }
    }

    fun getVideosInFolder(folderPath: String): List<VideoModel> {
        return videos.value.filter { File(it.path).parentFile?.absolutePath == folderPath }
    }

    fun manualRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            syncMediaUseCase()
            _isLoading.value = false
        }
    }

    fun autoSync() {
        viewModelScope.launch(Dispatchers.IO) {
            syncMediaUseCase()
        }
    }

    fun fetchVideos(context: Context) {
        manualRefresh()
    }

    fun refreshVideos(context: Context) {
        manualRefresh()
    }

    fun onVideosDeleted(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.removeVideosFromCache(uris.map { it.toString() })
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
            if (!themePreferences.savePositionOnQuit.first()) return@launch
            if (themePreferences.isHistoryPaused.first()) return@launch

            playbackRepository.saveRecentPlayback(
                RecentPlayback(
                    uri = currentUri,
                    position = position,
                    duration = duration,
                    mediaType = "video",
                    name = name,
                    size = video.size
                )
            )
        }
    }

    suspend fun getSavedPosition(uri: String): Long {
        val recent = playbackRepository.getRecentPlayback(uri) ?: return 0L
        if (recent.duration > 0L && recent.position >= recent.duration - 2000L) {
            return 0L
        }
        return recent.position
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

    fun getHistorySpeed(): Flow<Float?> = themePreferences.getHistorySpeed()

    fun setHistoryPlaybackSpeed(speed: Float) {
        viewModelScope.launch { themePreferences.saveHistoryPlaybackSpeed(speed) }
    }

    fun getPlaylistSpeed(playlistId: Int): Flow<Float?> = themePreferences.getPlaylistSpeed(playlistId)

    fun setPlaylistPlaybackSpeed(playlistId: Int, speed: Float) {
        viewModelScope.launch { themePreferences.savePlaylistPlaybackSpeed(playlistId, speed) }
    }

    fun setSkipSilenceEnabled(enabled: Boolean, persist: Boolean = true) {
        playbackConnection.setSkipSilenceEnabled(enabled, persist)
    }

    fun setBrightnessScope(scope: BrightnessScope) {
        viewModelScope.launch { themePreferences.saveBrightnessScope(scope) }
    }

    fun saveGlobalBrightness(brightness: Float) {
        viewModelScope.launch { themePreferences.saveGlobalBrightness(brightness) }
    }

    fun getFolderBrightness(folderName: String): Flow<Float?> = themePreferences.getFolderBrightness(folderName)

    fun saveFolderBrightness(folderName: String, brightness: Float) {
        viewModelScope.launch { themePreferences.saveFolderBrightness(folderName, brightness) }
    }

    fun setKeepScreenOnWhenPaused(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveKeepScreenOnWhenPaused(enabled) }
    }

    fun getFolderViewMode(folderKey: String, defaultMode: Int? = null): Flow<Int> =
        themePreferences.getFolderViewMode(folderKey, defaultMode)

    fun setFolderViewMode(folderKey: String, mode: Int) {
        viewModelScope.launch { themePreferences.saveFolderViewMode(folderKey, mode) }
    }

    fun getFolderGridView(folderKey: String, defaultGrid: Boolean = true): Flow<Boolean> =
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

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch { themePreferences.saveDefaultPlaybackSpeed(speed) }
    }

    fun setDefaultViewMode(mode: Int) {
        viewModelScope.launch { themePreferences.saveDefaultViewMode(mode) }
    }

    fun setDefaultSortOrder(sortOrder: String) {
        viewModelScope.launch { themePreferences.saveDefaultSortOrder(sortOrder) }
    }

    fun setDefaultSearchScope(scope: SearchScope) {
        viewModelScope.launch { themePreferences.saveDefaultSearchScope(scope) }
    }

    fun resetDefaultPreferences(clearFolderCustomizations: Boolean) {
        viewModelScope.launch {
            themePreferences.resetDefaultPreferences(clearFolderCustomizations)
        }
    }

    fun setGestureVolumeEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveGestureVolumeEnabled(enabled) }
    }

    fun setGestureBrightnessEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveGestureBrightnessEnabled(enabled) }
    }

    fun setGesturePinchToZoomEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveGesturePinchToZoomEnabled(enabled) }
    }

    fun setGestureSeekForwardEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveGestureSeekForwardEnabled(enabled) }
    }

    fun setGestureSeekBackwardEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveGestureSeekBackwardEnabled(enabled) }
    }

    fun setGesturePlayPauseEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveGesturePlayPauseEnabled(enabled) }
    }

    fun setGestureLongPressSpeedEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveGestureLongPressSpeedEnabled(enabled) }
    }

    fun setGestureTapControlsEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.saveGestureTapControlsEnabled(enabled) }
    }

    val subtitleTextSize: StateFlow<Int> = themePreferences.subtitleTextSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)

    fun setSubtitleTextSize(size: Int) {
        viewModelScope.launch { themePreferences.saveSubtitleTextSize(size) }
    }

    val subtitleTextColor: StateFlow<Int> = themePreferences.subtitleTextColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSubtitleTextColor(colorIndex: Int) {
        viewModelScope.launch { themePreferences.saveSubtitleTextColor(colorIndex) }
    }

    val subtitleBgStyle: StateFlow<Int> = themePreferences.subtitleBgStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    fun setSubtitleBgStyle(bgIndex: Int) {
        viewModelScope.launch { themePreferences.saveSubtitleBgStyle(bgIndex) }
    }

    fun playPlaylist(
        playlistItems: List<PlaylistItem>,
        startIndex: Int,
        videos: List<VideoModel>,
        audios: List<AudioModel>,
        videosByUri: Map<String, VideoModel>? = null,
        audiosByUri: Map<String, AudioModel>? = null
    ) {
        playbackConnection.playPlaylist(playlistItems, startIndex, videos, audios, videosByUri = videosByUri, audiosByUri = audiosByUri)
    }

    fun playHistory(
        historyItems: List<RecentPlayback>,
        startIndex: Int,
        videos: List<VideoModel>,
        audios: List<AudioModel>,
        audiosByUri: Map<String, AudioModel>? = null
    ) {
        playbackConnection.playHistory(historyItems, startIndex, videos, audios, audiosByUri = audiosByUri)
    }

    fun setFolderQueue(folderVideos: List<VideoModel>) {
        val queueItems = folderVideos.map { video ->
            PlaybackQueueItem(
                uri = video.uri.toString(),
                title = video.name,
                duration = video.duration,
                isVideo = true
            )
        }
        playbackConnection.setQueue(queueItems)
    }

    fun moveQueueItem(from: Int, to: Int, fallbackItems: List<com.mohamedaminelouati.oplayer.media.PlaybackQueueItem>? = null) {
        playbackConnection.moveQueueItem(from, to, fallbackItems)
    }

    fun removeQueueItem(index: Int, fallbackItems: List<com.mohamedaminelouati.oplayer.media.PlaybackQueueItem>? = null) {
        playbackConnection.removeQueueItem(index, fallbackItems)
    }

    fun removeQueueItems(uris: Collection<String>) {
        playbackConnection.removeQueueItems(uris)
    }

    val showSystemStatusBar: StateFlow<Boolean> = themePreferences.showSystemStatusBar.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoPlayNext: StateFlow<Boolean> = themePreferences.autoPlayNext.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoPip: StateFlow<Boolean> = themePreferences.autoPip.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val controlsTimeout: StateFlow<Int> = themePreferences.controlsTimeout.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)
    val seekIncrement: StateFlow<Int> = themePreferences.seekIncrement.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)
    val folderFlattenThreshold: StateFlow<Int> = themePreferences.folderFlattenThreshold.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val notifShowPrevious: StateFlow<Boolean> = themePreferences.notifShowPrevious.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val notifShowRewind: StateFlow<Boolean> = themePreferences.notifShowRewind.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val notifShowForward: StateFlow<Boolean> = themePreferences.notifShowForward.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val notifShowNext: StateFlow<Boolean> = themePreferences.notifShowNext.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val notifShowSpeed: StateFlow<Boolean> = themePreferences.notifShowSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val notifShowStop: StateFlow<Boolean> = themePreferences.notifShowStop.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val notifShowClose: StateFlow<Boolean> = themePreferences.notifShowClose.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val notifShowRepeat: StateFlow<Boolean> = themePreferences.notifShowRepeat.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val notifShowShuffle: StateFlow<Boolean> = themePreferences.notifShowShuffle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _stopAfterCurrent = MutableStateFlow(false)
    val stopAfterCurrent: StateFlow<Boolean> = _stopAfterCurrent.asStateFlow()

    fun setStopAfterCurrent(stop: Boolean) {
        _stopAfterCurrent.value = stop
    }

    fun toggleSystemStatusBar(show: Boolean) = viewModelScope.launch { themePreferences.saveShowSystemStatusBar(show) }
    fun setNotifShowPrevious(show: Boolean) = viewModelScope.launch { themePreferences.saveNotifShowPrevious(show) }
    fun setNotifShowRewind(show: Boolean) = viewModelScope.launch { themePreferences.saveNotifShowRewind(show) }
    fun setNotifShowForward(show: Boolean) = viewModelScope.launch { themePreferences.saveNotifShowForward(show) }
    fun setNotifShowNext(show: Boolean) = viewModelScope.launch { themePreferences.saveNotifShowNext(show) }
    fun setNotifShowSpeed(show: Boolean) = viewModelScope.launch { themePreferences.saveNotifShowSpeed(show) }
    fun setNotifShowStop(show: Boolean) = viewModelScope.launch { themePreferences.saveNotifShowStop(show) }
    fun setNotifShowClose(show: Boolean) = viewModelScope.launch { themePreferences.saveNotifShowClose(show) }
    fun setNotifShowRepeat(show: Boolean) = viewModelScope.launch { themePreferences.saveNotifShowRepeat(show) }
    fun setNotifShowShuffle(show: Boolean) = viewModelScope.launch { themePreferences.saveNotifShowShuffle(show) }
    fun setAutoPlayNext(autoPlay: Boolean) = viewModelScope.launch { themePreferences.saveAutoPlayNext(autoPlay) }
    fun setAutoPip(autoPip: Boolean) = viewModelScope.launch { themePreferences.saveAutoPip(autoPip) }
    fun setUpNextFullyExpanded(expanded: Boolean) = viewModelScope.launch { themePreferences.saveUpNextFullyExpanded(expanded) }

    private val _currentNavPath = MutableStateFlow<String?>(null)
    val currentNavPath: StateFlow<String?> = _currentNavPath.asStateFlow()

    val folderTree: StateFlow<com.mohamedaminelouati.oplayer.data.model.FolderNode?> = combine(videos, folderFlattenThreshold) { videoList, threshold ->
        buildVideoTree(videoList, threshold)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentVisibleFolders: StateFlow<List<com.mohamedaminelouati.oplayer.data.model.FolderNode>> = combine(folderTree, _currentNavPath) { tree, path ->
        if (tree == null) emptyList()
        else if (path.isNullOrEmpty()) tree.getVisibleChildren()
        else (findVideoNode(tree, path) ?: tree).getVisibleChildren()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateIntoFolder(path: String) {
        _currentNavPath.value = path
    }

    fun navigateUp(tree: com.mohamedaminelouati.oplayer.data.model.FolderNode?) {
        val current = _currentNavPath.value ?: return
        if (tree == null) { _currentNavPath.value = null; return }
        var parent = findVideoNode(tree, current)?.let { findVideoParent(tree, current) }
        while (parent != null && parent.isFlattened && parent.path != tree.path) {
            parent = findVideoParent(tree, parent.path)
        }
        if (parent == null || parent.path == tree.path) {
            _currentNavPath.value = null
        } else {
            _currentNavPath.value = parent.path.takeIf { it.isNotEmpty() }
        }
    }

    fun navigateToRoot() {
        _currentNavPath.value = null
    }

    fun playVideos(folderVideos: List<VideoModel>, startIndex: Int) {
        viewModelScope.launch {
            val savePosition = themePreferences.savePositionOnQuit.first()
            val startPos = if (savePosition && startIndex in folderVideos.indices) {
                getSavedPosition(folderVideos[startIndex].uri.toString())
            } else 0L
            playbackConnection.playVideos(folderVideos, startIndex, startPos)
        }
    }

    fun playMixedMedia(items: List<com.mohamedaminelouati.oplayer.data.SearchMediaItem>, startIndex: Int) {
        viewModelScope.launch {
            val savePosition = themePreferences.savePositionOnQuit.first()
            val startPos = if (savePosition && startIndex in items.indices) {
                getSavedPosition(items[startIndex].uri.toString())
            } else 0L
            playbackConnection.playMixedMedia(items, startIndex, startPos)
        }
    }

    private fun buildVideoTree(videos: List<VideoModel>, threshold: Int): com.mohamedaminelouati.oplayer.data.model.FolderNode {
        val root = com.mohamedaminelouati.oplayer.data.model.FolderNode("Internal", "")
        for (video in videos) {
            val parentFile = File(video.path).parentFile ?: continue
            val absPath = parentFile.absolutePath
            val parts = absPath.split("/").filter { it.isNotEmpty() }
            var node = root
            var builtPath = ""
            for (part in parts) {
                builtPath = if (builtPath.isEmpty()) "/$part" else "$builtPath/$part"
                node = node.subFolders.getOrPut(part) { com.mohamedaminelouati.oplayer.data.model.FolderNode(part, builtPath) }
                node.videoCount++
            }
            node.directMediaCount++
            root.videoCount++
        }
        flattenVideoTree(root, threshold)
        var effective = root
        while (effective.subFolders.size == 1 && effective.getVisibleChildren().size == 1 && effective.directMediaCount == 0) {
            val candidate = effective.getVisibleChildren().first()
            if (candidate.subFolders.isEmpty()) break
            effective = candidate
        }
        return effective
    }

    private fun flattenVideoTree(node: com.mohamedaminelouati.oplayer.data.model.FolderNode, threshold: Int) {
        for (child in node.subFolders.values) flattenVideoTree(child, threshold)
        if (node.subFolders.isNotEmpty() && node.subFolders.size <= threshold) {
            node.isFlattened = true
        }
    }

    private fun findVideoNode(root: com.mohamedaminelouati.oplayer.data.model.FolderNode, path: String): com.mohamedaminelouati.oplayer.data.model.FolderNode? {
        if (root.path == path) return root
        for (child in root.subFolders.values) {
            val found = findVideoNode(child, path)
            if (found != null) return found
        }
        return null
    }

    private fun findVideoParent(root: com.mohamedaminelouati.oplayer.data.model.FolderNode, targetPath: String): com.mohamedaminelouati.oplayer.data.model.FolderNode? {
        for (child in root.subFolders.values) {
            if (child.path == targetPath) return root
            val found = findVideoParent(child, targetPath)
            if (found != null) return found
        }
        return null
    }

}
