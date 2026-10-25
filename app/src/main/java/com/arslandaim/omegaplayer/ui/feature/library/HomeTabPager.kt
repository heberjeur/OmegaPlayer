package com.arslandaim.omegaplayer.ui.feature.library

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.data.AudioModel
import com.arslandaim.omegaplayer.data.Playlist
import com.arslandaim.omegaplayer.data.PlaylistItem
import com.arslandaim.omegaplayer.viewmodel.AudioViewModel
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel
import com.arslandaim.omegaplayer.data.RecentPlayback
import com.arslandaim.omegaplayer.ui.feature.library.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.unit.Dp
import com.arslandaim.omegaplayer.data.MediaSortOrder
import com.arslandaim.omegaplayer.data.SearchScope
import com.arslandaim.omegaplayer.data.SearchMediaItem
import com.arslandaim.omegaplayer.data.SecurityLockType
import com.arslandaim.omegaplayer.data.model.FolderNode
import com.arslandaim.omegaplayer.ui.feature.security.SecurityAuthDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeTabPager(
    pagerState: PagerState,
    activeTabs: List<MediaTab>,
    isLoading: Boolean,
    searchQuery: String,
    searchScope: SearchScope = SearchScope.CURRENT_TYPE,
    sortedAllMedia: List<SearchMediaItem> = emptyList(),
    bottomPadding: Dp,
    showRecentHistoryOnHome: Boolean,
    currentSortOrder: MediaSortOrder,
    currentSelectedFolder: String?,
    selectedVideoFolder: String?,
    selectedAudioFolder: String?,
    selectedPlaylistForDetails: Playlist?,
    recentPlayback: List<RecentPlayback>,
    sortedVideoFolders: List<FolderNode>,
    sortedAudioFolders: List<FolderNode>,
    sortedVideos: List<VideoModel>,
    sortedAudios: List<AudioModel>,
    sortedPlaylists: List<Playlist>,
    sortedHistory: List<RecentPlayback>,
    sortedPlaylistItems: List<PlaylistItem>,
    videos: List<VideoModel>,
    audios: List<AudioModel>,
    videosByUri: Map<String, VideoModel>,
    audiosByUri: Map<String, AudioModel>,
    isVideoPlaying: Boolean,
    isAudioPlaying: Boolean,
    activeVideoUri: String?,
    activeAudioUri: String?,
    padding: PaddingValues,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: VideoViewModel,
    audioViewModel: AudioViewModel,
    onVideoClick: (String, Long, String?) -> Unit,
    onAudioClick: (String, Long, String?) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenHistory: () -> Unit,
    onMediaPendingPlaylist: (String, String) -> Unit,
    onFolderPendingPlaylist: (String, String) -> Unit,
    onFolderExclude: (String, Boolean) -> Unit,
    onHistoryDeviceDelete: (RecentPlayback) -> Unit,
    onFolderDeviceDelete: (String, Boolean) -> Unit,
    onVideoDeviceDelete: (VideoModel) -> Unit,
    onAudioDeviceDelete: (AudioModel) -> Unit,
    onVideoRename: (VideoModel, String) -> Unit = { _, _ -> },
    onAudioRename: (AudioModel, String) -> Unit = { _, _ -> },
    onFolderRename: (String, String, Boolean) -> Unit = { _, _, _ -> },
    selectedKeys: Set<String> = emptySet(),
    isSelectionMode: Boolean = false,
    onToggleSelect: (String) -> Unit = {}
) {
    var preloadNeighborPages by remember { mutableStateOf(0) }
    LaunchedEffect(videos.isNotEmpty(), audios.isNotEmpty()) {
        if (videos.isEmpty() && audios.isEmpty()) return@LaunchedEffect
        delay(1200)
        preloadNeighborPages = 1
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val securityLockType by viewModel.securityLockType.collectAsStateWithLifecycle()
    val securityLockedFolders by viewModel.securityLockedFolders.collectAsStateWithLifecycle()
    val securityLockedMedia by viewModel.securityLockedMedia.collectAsStateWithLifecycle()

    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    if (pendingAuthAction != null && securityLockType != SecurityLockType.NONE) {
        SecurityAuthDialog(
            lockType = securityLockType,
            videoViewModel = viewModel,
            onSuccess = {
                val action = pendingAuthAction
                pendingAuthAction = null
                action?.invoke()
            },
            onDismiss = {
                pendingAuthAction = null
            }
        )
    }

    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text(stringResource(R.string.action_delete_playlist)) },
            text = { Text(stringResource(R.string.delete_playlist_confirm, playlist.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val p = playlistToDelete
                        playlistToDelete = null
                        p?.let { audioViewModel.deletePlaylist(it) }
                    }
                ) {
                    Text(stringResource(R.string.action_delete_playlist), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val getParentFolderForUri: (String) -> String? = { uri ->
        videosByUri[uri]?.path?.substringBeforeLast("/", "")
            ?: audiosByUri[uri]?.path?.substringBeforeLast("/", "")
    }

    val isFolderProtected: (String) -> Boolean = { path ->
        if (securityLockType == SecurityLockType.NONE || securityLockedFolders.isEmpty()) false
        else securityLockedFolders.any { path == it || path.startsWith("$it/") }
    }

    val isMediaProtected: (String, String?) -> Boolean = { uri, parentFolder ->
        if (securityLockType == SecurityLockType.NONE) false
        else if (securityLockedMedia.contains(uri)) true
        else if (!parentFolder.isNullOrEmpty() && securityLockedFolders.isNotEmpty()) {
            securityLockedFolders.any { parentFolder == it || parentFolder.startsWith("$it/") }
        } else false
    }

    val runWithFolderAuth: (String, () -> Unit) -> Unit = { path, action ->
        if (isFolderProtected(path)) {
            pendingAuthAction = action
        } else {
            action()
        }
    }

    val runWithMediaAuth: (String, () -> Unit) -> Unit = { uri, action ->
        val parentFolder = getParentFolderForUri(uri)
        if (isMediaProtected(uri, parentFolder)) {
            pendingAuthAction = action
        } else {
            action()
        }
    }

    val handleToggleFolderLock: (String) -> Unit = { path ->
        if (securityLockedFolders.contains(path)) {
            runWithFolderAuth(path) {
                viewModel.unlockFolder(path)
                Toast.makeText(context, context.getString(R.string.folder_unlocked_toast), Toast.LENGTH_SHORT).show()
            }
        } else {
            if (securityLockType == SecurityLockType.NONE) {
                Toast.makeText(context, context.getString(R.string.security_setup_required), Toast.LENGTH_LONG).show()
            } else {
                viewModel.toggleFolderLock(path)
                Toast.makeText(context, context.getString(R.string.folder_locked_toast), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val handleToggleMediaLock: (String) -> Unit = { uri ->
        if (securityLockedMedia.contains(uri)) {
            runWithMediaAuth(uri) {
                viewModel.unlockMedia(uri)
                Toast.makeText(context, context.getString(R.string.media_unlocked_toast), Toast.LENGTH_SHORT).show()
            }
        } else {
            if (securityLockType == SecurityLockType.NONE) {
                Toast.makeText(context, context.getString(R.string.security_setup_required), Toast.LENGTH_LONG).show()
            } else {
                viewModel.toggleMediaLock(uri)
                Toast.makeText(context, context.getString(R.string.media_locked_toast), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val handlePlayFolder: (String, Boolean) -> Unit = { path, isVideo ->
        runWithFolderAuth(path) {
            scope.launch {
                if (isVideo) {
                    val videosInFolder = viewModel.getVideosInFolder(path)
                    if (videosInFolder.isNotEmpty()) {
                        viewModel.playVideos(videosInFolder, 0)
                        val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(videosInFolder.first().uri.toString())
                        onVideoClick(encodedUri, -1L, "folder")
                    }
                } else {
                    val audiosInFolder = audioViewModel.getAudiosInFolder(path)
                    if (audiosInFolder.isNotEmpty()) {
                        audioViewModel.playAudios(audiosInFolder, 0)
                        val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(audiosInFolder.first().uri.toString())
                        onAudioClick(encodedUri, -1L, "folder")
                    }
                }
            }
        }
    }

    val isSearchingGlobal = searchQuery.isNotEmpty() && (searchScope == SearchScope.CURRENT_TYPE || searchScope == SearchScope.ALL_MEDIA)
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(padding), beyondViewportPageCount = preloadNeighborPages, userScrollEnabled = currentSelectedFolder == null && selectedPlaylistForDetails == null && !isSearchingGlobal && !isSelectionMode) { page ->
        val pageTab = activeTabs.getOrNull(page) ?: activeTabs.first()
        val isSearchingAllMedia = searchQuery.isNotEmpty() && (pageTab == MediaTab.VIDEOS || pageTab == MediaTab.AUDIOS) && searchScope == SearchScope.ALL_MEDIA
        val isSearchingTabMedia = searchQuery.isNotEmpty() && (pageTab == MediaTab.VIDEOS || pageTab == MediaTab.AUDIOS) && searchScope == SearchScope.CURRENT_TYPE
        val isFolderLevel = currentSelectedFolder == null && selectedPlaylistForDetails == null && !isSearchingAllMedia && !isSearchingTabMedia
        val pageContextKey = when {
            pageTab == MediaTab.VIDEOS && selectedVideoFolder != null -> "folder_video_$selectedVideoFolder"
            pageTab == MediaTab.VIDEOS -> "tab_videos"
            pageTab == MediaTab.AUDIOS && selectedAudioFolder != null -> "folder_audio_$selectedAudioFolder"
            pageTab == MediaTab.AUDIOS -> "tab_audios"
            pageTab == MediaTab.PLAYLISTS && selectedPlaylistForDetails != null -> "playlist_${selectedPlaylistForDetails.id}"
            pageTab == MediaTab.PLAYLISTS -> "tab_playlists"
            pageTab == MediaTab.HISTORY -> "tab_history"
            else -> "default"
        }
        val pageViewMode by remember(pageContextKey) {
            viewModel.getFolderViewMode(pageContextKey)
        }.collectAsStateWithLifecycle(initialValue = viewModel.defaultViewMode.value)

        val pageSortedFolders = remember(pageTab, sortedVideoFolders, sortedAudioFolders) {
            if (pageTab == MediaTab.VIDEOS) sortedVideoFolders else if (pageTab == MediaTab.AUDIOS) sortedAudioFolders else emptyList()
        }

        val scrollContextKey = remember(pageTab, currentSelectedFolder, selectedPlaylistForDetails, currentSortOrder) {
            "${pageTab.name}_${currentSelectedFolder ?: "root"}_${selectedPlaylistForDetails?.id ?: "none"}_${currentSortOrder.name}"
        }
        val listState = remember(scrollContextKey) { LazyListState() }
        val gridState = remember(scrollContextKey) { LazyGridState() }

        LaunchedEffect(currentSortOrder, currentSelectedFolder, selectedPlaylistForDetails, searchQuery, searchScope) {
            listState.scrollToItem(0)
            gridState.scrollToItem(0)
        }

        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = {
                if (pageTab == MediaTab.VIDEOS) viewModel.manualRefresh()
                else audioViewModel.manualRefresh()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            if (isFolderLevel && pageSortedFolders.isEmpty() && pageTab != MediaTab.PLAYLISTS && pageTab != MediaTab.HISTORY) {
                EmptyState(searchQuery.isNotEmpty(), true)
            } else if (isSearchingAllMedia && sortedAllMedia.isEmpty()) {
                EmptyState(searchQuery.isNotEmpty(), false)
            } else if (selectedPlaylistForDetails != null && sortedPlaylistItems.isEmpty()) {
                EmptyState(searchQuery.isNotEmpty(), false)
            } else if (!isFolderLevel && !isSearchingAllMedia && selectedPlaylistForDetails == null && (if (pageTab == MediaTab.VIDEOS) sortedVideos.isEmpty() else sortedAudios.isEmpty())) {
                EmptyState(searchQuery.isNotEmpty(), false)
            } else {
                if (pageViewMode == 1 || pageViewMode == 2) {
                    val cols = if (pageViewMode == 1) 2 else 1
                    val ratio = if (pageViewMode == 1) 1f else (16f / 9f)
                    LazyVerticalGrid(state = gridState, columns = GridCells.Fixed(cols), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isFolderLevel) {
                            if (showRecentHistoryOnHome && pageTab != MediaTab.PLAYLISTS && pageTab != MediaTab.HISTORY && recentPlayback.isNotEmpty()) {
                                item(span = { GridItemSpan(cols) }) {
                                    RecentPlaybackSection(
                                        recentPlayback = recentPlayback,
                                        onItemClick = { item, index ->
                                            runWithMediaAuth(item.uri) {
                                                viewModel.playHistory(recentPlayback, index, videos, audios, audiosByUri = audiosByUri)
                                                val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                                if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                            }
                                        },
                                        onViewAllClick = {
                                            onOpenHistory()
                                        }
                                    )
                                }
                            }
                            if (pageTab == MediaTab.HISTORY) {
                                if (sortedHistory.isEmpty()) {
                                    item(span = { GridItemSpan(cols) }) { EmptyState(searchQuery.isNotEmpty(), false) }
                                } else {
                                    items(sortedHistory, key = { it.uri }) { item ->
                                        HistoryGridCard(
                                            item = item,
                                            onClick = {
                                                runWithMediaAuth(item.uri) {
                                                    val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                                    viewModel.playHistory(sortedHistory, index, videos, audios, audiosByUri = audiosByUri)
                                                    val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                                    if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                                }
                                            },
                                            onDelete = { viewModel.deleteHistoryItem(item.uri) },
                                            onDeleteFromDevice = { onHistoryDeviceDelete(item) },
                                            onAddToPlaylist = { onMediaPendingPlaylist(item.uri, item.mediaType) },
                                            aspectRatio = ratio,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedKeys.contains(item.uri),
                                            onToggleSelect = { onToggleSelect(item.uri) }
                                        )
                                    }
                                }
                            } else if (pageTab == MediaTab.PLAYLISTS) {
                                if (sortedPlaylists.isNotEmpty()) {
                                    items(sortedPlaylists, key = { it.id }) { playlist ->
                                        PlaylistGridCard(
                                            playlist = playlist,
                                            onClick = { onOpenPlaylist(playlist) },
                                            onDelete = { playlistToDelete = playlist },
                                            aspectRatio = ratio,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedKeys.contains(playlist.id.toString()),
                                            onToggleSelect = { onToggleSelect(playlist.id.toString()) }
                                        )
                                    }
                                }
                            } else {
                                items(pageSortedFolders, key = { it.path }) { folderNode ->
                                    FolderGridItem(
                                        name = folderNode.name,
                                        path = folderNode.path,
                                        count = folderNode.videoCount,
                                        onClick = {
                                            runWithFolderAuth(folderNode.path) {
                                                if (folderNode.subFolders.isNotEmpty() && !folderNode.isFlattened) {
                                                    if (pageTab == MediaTab.VIDEOS) viewModel.navigateIntoFolder(folderNode.path)
                                                    else audioViewModel.navigateIntoFolder(folderNode.path)
                                                } else {
                                                    if (pageTab == MediaTab.VIDEOS) viewModel.setSelectedFolder(folderNode.path)
                                                    else audioViewModel.setSelectedFolder(folderNode.path)
                                                }
                                            }
                                        },
                                        onPlay = { handlePlayFolder(folderNode.path, pageTab == MediaTab.VIDEOS) },
                                        onExclude = { onFolderExclude(folderNode.path, pageTab == MediaTab.VIDEOS) },
                                        onAddToPlaylist = { onFolderPendingPlaylist(folderNode.path, if (pageTab == MediaTab.VIDEOS) "video" else "audio") },
                                        onDelete = { onFolderDeviceDelete(folderNode.path, pageTab == MediaTab.VIDEOS) },
                                        onRename = { newName -> onFolderRename(folderNode.path, newName, pageTab == MediaTab.VIDEOS) },
                                        isLocked = isFolderProtected(folderNode.path),
                                        onToggleLock = { handleToggleFolderLock(folderNode.path) },
                                        aspectRatio = ratio,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedKeys.contains(folderNode.path),
                                        onToggleSelect = { onToggleSelect(folderNode.path) }
                                    )
                                }
                            }
                        } else if (selectedPlaylistForDetails != null) {
                            val currentPlaylist = selectedPlaylistForDetails
                            items(sortedPlaylistItems, key = { it.id }) { item ->
                                PlaylistGridItem(
                                    item = item,
                                    videosByUri = videosByUri,
                                    audiosByUri = audiosByUri,
                                    isPlaying = if (item.mediaType == "video") isVideoPlaying && activeVideoUri == item.mediaUri else isAudioPlaying && activeAudioUri == item.mediaUri,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onPlayItem = { clickedItem ->
                                        runWithMediaAuth(clickedItem.mediaUri) {
                                            val index = sortedPlaylistItems.indexOfFirst { it.id == clickedItem.id }.coerceAtLeast(0)
                                            audioViewModel.playPlaylist(sortedPlaylistItems, index, videos, audios, videosByUri = videosByUri, audiosByUri = audiosByUri)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(clickedItem.mediaUri)
                                            if (clickedItem.mediaType == "video") onVideoClick(encodedUri, -1L, "playlist_${currentPlaylist.id}") else onAudioClick(encodedUri, -1L, "playlist_${currentPlaylist.id}")
                                        }
                                    },
                                    playlist = currentPlaylist,
                                    audioViewModel = audioViewModel,
                                    onVideoDelete = { video -> onVideoDeviceDelete(video) },
                                    onAudioDelete = { audio -> onAudioDeviceDelete(audio) },
                                    onVideoRename = onVideoRename,
                                    onAudioRename = onAudioRename,
                                    aspectRatio = ratio,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedKeys.contains(item.id.toString()),
                                    onToggleSelect = { onToggleSelect(item.id.toString()) }
                                )
                            }
                        } else if (isSearchingAllMedia) {
                            items(sortedAllMedia, key = { it.searchKey }) { item ->
                                when (item) {
                                    is SearchMediaItem.Video -> {
                                        val uriStr = item.uri.toString()
                                        val isLocked = isMediaProtected(uriStr, item.model.path.substringBeforeLast("/", ""))
                                        VideoGridItem(
                                            video = item.model,
                                            isPlaying = isVideoPlaying && activeVideoUri == uriStr,
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            onClick = { uri ->
                                                runWithMediaAuth(uriStr) {
                                                    val index = sortedAllMedia.indexOf(item).coerceAtLeast(0)
                                                    viewModel.playMixedMedia(sortedAllMedia, index)
                                                    onVideoClick(uri, -1L, "search")
                                                }
                                            },
                                            onDelete = { onVideoDeviceDelete(item.model) },
                                            onPlaylist = { onMediaPendingPlaylist(uriStr, "video") },
                                            onRename = { newName -> onVideoRename(item.model, newName) },
                                            isLocked = isLocked,
                                            onToggleLock = { handleToggleMediaLock(uriStr) },
                                            aspectRatio = ratio,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedKeys.contains(uriStr),
                                            onToggleSelect = { onToggleSelect(uriStr) }
                                        )
                                    }
                                    is SearchMediaItem.Audio -> {
                                        val uriStr = item.uri.toString()
                                        val isLocked = isMediaProtected(uriStr, item.model.path.substringBeforeLast("/", ""))
                                        AudioGridItem(
                                            audio = item.model,
                                            isPlaying = isAudioPlaying && activeAudioUri == uriStr,
                                            onClick = { uri ->
                                                runWithMediaAuth(uriStr) {
                                                    val index = sortedAllMedia.indexOf(item).coerceAtLeast(0)
                                                    audioViewModel.playMixedMedia(sortedAllMedia, index)
                                                    onAudioClick(uri, -1L, "search")
                                                }
                                            },
                                            onDelete = { onAudioDeviceDelete(item.model) },
                                            onPlaylist = { onMediaPendingPlaylist(uriStr, "audio") },
                                            onRename = { newName -> onAudioRename(item.model, newName) },
                                            isLocked = isLocked,
                                            onToggleLock = { handleToggleMediaLock(uriStr) },
                                            aspectRatio = ratio,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedKeys.contains(uriStr),
                                            onToggleSelect = { onToggleSelect(uriStr) }
                                        )
                                    }
                                }
                            }
                        } else if (pageTab == MediaTab.VIDEOS) {
                            items(sortedVideos, key = { it.id }) { video ->
                                val uriStr = video.uri.toString()
                                val isLocked = isMediaProtected(uriStr, video.path.substringBeforeLast("/", ""))
                                VideoGridItem(
                                    video = video,
                                    isPlaying = isVideoPlaying && activeVideoUri == uriStr,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = { uri ->
                                        runWithMediaAuth(uriStr) {
                                            val index = sortedVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                            viewModel.playVideos(sortedVideos, index)
                                            onVideoClick(uri, -1L, "folder")
                                        }
                                    },
                                    onDelete = { onVideoDeviceDelete(video) },
                                    onPlaylist = { onMediaPendingPlaylist(uriStr, "video") },
                                    onRename = { newName -> onVideoRename(video, newName) },
                                    isLocked = isLocked,
                                    onToggleLock = { handleToggleMediaLock(uriStr) },
                                    aspectRatio = ratio,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedKeys.contains(uriStr),
                                    onToggleSelect = { onToggleSelect(uriStr) }
                                )
                            }
                        } else {
                            items(sortedAudios, key = { it.id }) { audio ->
                                val uriStr = audio.uri.toString()
                                val isLocked = isMediaProtected(uriStr, audio.path.substringBeforeLast("/", ""))
                                AudioGridItem(
                                    audio = audio,
                                    isPlaying = isAudioPlaying && activeAudioUri == uriStr,
                                    onClick = { uri ->
                                        runWithMediaAuth(uriStr) {
                                            val index = sortedAudios.indexOfFirst { it.id == audio.id }.coerceAtLeast(0)
                                            audioViewModel.playAudios(sortedAudios, index)
                                            onAudioClick(uri, -1L, "folder")
                                        }
                                    },
                                    onDelete = { onAudioDeviceDelete(audio) },
                                    onPlaylist = { onMediaPendingPlaylist(uriStr, "audio") },
                                    onRename = { newName -> onAudioRename(audio, newName) },
                                    isLocked = isLocked,
                                    onToggleLock = { handleToggleMediaLock(uriStr) },
                                    aspectRatio = ratio,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedKeys.contains(uriStr),
                                    onToggleSelect = { onToggleSelect(uriStr) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (showRecentHistoryOnHome && isFolderLevel && pageTab != MediaTab.PLAYLISTS && pageTab != MediaTab.HISTORY && recentPlayback.isNotEmpty()) {
                            item {
                                RecentPlaybackSection(
                                    recentPlayback = recentPlayback,
                                    onItemClick = { item, index ->
                                        runWithMediaAuth(item.uri) {
                                            viewModel.playHistory(recentPlayback, index, videos, audios, audiosByUri = audiosByUri)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                            if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                        }
                                    },
                                    onViewAllClick = {
                                        onOpenHistory()
                                    }
                                )
                            }
                        }
                        if (isFolderLevel) {
                            if (pageTab == MediaTab.HISTORY) {
                                if (sortedHistory.isEmpty()) {
                                    item { EmptyState(searchQuery.isNotEmpty(), false) }
                                } else {
                                    items(sortedHistory, key = { it.uri }) { item ->
                                        HistoryItem(
                                            item = item,
                                            onClick = {
                                                runWithMediaAuth(item.uri) {
                                                    val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                                    viewModel.playHistory(sortedHistory, index, videos, audios, audiosByUri = audiosByUri)
                                                    val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                                    if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                                }
                                            },
                                            onDelete = {
                                                viewModel.deleteHistoryItem(item.uri)
                                            },
                                            onDeleteFromDevice = { onHistoryDeviceDelete(item) },
                                            onAddToPlaylist = { onMediaPendingPlaylist(item.uri, item.mediaType) },
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedKeys.contains(item.uri),
                                            onToggleSelect = { onToggleSelect(item.uri) }
                                        )
                                    }
                                }
                            } else if (pageTab == MediaTab.PLAYLISTS) {
                                if (sortedPlaylists.isEmpty()) { item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_playlists_yet), color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                                else {
                                    items(sortedPlaylists, key = { it.id }) { playlist ->
                                        PlaylistListItem(
                                            playlist = playlist,
                                            onClick = { onOpenPlaylist(playlist) },
                                            onDelete = { playlistToDelete = playlist },
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedKeys.contains(playlist.id.toString()),
                                            onToggleSelect = { onToggleSelect(playlist.id.toString()) }
                                        )
                                    }
                                }
                            } else {
                                items(pageSortedFolders, key = { it.path }) { folderNode ->
                                    FolderListItem(
                                        name = folderNode.name,
                                        path = folderNode.path,
                                        count = folderNode.videoCount,
                                        onClick = {
                                            runWithFolderAuth(folderNode.path) {
                                                if (folderNode.subFolders.isNotEmpty() && !folderNode.isFlattened) {
                                                    if (pageTab == MediaTab.VIDEOS) viewModel.navigateIntoFolder(folderNode.path)
                                                    else audioViewModel.navigateIntoFolder(folderNode.path)
                                                } else {
                                                    if (pageTab == MediaTab.VIDEOS) viewModel.setSelectedFolder(folderNode.path)
                                                    else audioViewModel.setSelectedFolder(folderNode.path)
                                                }
                                            }
                                        },
                                        onPlay = { handlePlayFolder(folderNode.path, pageTab == MediaTab.VIDEOS) },
                                        onDelete = { onFolderDeviceDelete(folderNode.path, pageTab == MediaTab.VIDEOS) },
                                        onExclude = { onFolderExclude(folderNode.path, pageTab == MediaTab.VIDEOS) },
                                        onAddToPlaylist = { onFolderPendingPlaylist(folderNode.path, if (pageTab == MediaTab.VIDEOS) "video" else "audio") },
                                        onRename = { newName -> onFolderRename(folderNode.path, newName, pageTab == MediaTab.VIDEOS) },
                                        isLocked = isFolderProtected(folderNode.path),
                                        onToggleLock = { handleToggleFolderLock(folderNode.path) },
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedKeys.contains(folderNode.path),
                                        onToggleSelect = { onToggleSelect(folderNode.path) }
                                    )
                                }
                            }
                        } else if (selectedPlaylistForDetails != null) {
                            val currentPlaylist = selectedPlaylistForDetails
                            items(sortedPlaylistItems, key = { it.id }) { item ->
                                MediaListItemInPlaylist(
                                    item = item,
                                    videosByUri = videosByUri,
                                    audiosByUri = audiosByUri,
                                    audioViewModel = audioViewModel,
                                    isPlaying = if (item.mediaType == "video") isVideoPlaying && activeVideoUri == item.mediaUri else isAudioPlaying && activeAudioUri == item.mediaUri,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onPlayItem = { clickedItem ->
                                        runWithMediaAuth(clickedItem.mediaUri) {
                                            val index = sortedPlaylistItems.indexOfFirst { it.id == clickedItem.id }.coerceAtLeast(0)
                                            audioViewModel.playPlaylist(sortedPlaylistItems, index, videos, audios, videosByUri = videosByUri, audiosByUri = audiosByUri)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(clickedItem.mediaUri)
                                            if (clickedItem.mediaType == "video") onVideoClick(encodedUri, -1L, "playlist_${currentPlaylist.id}") else onAudioClick(encodedUri, -1L, "playlist_${currentPlaylist.id}")
                                        }
                                    },
                                    playlist = currentPlaylist,
                                    onVideoDelete = { video -> onVideoDeviceDelete(video) },
                                    onAudioDelete = { audio -> onAudioDeviceDelete(audio) },
                                    onVideoRename = onVideoRename,
                                    onAudioRename = onAudioRename,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedKeys.contains(item.id.toString()),
                                    onToggleSelect = { onToggleSelect(item.id.toString()) }
                                )
                            }
                        } else if (isSearchingAllMedia) {
                            items(sortedAllMedia, key = { it.searchKey }) { item ->
                                when (item) {
                                    is SearchMediaItem.Video -> {
                                        val uriStr = item.uri.toString()
                                        val isLocked = isMediaProtected(uriStr, item.model.path.substringBeforeLast("/", ""))
                                        VideoListItem(
                                            video = item.model,
                                            isPlaying = isVideoPlaying && activeVideoUri == uriStr,
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            onClick = {
                                                runWithMediaAuth(uriStr) {
                                                    val index = sortedAllMedia.indexOf(item).coerceAtLeast(0)
                                                    viewModel.playMixedMedia(sortedAllMedia, index)
                                                    val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(uriStr)
                                                    onVideoClick(encodedUri, -1L, "search")
                                                }
                                            },
                                            onDeleteClick = { onVideoDeviceDelete(item.model) },
                                            onPlaylistClick = { onMediaPendingPlaylist(uriStr, "video") },
                                            onRename = { newName -> onVideoRename(item.model, newName) },
                                            isLocked = isLocked,
                                            onToggleLock = { handleToggleMediaLock(uriStr) },
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedKeys.contains(uriStr),
                                            onToggleSelect = { onToggleSelect(uriStr) }
                                        )
                                    }
                                    is SearchMediaItem.Audio -> {
                                        val uriStr = item.uri.toString()
                                        val isLocked = isMediaProtected(uriStr, item.model.path.substringBeforeLast("/", ""))
                                        AudioListItem(
                                            audio = item.model,
                                            isPlaying = isAudioPlaying && activeAudioUri == uriStr,
                                            onClick = {
                                                runWithMediaAuth(uriStr) {
                                                    val index = sortedAllMedia.indexOf(item).coerceAtLeast(0)
                                                    audioViewModel.playMixedMedia(sortedAllMedia, index)
                                                    val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(uriStr)
                                                    onAudioClick(encodedUri, -1L, "search")
                                                }
                                            },
                                            onDeleteClick = { onAudioDeviceDelete(item.model) },
                                            onPlaylistClick = { onMediaPendingPlaylist(uriStr, "audio") },
                                            onRename = { newName -> onAudioRename(item.model, newName) },
                                            isLocked = isLocked,
                                            onToggleLock = { handleToggleMediaLock(uriStr) },
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedKeys.contains(uriStr),
                                            onToggleSelect = { onToggleSelect(uriStr) }
                                        )
                                    }
                                }
                            }
                        } else if (pageTab == MediaTab.VIDEOS) {
                            items(sortedVideos, key = { it.id }) { video ->
                                val uriStr = video.uri.toString()
                                val isLocked = isMediaProtected(uriStr, video.path.substringBeforeLast("/", ""))
                                VideoListItem(
                                    video = video,
                                    isPlaying = isVideoPlaying && activeVideoUri == uriStr,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = {
                                        runWithMediaAuth(uriStr) {
                                            val index = sortedVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                            viewModel.playVideos(sortedVideos, index)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(uriStr)
                                            onVideoClick(encodedUri, -1L, "folder")
                                        }
                                    },
                                    onDeleteClick = { onVideoDeviceDelete(video) },
                                    onPlaylistClick = { onMediaPendingPlaylist(uriStr, "video") },
                                    onRename = { newName -> onVideoRename(video, newName) },
                                    isLocked = isLocked,
                                    onToggleLock = { handleToggleMediaLock(uriStr) },
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedKeys.contains(uriStr),
                                    onToggleSelect = { onToggleSelect(uriStr) }
                                )
                            }
                        } else {
                            items(sortedAudios, key = { it.id }) { audio ->
                                val uriStr = audio.uri.toString()
                                val isLocked = isMediaProtected(uriStr, audio.path.substringBeforeLast("/", ""))
                                AudioListItem(
                                    audio = audio,
                                    isPlaying = isAudioPlaying && activeAudioUri == uriStr,
                                    onClick = {
                                        runWithMediaAuth(uriStr) {
                                            val index = sortedAudios.indexOfFirst { it.id == audio.id }.coerceAtLeast(0)
                                            audioViewModel.playAudios(sortedAudios, index)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(uriStr)
                                            onAudioClick(encodedUri, -1L, "folder")
                                        }
                                    },
                                    onDeleteClick = { onAudioDeviceDelete(audio) },
                                    onPlaylistClick = { onMediaPendingPlaylist(uriStr, "audio") },
                                    onRename = { newName -> onAudioRename(audio, newName) },
                                    isLocked = isLocked,
                                    onToggleLock = { handleToggleMediaLock(uriStr) },
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedKeys.contains(uriStr),
                                    onToggleSelect = { onToggleSelect(uriStr) }
                                )
                            }
                        }
                    }
                }
        }
    }
    }
}
