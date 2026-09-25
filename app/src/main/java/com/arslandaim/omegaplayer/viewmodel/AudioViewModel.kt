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
import com.arslandaim.omegaplayer.domain.usecase.media.SyncMediaUseCase
import com.arslandaim.omegaplayer.domain.usecase.playback.GetRecentPlaybackUseCase
import com.arslandaim.omegaplayer.domain.usecase.playback.PlaylistUseCases
import com.arslandaim.omegaplayer.media.PlaybackConnection
import com.arslandaim.omegaplayer.media.PlaybackQueueItem
import com.arslandaim.omegaplayer.util.Resource
import com.arslandaim.omegaplayer.util.StartupTrace
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
    private val getAudiosUseCase: com.arslandaim.omegaplayer.domain.usecase.media.GetAudiosUseCase,
    private val syncMediaUseCase: SyncMediaUseCase,
    private val playlistUseCases: PlaylistUseCases,
    private val getRecentPlaybackUseCase: GetRecentPlaybackUseCase,
    private val playbackConnection: PlaybackConnection,
    private val playbackRepository: PlaybackRepository,
    private val mediaRepository: com.arslandaim.omegaplayer.data.repository.MediaRepository,
    private val themePreferences: ThemePreferences,
    val eqManager: com.arslandaim.omegaplayer.media.EqManager
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentNavPath = MutableStateFlow<String?>(null)
    private val _selectedFolder = MutableStateFlow<String?>(null)

    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    val activeAudioUri: StateFlow<String?> = playbackConnection.currentMediaItem
        .map { it?.localConfiguration?.uri?.toString() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val volumeBoostEnabled: StateFlow<Boolean> = themePreferences.volumeBoostEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying
    val mediaController: StateFlow<MediaController?> = playbackConnection.mediaController
    val activeQueue: StateFlow<List<PlaybackQueueItem>> = playbackConnection.currentQueue

    private val _audioError = MutableStateFlow<String?>(null)
    val audioError: StateFlow<String?> = _audioError.asStateFlow()

    val excludedFolders: StateFlow<Set<String>> = themePreferences.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val autoPlayNext: StateFlow<Boolean> = themePreferences.autoPlayNext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val playerOrientation: StateFlow<Int> = themePreferences.playerOrientation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val upNextFullyExpanded: StateFlow<Boolean> = themePreferences.upNextFullyExpanded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawAudios: Flow<List<AudioModel>> = getAudiosUseCase()
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
        .map { resource -> if (resource is Resource.Success) resource.data ?: emptyList() else emptyList() }

    val audios: StateFlow<List<AudioModel>> = combine(rawAudios, excludedFolders) { audioList, excluded ->
        if (excluded.isEmpty()) audioList
        else audioList.filter {
            val folder = File(it.path).parentFile?.name ?: "Internal"
            !excluded.contains(folder)
        }
    }.onEach { list -> StartupTrace.markOnce("audios.firstEmission") { "audios flow first emission (${list.size} items)" } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audiosByUri: StateFlow<Map<String, AudioModel>> = audios
        .map { list -> list.associateBy { it.uri.toString() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
            MediaSortOrder.SIZE_ASC -> result.sortedBy { it.size }
            MediaSortOrder.SIZE_DESC -> result.sortedByDescending { it.size }
            MediaSortOrder.DURATION_ASC -> result.sortedBy { it.duration }
            MediaSortOrder.DURATION_DESC -> result.sortedByDescending { it.duration }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistUseCases.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentPlayback: StateFlow<List<RecentPlayback>> = getRecentPlaybackUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val fullHistory: StateFlow<List<RecentPlayback>> = playbackRepository.getAllRecentPlayback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val autoPip: StateFlow<Boolean> = themePreferences.autoPip.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    val controlsTimeout: StateFlow<Int> = themePreferences.controlsTimeout.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = 3
    )

    val isHistoryPaused: StateFlow<Boolean> = themePreferences.isHistoryPaused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val folderFlattenThreshold: StateFlow<Int> = themePreferences.folderFlattenThreshold.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = 5
    )

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

    fun deleteHistoryItem(uri: String) {
        viewModelScope.launch {
            playbackRepository.deleteRecentPlayback(uri)
        }
    }

    fun setStopAfterCurrent(enabled: Boolean) {
        _stopAfterCurrent.value = enabled
        if (enabled) {
            _sleepTimerActive.value = true
            _sleepTimerTimeLeft.value = 0
            sleepTimerJob?.cancel()
            
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
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val audiosInSelectedFolder: StateFlow<List<AudioModel>> = combine(audios, _selectedFolder) { audioList, folder ->
        if (folder == null) emptyList()
        else audioList.filter { File(it.path).parentFile?.absolutePath == folder }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
                            .setArtist("")
                            .setAlbumTitle("")
                            .setAlbumArtist("")
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
        stopIfPlaying(listOf(uri))
    }

    fun stopIfPlaying(uris: List<Uri>) {
        val controller = playbackConnection.mediaController.value ?: return
        val currentUri = controller.currentMediaItem?.localConfiguration?.uri
        val isCurrentDeleted = currentUri != null && uris.contains(currentUri)

        if (isCurrentDeleted && !autoPlayNext.value) {
            controller.stop()
            controller.clearMediaItems()
        } else {
            for (i in controller.mediaItemCount - 1 downTo 0) {
                val itemUri = controller.getMediaItemAt(i).localConfiguration?.uri
                if (itemUri != null && uris.contains(itemUri)) {
                    controller.removeMediaItem(i)
                }
            }
            if (controller.mediaItemCount == 0) {
                controller.stop()
                controller.clearMediaItems()
            }
        }
    }

    fun getAudiosInFolder(folderPath: String): List<AudioModel> {
        return audios.value.filter { File(it.path).parentFile?.absolutePath == folderPath }
    }

    fun manualRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                syncMediaUseCase()
            } catch (e: Exception) {
                _audioError.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAudios(context: Context) {
        manualRefresh()
    }

    fun refreshAudios(context: Context) {
        manualRefresh()
    }

    fun onAudiosDeleted(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.removeAudiosFromCache(uris.map { it.toString() })
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

    fun setUpNextFullyExpanded(expanded: Boolean) = viewModelScope.launch { themePreferences.saveUpNextFullyExpanded(expanded) }

    val folderTree: StateFlow<com.arslandaim.omegaplayer.data.model.FolderNode?> = combine(audios, folderFlattenThreshold) { audioList, threshold ->
        StartupTrace.trace("folder tree rebuilt (${audioList.size} items)") { buildAudioTree(audioList, threshold) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentVisibleFolders: StateFlow<List<com.arslandaim.omegaplayer.data.model.FolderNode>> = combine(folderTree, _currentNavPath) { tree, path ->
        if (tree == null) emptyList()
        else if (path.isNullOrEmpty()) tree.getVisibleChildren()
        else (findAudioNode(tree, path) ?: tree).getVisibleChildren()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentNavPath: StateFlow<String?> = _currentNavPath.asStateFlow()

    fun navigateIntoFolder(path: String) {
        _currentNavPath.value = path
    }

    fun navigateUp(tree: com.arslandaim.omegaplayer.data.model.FolderNode?) {
        val current = _currentNavPath.value ?: return
        if (tree == null) { _currentNavPath.value = null; return }
        var parent = findAudioNode(tree, current)?.let { findAudioParent(tree, current) }
        while (parent != null && parent.isFlattened && parent.path != tree.path) {
            parent = findAudioParent(tree, parent.path)
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

    fun playAudios(folderAudios: List<AudioModel>, startIndex: Int) {
        playbackConnection.playAudios(folderAudios, startIndex)
    }

    private fun buildAudioTree(audios: List<AudioModel>, threshold: Int): com.arslandaim.omegaplayer.data.model.FolderNode {
        val root = com.arslandaim.omegaplayer.data.model.FolderNode("Internal", "")
        for (audio in audios) {
            val parentFile = File(audio.path).parentFile ?: continue
            val absPath = parentFile.absolutePath
            val parts = absPath.split("/").filter { it.isNotEmpty() }
            var node = root
            var builtPath = ""
            for (part in parts) {
                builtPath = if (builtPath.isEmpty()) "/$part" else "$builtPath/$part"
                node = node.subFolders.getOrPut(part) { com.arslandaim.omegaplayer.data.model.FolderNode(part, builtPath) }
                node.videoCount++
            }
            node.directMediaCount++
            root.videoCount++
        }
        flattenAudioTree(root, threshold)
        var effective = root
        while (effective.subFolders.size == 1 && effective.getVisibleChildren().size == 1 && effective.directMediaCount == 0) {
            val candidate = effective.getVisibleChildren().first()
            if (candidate.subFolders.isEmpty()) break
            effective = candidate
        }
        return effective
    }

    private fun flattenAudioTree(node: com.arslandaim.omegaplayer.data.model.FolderNode, threshold: Int) {
        for (child in node.subFolders.values) flattenAudioTree(child, threshold)
        if (node.subFolders.isNotEmpty() && node.subFolders.size <= threshold) {
            node.isFlattened = true
        }
    }

    private fun findAudioNode(root: com.arslandaim.omegaplayer.data.model.FolderNode, path: String): com.arslandaim.omegaplayer.data.model.FolderNode? {
        if (root.path == path) return root
        for (child in root.subFolders.values) {
            val found = findAudioNode(child, path)
            if (found != null) return found
        }
        return null
    }

    private fun findAudioParent(root: com.arslandaim.omegaplayer.data.model.FolderNode, targetPath: String): com.arslandaim.omegaplayer.data.model.FolderNode? {
        for (child in root.subFolders.values) {
            if (child.path == targetPath) return root
            val found = findAudioParent(child, targetPath)
            if (found != null) return found
        }
        return null
    }

}
