package com.mohamedaminelouati.oplayer.ui.feature.library

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mohamedaminelouati.oplayer.R
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedaminelouati.oplayer.data.VideoModel
import com.mohamedaminelouati.oplayer.data.AudioModel
import com.mohamedaminelouati.oplayer.data.Playlist
import com.mohamedaminelouati.oplayer.data.PlaylistItem
import com.mohamedaminelouati.oplayer.viewmodel.AudioViewModel
import com.mohamedaminelouati.oplayer.viewmodel.VideoViewModel
import com.mohamedaminelouati.oplayer.ui.common.ModernLoadingDialog
import com.mohamedaminelouati.oplayer.data.MediaSortOrder
import com.mohamedaminelouati.oplayer.data.RecentPlayback
import com.mohamedaminelouati.oplayer.data.SearchScope
import com.mohamedaminelouati.oplayer.data.SearchMediaItem
import com.mohamedaminelouati.oplayer.data.SecurityLockType
import com.mohamedaminelouati.oplayer.ui.feature.library.components.*
import com.mohamedaminelouati.oplayer.ui.feature.security.SecurityAuthDialog
import com.mohamedaminelouati.oplayer.ui.theme.LocalPlaybackProgress
import com.mohamedaminelouati.oplayer.util.DetailedMediaInfo
import com.mohamedaminelouati.oplayer.util.extractDetailedMediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MediaTab { VIDEOS, AUDIOS, PLAYLISTS, HISTORY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: VideoViewModel,
    audioViewModel: AudioViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onVideoClick: (String, Long, String?) -> Unit,
    onAudioClick: (String, Long, String?) -> Unit,
    onSettingsClick: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    isFocused: Boolean = true,
    initialTab: MediaTab? = null,
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab ?: MediaTab.VIDEOS) }

    val showRecentHistoryOnHome by viewModel.showRecentHistoryOnHome.collectAsStateWithLifecycle()
    val showHistoryTab by viewModel.showHistoryTab.collectAsStateWithLifecycle()
    var showFullHistoryScreen by remember { mutableStateOf(false) }
    var previousTabBeforeHistory by remember { mutableStateOf<MediaTab?>(null) }

    val isHistoryVisible = showHistoryTab || showFullHistoryScreen
    val fullHistory by remember(isHistoryVisible) {
        if (isHistoryVisible) viewModel.fullHistory else flowOf(emptyList<RecentPlayback>())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val activeTabs = remember(showHistoryTab) {
        if (showHistoryTab) MediaTab.entries else MediaTab.entries.filter { it != MediaTab.HISTORY }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { activeTabs.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in activeTabs.indices) {
            val newTab = activeTabs[pagerState.currentPage]
            if (newTab != selectedTab && newTab != MediaTab.HISTORY) {
                previousTabBeforeHistory = null
            }
            selectedTab = newTab
        }
    }

    LaunchedEffect(selectedTab, activeTabs) {
        val targetIndex = activeTabs.indexOf(selectedTab)
        if (targetIndex != -1 && targetIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoadingVideos by viewModel.isLoading.collectAsStateWithLifecycle()
    val videoFolders by viewModel.currentVisibleFolders.collectAsStateWithLifecycle()
    val videoNavPath by viewModel.currentNavPath.collectAsStateWithLifecycle()
    val videoFolderTree by viewModel.folderTree.collectAsStateWithLifecycle()
    val selectedVideoFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val videosInFolder by viewModel.videosInSelectedFolder.collectAsStateWithLifecycle()
    val recentPlayback by viewModel.recentPlayback.collectAsStateWithLifecycle()
    val playbackProgressMap by viewModel.playbackProgressMap.collectAsStateWithLifecycle()

    val audios by audioViewModel.audios.collectAsStateWithLifecycle()
    val isLoadingAudios by audioViewModel.isLoading.collectAsStateWithLifecycle()
    val audioFolders by audioViewModel.currentVisibleFolders.collectAsStateWithLifecycle()
    val audioNavPath by audioViewModel.currentNavPath.collectAsStateWithLifecycle()
    val audioFolderTree by audioViewModel.folderTree.collectAsStateWithLifecycle()
    val selectedAudioFolder by audioViewModel.selectedFolder.collectAsStateWithLifecycle()
    val audiosInFolder by audioViewModel.audiosInSelectedFolder.collectAsStateWithLifecycle()
    val playlists by audioViewModel.playlists.collectAsStateWithLifecycle()

    val activeVideoUri by viewModel.activeVideoUri.collectAsStateWithLifecycle()
    val isVideoPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val activeAudioUri by audioViewModel.activeAudioUri.collectAsStateWithLifecycle()
    val isAudioPlaying by audioViewModel.isPlaying.collectAsStateWithLifecycle()
    
    val isLoading = if (selectedTab == MediaTab.VIDEOS) isLoadingVideos else isLoadingAudios

    var searchQuery by remember { mutableStateOf("") }
    val defaultSearchScope by viewModel.defaultSearchScope.collectAsStateWithLifecycle()
    var searchScope by rememberSaveable(defaultSearchScope) { mutableStateOf(defaultSearchScope) }
    var isProcessing by remember { mutableStateOf(false) }
    
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var mediaPendingPlaylist by remember { mutableStateOf<Pair<String, String>?>(null) }
    var folderPendingPlaylist by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedPlaylistForDetails by remember { mutableStateOf<Playlist?>(null) }
    val playlistItems by (if (selectedPlaylistForDetails != null) audioViewModel.getPlaylistItems(selectedPlaylistForDetails!!.id) else flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val currentSelectedFolder = when (selectedTab) {
        MediaTab.VIDEOS -> selectedVideoFolder
        MediaTab.AUDIOS -> selectedAudioFolder
        else -> null
    }
    val currentFolders = when (selectedTab) {
        MediaTab.VIDEOS -> videoFolders
        MediaTab.AUDIOS -> audioFolders
        else -> emptyList()
    }

    val currentContextKey = when {
        showFullHistoryScreen -> "tab_history"
        selectedTab == MediaTab.VIDEOS && selectedVideoFolder != null -> "folder_video_$selectedVideoFolder"
        selectedTab == MediaTab.VIDEOS -> "tab_videos"
        selectedTab == MediaTab.AUDIOS && selectedAudioFolder != null -> "folder_audio_$selectedAudioFolder"
        selectedTab == MediaTab.AUDIOS -> "tab_audios"
        selectedTab == MediaTab.PLAYLISTS && selectedPlaylistForDetails != null -> "playlist_${selectedPlaylistForDetails?.id}"
        selectedTab == MediaTab.PLAYLISTS -> "tab_playlists"
        selectedTab == MediaTab.HISTORY -> "tab_history"
        else -> "default"
    }

    val currentTabViewMode by remember(currentContextKey) {
        viewModel.getFolderViewMode(currentContextKey)
    }.collectAsStateWithLifecycle(initialValue = viewModel.defaultViewMode.value)

    val currentSortOrderName by remember(currentContextKey) {
        viewModel.getFolderSortOrder(currentContextKey)
    }.collectAsStateWithLifecycle(initialValue = MediaSortOrder.DATE_DESC.name)

    val currentSortOrder = remember(currentSortOrderName) {
        MediaSortOrder.values().firstOrNull { it.name == currentSortOrderName } ?: MediaSortOrder.DATE_DESC
    }

    val isCurrentFolderOpen = remember(selectedTab, selectedVideoFolder, selectedAudioFolder, selectedPlaylistForDetails) {
        (selectedTab == MediaTab.VIDEOS && selectedVideoFolder != null) || 
        (selectedTab == MediaTab.AUDIOS && selectedAudioFolder != null) ||
        (selectedTab == MediaTab.PLAYLISTS && selectedPlaylistForDetails != null)
    }

    val isNavPathOpen = remember(selectedTab, videoNavPath, audioNavPath) {
        (selectedTab == MediaTab.VIDEOS && !videoNavPath.isNullOrEmpty()) ||
        (selectedTab == MediaTab.AUDIOS && !audioNavPath.isNullOrEmpty())
    }

    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedKeys.isNotEmpty()

    LaunchedEffect(isSelectionMode) {
        onSelectionModeChange(isSelectionMode)
    }

    DisposableEffect(Unit) {
        onDispose {
            onSelectionModeChange(false)
        }
    }

    val onToggleSelect: (String) -> Unit = { key ->
        selectedKeys = if (selectedKeys.contains(key)) selectedKeys - key else selectedKeys + key
    }

    LaunchedEffect(selectedTab, currentSelectedFolder, selectedPlaylistForDetails, showFullHistoryScreen) {
        selectedKeys = emptySet()
    }

    var showExitConfirmDialog by remember { mutableStateOf(false) }
    val activity = context as? Activity
    BackHandler(enabled = isFocused) {
        if (selectedKeys.isNotEmpty()) {
            selectedKeys = emptySet()
        } else if (showFullHistoryScreen) {
            showFullHistoryScreen = false
        } else if (previousTabBeforeHistory != null && selectedTab == MediaTab.HISTORY) {
            selectedTab = previousTabBeforeHistory ?: MediaTab.VIDEOS
            previousTabBeforeHistory = null
        } else if (isCurrentFolderOpen) {
            if (selectedTab == MediaTab.VIDEOS) viewModel.setSelectedFolder(null)
            else if (selectedTab == MediaTab.AUDIOS) audioViewModel.setSelectedFolder(null)
            else selectedPlaylistForDetails = null
        } else if (isNavPathOpen) {
            if (selectedTab == MediaTab.VIDEOS) viewModel.navigateUp(videoFolderTree)
            else if (selectedTab == MediaTab.AUDIOS) audioViewModel.navigateUp(audioFolderTree)
        } else {
            showExitConfirmDialog = true
        }
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text(text = stringResource(R.string.dialog_exit_title)) },
            text = { Text(text = stringResource(R.string.dialog_exit_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        activity?.finish()
                    }
                ) {
                    Text(text = stringResource(R.string.action_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val sortedVideoFolders = remember(videoFolders, searchQuery, searchScope, selectedVideoFolder, currentSortOrder) {
        if (selectedVideoFolder != null || (searchQuery.isNotEmpty() && searchScope != SearchScope.CURRENT_FOLDER)) emptyList()
        else {
            val baseList = if (searchQuery.isEmpty()) videoFolders
            else videoFolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
            when (currentSortOrder) {
                MediaSortOrder.NAME_ASC -> baseList.sortedBy { it.name.lowercase() }
                MediaSortOrder.NAME_DESC -> baseList.sortedByDescending { it.name.lowercase() }
                MediaSortOrder.SIZE_DESC -> baseList.sortedByDescending { it.videoCount }
                MediaSortOrder.SIZE_ASC -> baseList.sortedBy { it.videoCount }
                MediaSortOrder.DURATION_DESC -> baseList.sortedByDescending { it.videoCount }
                MediaSortOrder.DURATION_ASC -> baseList.sortedBy { it.videoCount }
                MediaSortOrder.DATE_DESC -> baseList
                MediaSortOrder.DATE_ASC -> baseList.reversed()
            }
        }
    }

    val sortedAudioFolders = remember(audioFolders, searchQuery, searchScope, selectedAudioFolder, currentSortOrder) {
        if (selectedAudioFolder != null || (searchQuery.isNotEmpty() && searchScope != SearchScope.CURRENT_FOLDER)) emptyList()
        else {
            val baseList = if (searchQuery.isEmpty()) audioFolders
            else audioFolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
            when (currentSortOrder) {
                MediaSortOrder.NAME_ASC -> baseList.sortedBy { it.name.lowercase() }
                MediaSortOrder.NAME_DESC -> baseList.sortedByDescending { it.name.lowercase() }
                MediaSortOrder.SIZE_DESC -> baseList.sortedByDescending { it.videoCount }
                MediaSortOrder.SIZE_ASC -> baseList.sortedBy { it.videoCount }
                MediaSortOrder.DURATION_DESC -> baseList.sortedByDescending { it.videoCount }
                MediaSortOrder.DURATION_ASC -> baseList.sortedBy { it.videoCount }
                MediaSortOrder.DATE_DESC -> baseList
                MediaSortOrder.DATE_ASC -> baseList.reversed()
            }
        }
    }

    val sortedVideos = remember(videosInFolder, videos, searchQuery, searchScope, selectedVideoFolder, currentSortOrder) {
        val isSearchingAllVideos = searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE
        if (selectedVideoFolder == null && !isSearchingAllVideos) emptyList()
        else {
            val sourceList = if (isSearchingAllVideos) videos else videosInFolder
            val list = if (searchQuery.isEmpty()) sourceList
            else sourceList.filter { it.name.contains(searchQuery, ignoreCase = true) }
            when (currentSortOrder) {
                MediaSortOrder.DATE_DESC -> list.sortedByDescending { it.id }
                MediaSortOrder.DATE_ASC -> list.sortedBy { it.id }
                MediaSortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                MediaSortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                MediaSortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
                MediaSortOrder.SIZE_ASC -> list.sortedBy { it.size }
                MediaSortOrder.DURATION_DESC -> list.sortedByDescending { it.duration }
                MediaSortOrder.DURATION_ASC -> list.sortedBy { it.duration }
            }
        }
    }
    
    val sortedAudios = remember(audiosInFolder, audios, searchQuery, searchScope, selectedAudioFolder, currentSortOrder) {
        val isSearchingAllAudios = searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE
        if (selectedAudioFolder == null && !isSearchingAllAudios) emptyList()
        else {
            val sourceList = if (isSearchingAllAudios) audios else audiosInFolder
            val list = if (searchQuery.isEmpty()) sourceList
            else sourceList.filter { it.name.contains(searchQuery, ignoreCase = true) }
            when (currentSortOrder) {
                MediaSortOrder.DATE_DESC -> list.sortedByDescending { it.id }
                MediaSortOrder.DATE_ASC -> list.sortedBy { it.id }
                MediaSortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                MediaSortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                MediaSortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
                MediaSortOrder.SIZE_ASC -> list.sortedBy { it.size }
                MediaSortOrder.DURATION_DESC -> list.sortedByDescending { it.duration }
                MediaSortOrder.DURATION_ASC -> list.sortedBy { it.duration }
            }
        }
    }

    val sortedAllMedia = remember(videos, audios, searchQuery, searchScope, currentSortOrder) {
        if (searchQuery.isEmpty() || searchScope != SearchScope.ALL_MEDIA) emptyList<SearchMediaItem>()
        else {
            val matchingVideos = videos.filter { it.name.contains(searchQuery, ignoreCase = true) }.map { SearchMediaItem.Video(it) }
            val matchingAudios = audios.filter { it.name.contains(searchQuery, ignoreCase = true) }.map { SearchMediaItem.Audio(it) }
            val combined: List<SearchMediaItem> = matchingVideos + matchingAudios
            when (currentSortOrder) {
                MediaSortOrder.NAME_ASC -> combined.sortedBy { it.name.lowercase() }
                MediaSortOrder.NAME_DESC -> combined.sortedByDescending { it.name.lowercase() }
                MediaSortOrder.DATE_DESC -> combined.sortedByDescending { it.id }
                MediaSortOrder.DATE_ASC -> combined.sortedBy { it.id }
                MediaSortOrder.SIZE_DESC -> combined.sortedByDescending { it.size }
                MediaSortOrder.SIZE_ASC -> combined.sortedBy { it.size }
                MediaSortOrder.DURATION_DESC -> combined.sortedByDescending { it.duration }
                MediaSortOrder.DURATION_ASC -> combined.sortedBy { it.duration }
            }
        }
    }

    val sortedPlaylists = remember(playlists, searchQuery, currentSortOrder) {
        val list = if (searchQuery.isEmpty()) playlists
        else playlists.filter { it.name.contains(searchQuery, ignoreCase = true) }
        when (currentSortOrder) {
            MediaSortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            MediaSortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            MediaSortOrder.DATE_ASC -> list.sortedBy { it.createdAt }
            MediaSortOrder.DATE_DESC, MediaSortOrder.SIZE_DESC, MediaSortOrder.SIZE_ASC, MediaSortOrder.DURATION_DESC, MediaSortOrder.DURATION_ASC -> list.sortedByDescending { it.createdAt }
        }
    }

    val sortedHistory = remember(fullHistory, searchQuery, currentSortOrder) {
        val list = if (searchQuery.isEmpty()) fullHistory
        else fullHistory.filter { it.name.contains(searchQuery, ignoreCase = true) }
        when (currentSortOrder) {
            MediaSortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            MediaSortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            MediaSortOrder.DATE_ASC -> list.sortedBy { it.lastPlayed }
            MediaSortOrder.DATE_DESC -> list.sortedByDescending { it.lastPlayed }
            MediaSortOrder.DURATION_DESC, MediaSortOrder.SIZE_DESC -> list.sortedByDescending { it.duration }
            MediaSortOrder.DURATION_ASC, MediaSortOrder.SIZE_ASC -> list.sortedBy { it.duration }
        }
    }

    val videosByUri by viewModel.videosByUri.collectAsStateWithLifecycle()
    val audiosByUri by audioViewModel.audiosByUri.collectAsStateWithLifecycle()

    val sortedPlaylistItems = remember(playlistItems, searchQuery, currentSortOrder, videosByUri, audiosByUri) {
        fun getItemName(item: PlaylistItem): String {
            return if (item.mediaType == "video") {
                videosByUri[item.mediaUri]?.name ?: ""
            } else {
                audiosByUri[item.mediaUri]?.name ?: ""
            }
        }
        val validItems = playlistItems.filter { item ->
            if (item.mediaType == "video") videosByUri.containsKey(item.mediaUri)
            else audiosByUri.containsKey(item.mediaUri)
        }
        val list = if (searchQuery.isEmpty()) validItems
        else validItems.filter { getItemName(it).contains(searchQuery, ignoreCase = true) }
        when (currentSortOrder) {
            MediaSortOrder.NAME_ASC -> list.sortedBy { getItemName(it).lowercase() }
            MediaSortOrder.NAME_DESC -> list.sortedByDescending { getItemName(it).lowercase() }
            MediaSortOrder.DATE_ASC -> list.sortedBy { it.addedAt }
            MediaSortOrder.DATE_DESC, MediaSortOrder.SIZE_DESC, MediaSortOrder.SIZE_ASC, MediaSortOrder.DURATION_DESC, MediaSortOrder.DURATION_ASC -> list.sortedByDescending { it.addedAt }
        }
    }

    val visibleItemKeys: List<String> = remember(
        showFullHistoryScreen,
        selectedPlaylistForDetails,
        sortedPlaylistItems,
        searchQuery,
        searchScope,
        selectedTab,
        sortedAllMedia,
        selectedVideoFolder,
        selectedAudioFolder,
        sortedVideos,
        sortedAudios,
        sortedVideoFolders,
        sortedAudioFolders,
        sortedPlaylists,
        sortedHistory
    ) {
        when {
            showFullHistoryScreen -> sortedHistory.map { it.uri }
            selectedPlaylistForDetails != null -> sortedPlaylistItems.map { it.id.toString() }
            searchQuery.isNotEmpty() && searchScope == SearchScope.ALL_MEDIA && (selectedTab == MediaTab.VIDEOS || selectedTab == MediaTab.AUDIOS) -> sortedAllMedia.map { it.uri.toString() }
            selectedTab == MediaTab.VIDEOS -> {
                if (selectedVideoFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) {
                    sortedVideos.map { it.uri.toString() }
                } else {
                    sortedVideoFolders.map { it.path }
                }
            }
            selectedTab == MediaTab.AUDIOS -> {
                if (selectedAudioFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) {
                    sortedAudios.map { it.uri.toString() }
                } else {
                    sortedAudioFolders.map { it.path }
                }
            }
            selectedTab == MediaTab.PLAYLISTS -> sortedPlaylists.map { it.id.toString() }
            selectedTab == MediaTab.HISTORY -> sortedHistory.map { it.uri }
            else -> emptyList()
        }
    }

    val securityLockType by viewModel.securityLockType.collectAsStateWithLifecycle()
    val securityLockedFolders by viewModel.securityLockedFolders.collectAsStateWithLifecycle()
    val securityLockedMedia by viewModel.securityLockedMedia.collectAsStateWithLifecycle()

    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var renameMediaTarget by remember { mutableStateOf<Triple<Uri, String, String>?>(null) }
    var renameFolderTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var detailedInfoTarget by remember { mutableStateOf<DetailedMediaInfo?>(null) }
    var folderInfoTarget by remember { mutableStateOf<Triple<String, String, Int>?>(null) }

    var batchPendingPlaylist by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var showBatchDeletePlaylistConfirm by remember { mutableStateOf(false) }

    var pendingUrisToDelete by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                isProcessing = true
                val deletedUris = pendingUrisToDelete
                deletedUris.forEach { uri ->
                    viewModel.deleteHistoryItem(uri.toString())
                }
                viewModel.stopIfPlaying(deletedUris)
                audioViewModel.stopIfPlaying(deletedUris)
                viewModel.onVideosDeleted(deletedUris)
                audioViewModel.onAudiosDeleted(deletedUris)
                pendingUrisToDelete = emptyList()
                isProcessing = false
            }
        } else {
            pendingUrisToDelete = emptyList()
            isProcessing = false
            Toast.makeText(context, context.getString(R.string.delete_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    val isFolderLevel = remember(selectedTab, selectedVideoFolder, selectedAudioFolder, searchQuery, searchScope, showFullHistoryScreen, selectedPlaylistForDetails) {
        !showFullHistoryScreen && selectedPlaylistForDetails == null && (
            (selectedTab == MediaTab.VIDEOS && selectedVideoFolder == null && (searchQuery.isEmpty() || searchScope == SearchScope.CURRENT_FOLDER)) ||
            (selectedTab == MediaTab.AUDIOS && selectedAudioFolder == null && (searchQuery.isEmpty() || searchScope == SearchScope.CURRENT_FOLDER))
        )
    }

    val isSelectionLocked = remember(selectedKeys, isFolderLevel, securityLockedFolders, securityLockedMedia) {
        if (selectedKeys.isEmpty()) false
        else if (isFolderLevel) {
            selectedKeys.all { securityLockedFolders.contains(it) }
        } else {
            selectedKeys.all { securityLockedMedia.contains(it) }
        }
    }

    val isHistoryContext = showFullHistoryScreen || selectedTab == MediaTab.HISTORY
    val isPlaylistTopLevel = selectedTab == MediaTab.PLAYLISTS && selectedPlaylistForDetails == null

    val canBatchPlay = selectedKeys.isNotEmpty() && !isPlaylistTopLevel
    val canBatchAddToPlaylist = selectedKeys.isNotEmpty() && !isPlaylistTopLevel
    val canBatchShare = selectedKeys.isNotEmpty() && !isPlaylistTopLevel
    val canBatchRename = selectedKeys.size == 1 && !isPlaylistTopLevel && !isHistoryContext
    val canBatchInformation = selectedKeys.size == 1 && !isPlaylistTopLevel
    val canBatchDelete = selectedKeys.isNotEmpty()
    val canShowLock = (selectedTab == MediaTab.VIDEOS || selectedTab == MediaTab.AUDIOS) && selectedPlaylistForDetails == null && !showFullHistoryScreen

    val handleBatchPlay: () -> Unit = {
        when {
            showFullHistoryScreen || selectedTab == MediaTab.HISTORY -> {
                val list = sortedHistory.filter { selectedKeys.contains(it.uri) }
                if (list.isNotEmpty()) {
                    viewModel.playHistory(list, 0, videos, audios, audiosByUri = audiosByUri)
                    val first = list.first()
                    val encodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeEncodeUri(first.uri)
                    if (first.mediaType == "video") onVideoClick(encodedUri, first.position, "history") else onAudioClick(encodedUri, first.position, "history")
                    selectedKeys = emptySet()
                }
            }
            selectedPlaylistForDetails != null -> {
                val list = sortedPlaylistItems.filter { selectedKeys.contains(it.id.toString()) }
                if (list.isNotEmpty()) {
                    audioViewModel.playPlaylist(list, 0, videos, audios, videosByUri = videosByUri, audiosByUri = audiosByUri)
                    val first = list.first()
                    val encodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeEncodeUri(first.mediaUri)
                    if (first.mediaType == "video") onVideoClick(encodedUri, -1L, "playlist_${selectedPlaylistForDetails!!.id}") else onAudioClick(encodedUri, -1L, "playlist_${selectedPlaylistForDetails!!.id}")
                    selectedKeys = emptySet()
                }
            }
            searchQuery.isNotEmpty() && searchScope == SearchScope.ALL_MEDIA && (selectedTab == MediaTab.VIDEOS || selectedTab == MediaTab.AUDIOS) -> {
                val list = sortedAllMedia.filter { selectedKeys.contains(it.uri.toString()) }
                if (list.isNotEmpty()) {
                    viewModel.playMixedMedia(list, 0)
                    val first = list.first()
                    val encodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeEncodeUri(first.uri.toString())
                    if (first.isVideo) onVideoClick(encodedUri, -1L, "search") else onAudioClick(encodedUri, -1L, "search")
                    selectedKeys = emptySet()
                }
            }
            selectedTab == MediaTab.VIDEOS && (selectedVideoFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) -> {
                val list = sortedVideos.filter { selectedKeys.contains(it.uri.toString()) }
                if (list.isNotEmpty()) {
                    viewModel.playVideos(list, 0)
                    val first = list.first()
                    val encodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeEncodeUri(first.uri.toString())
                    onVideoClick(encodedUri, -1L, "folder")
                    selectedKeys = emptySet()
                }
            }
            selectedTab == MediaTab.AUDIOS && (selectedAudioFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) -> {
                val list = sortedAudios.filter { selectedKeys.contains(it.uri.toString()) }
                if (list.isNotEmpty()) {
                    audioViewModel.playAudios(list, 0)
                    val first = list.first()
                    val encodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeEncodeUri(first.uri.toString())
                    onAudioClick(encodedUri, -1L, "folder")
                    selectedKeys = emptySet()
                }
            }
            selectedTab == MediaTab.VIDEOS -> {
                scope.launch {
                    isProcessing = true
                    val allVideos = selectedKeys.flatMap { viewModel.getVideosInFolder(it) }
                    if (allVideos.isNotEmpty()) {
                        viewModel.playVideos(allVideos, 0)
                        val encodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeEncodeUri(allVideos.first().uri.toString())
                        onVideoClick(encodedUri, -1L, "folder_batch")
                        selectedKeys = emptySet()
                    }
                    isProcessing = false
                }
            }
            selectedTab == MediaTab.AUDIOS -> {
                scope.launch {
                    isProcessing = true
                    val allAudios = selectedKeys.flatMap { audioViewModel.getAudiosInFolder(it) }
                    if (allAudios.isNotEmpty()) {
                        audioViewModel.playAudios(allAudios, 0)
                        val encodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeEncodeUri(allAudios.first().uri.toString())
                        onAudioClick(encodedUri, -1L, "folder_batch")
                        selectedKeys = emptySet()
                    }
                    isProcessing = false
                }
            }
        }
    }

    val handleBatchAddToPlaylist: () -> Unit = {
        scope.launch {
            val itemsToAdd = mutableListOf<Pair<String, String>>()
            when {
                showFullHistoryScreen || selectedTab == MediaTab.HISTORY -> {
                    sortedHistory.filter { selectedKeys.contains(it.uri) }.forEach {
                        itemsToAdd.add(it.uri to it.mediaType)
                    }
                }
                selectedPlaylistForDetails != null -> {
                    sortedPlaylistItems.filter { selectedKeys.contains(it.id.toString()) }.forEach {
                        itemsToAdd.add(it.mediaUri to it.mediaType)
                    }
                }
                searchQuery.isNotEmpty() && searchScope == SearchScope.ALL_MEDIA && (selectedTab == MediaTab.VIDEOS || selectedTab == MediaTab.AUDIOS) -> {
                    sortedAllMedia.filter { selectedKeys.contains(it.uri.toString()) }.forEach {
                        itemsToAdd.add(it.uri.toString() to if (it.isVideo) "video" else "audio")
                    }
                }
                selectedTab == MediaTab.VIDEOS && (selectedVideoFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) -> {
                    sortedVideos.filter { selectedKeys.contains(it.uri.toString()) }.forEach {
                        itemsToAdd.add(it.uri.toString() to "video")
                    }
                }
                selectedTab == MediaTab.AUDIOS && (selectedAudioFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) -> {
                    sortedAudios.filter { selectedKeys.contains(it.uri.toString()) }.forEach {
                        itemsToAdd.add(it.uri.toString() to "audio")
                    }
                }
                selectedTab == MediaTab.VIDEOS -> {
                    selectedKeys.forEach { folderPath ->
                        viewModel.getVideosInFolder(folderPath).forEach {
                            itemsToAdd.add(it.uri.toString() to "video")
                        }
                    }
                }
                selectedTab == MediaTab.AUDIOS -> {
                    selectedKeys.forEach { folderPath ->
                        audioViewModel.getAudiosInFolder(folderPath).forEach {
                            itemsToAdd.add(it.uri.toString() to "audio")
                        }
                    }
                }
            }
            if (itemsToAdd.isNotEmpty()) {
                batchPendingPlaylist = itemsToAdd
                showAddToPlaylistDialog = true
            }
        }
    }

    val handleBatchShare: () -> Unit = {
        scope.launch {
            val urisToShare = mutableListOf<Uri>()
            var mime = "*/*"
            when {
                showFullHistoryScreen || selectedTab == MediaTab.HISTORY -> {
                    sortedHistory.filter { selectedKeys.contains(it.uri) }.forEach {
                        urisToShare.add(Uri.parse(it.uri))
                    }
                }
                selectedPlaylistForDetails != null -> {
                    sortedPlaylistItems.filter { selectedKeys.contains(it.id.toString()) }.forEach {
                        urisToShare.add(Uri.parse(it.mediaUri))
                    }
                }
                searchQuery.isNotEmpty() && searchScope == SearchScope.ALL_MEDIA && (selectedTab == MediaTab.VIDEOS || selectedTab == MediaTab.AUDIOS) -> {
                    sortedAllMedia.filter { selectedKeys.contains(it.uri.toString()) }.forEach {
                        urisToShare.add(it.uri)
                    }
                }
                selectedTab == MediaTab.VIDEOS && (selectedVideoFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) -> {
                    mime = "video/*"
                    sortedVideos.filter { selectedKeys.contains(it.uri.toString()) }.forEach {
                        urisToShare.add(it.uri)
                    }
                }
                selectedTab == MediaTab.AUDIOS && (selectedAudioFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) -> {
                    mime = "audio/*"
                    sortedAudios.filter { selectedKeys.contains(it.uri.toString()) }.forEach {
                        urisToShare.add(it.uri)
                    }
                }
                selectedTab == MediaTab.VIDEOS -> {
                    mime = "video/*"
                    selectedKeys.forEach { folderPath ->
                        viewModel.getVideosInFolder(folderPath).forEach {
                            urisToShare.add(it.uri)
                        }
                    }
                }
                selectedTab == MediaTab.AUDIOS -> {
                    mime = "audio/*"
                    selectedKeys.forEach { folderPath ->
                        audioViewModel.getAudiosInFolder(folderPath).forEach {
                            urisToShare.add(it.uri)
                        }
                    }
                }
            }
            if (urisToShare.isNotEmpty()) {
                com.mohamedaminelouati.oplayer.util.MediaUtils.shareMedia(context, urisToShare, mime)
                selectedKeys = emptySet()
            }
        }
    }

    val handleBatchDelete: () -> Unit = {
        scope.launch {
            val urisToDelete = mutableListOf<Uri>()
            when {
                showFullHistoryScreen || selectedTab == MediaTab.HISTORY -> {
                    sortedHistory.filter { selectedKeys.contains(it.uri) }.forEach {
                        urisToDelete.add(Uri.parse(it.uri))
                    }
                }
                searchQuery.isNotEmpty() && searchScope == SearchScope.ALL_MEDIA && (selectedTab == MediaTab.VIDEOS || selectedTab == MediaTab.AUDIOS) -> {
                    sortedAllMedia.filter { selectedKeys.contains(it.uri.toString()) }.forEach {
                        urisToDelete.add(it.uri)
                    }
                }
                selectedTab == MediaTab.VIDEOS && (selectedVideoFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) -> {
                    sortedVideos.filter { selectedKeys.contains(it.uri.toString()) }.forEach {
                        urisToDelete.add(it.uri)
                    }
                }
                selectedTab == MediaTab.AUDIOS && (selectedAudioFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) -> {
                    sortedAudios.filter { selectedKeys.contains(it.uri.toString()) }.forEach {
                        urisToDelete.add(it.uri)
                    }
                }
                selectedTab == MediaTab.VIDEOS -> {
                    selectedKeys.forEach { folderPath ->
                        viewModel.getVideosInFolder(folderPath).forEach {
                            urisToDelete.add(it.uri)
                        }
                    }
                }
                selectedTab == MediaTab.AUDIOS -> {
                    selectedKeys.forEach { folderPath ->
                        audioViewModel.getAudiosInFolder(folderPath).forEach {
                            urisToDelete.add(it.uri)
                        }
                    }
                }
            }
            if (urisToDelete.isNotEmpty()) {
                pendingUrisToDelete = urisToDelete
                selectedKeys = emptySet()
                com.mohamedaminelouati.oplayer.util.MediaUtils.requestMediaDelete(
                    context = context,
                    uris = urisToDelete,
                    deleteLauncher = deleteLauncher,
                    onRequireInternalPopup = {
                        scope.launch {
                            isProcessing = true
                            urisToDelete.forEach { uri ->
                                viewModel.deleteHistoryItem(uri.toString())
                            }
                            viewModel.stopIfPlaying(urisToDelete)
                            audioViewModel.stopIfPlaying(urisToDelete)
                            viewModel.onVideosDeleted(urisToDelete)
                            audioViewModel.onAudiosDeleted(urisToDelete)
                            isProcessing = false
                        }
                    }
                )
            }
        }
    }

    val handleBatchRemoveFromPlaylist: () -> Unit = {
        selectedPlaylistForDetails?.let { playlist ->
            val itemsToRemove = sortedPlaylistItems.filter { selectedKeys.contains(it.id.toString()) }
            itemsToRemove.forEach { item ->
                audioViewModel.removeFromPlaylist(playlist.id, item.mediaUri)
            }
            selectedKeys = emptySet()
        }
    }

    val handleBatchDeletePlaylists: () -> Unit = {
        selectedKeys.forEach { idStr ->
            playlists.firstOrNull { it.id.toString() == idStr }?.let { playlist ->
                audioViewModel.deletePlaylist(playlist)
            }
        }
        selectedKeys = emptySet()
    }

    val handleBatchRename: () -> Unit = {
        if (selectedKeys.size == 1) {
            val singleKey = selectedKeys.first()
            if (isFolderLevel) {
                renameFolderTarget = Pair(singleKey, selectedTab == MediaTab.VIDEOS)
            } else {
                when {
                    selectedPlaylistForDetails != null -> {
                        val pItem = sortedPlaylistItems.firstOrNull { it.id.toString() == singleKey }
                        if (pItem != null) {
                            val isVid = pItem.mediaType == "video"
                            val vid = if (isVid) videosByUri[pItem.mediaUri] else null
                            val aud = if (!isVid) audiosByUri[pItem.mediaUri] else null
                            val uri = vid?.uri ?: aud?.uri ?: Uri.parse(pItem.mediaUri)
                            val name = vid?.name ?: aud?.name ?: ""
                            val path = vid?.path ?: aud?.path ?: ""
                            if (name.isNotEmpty()) {
                                renameMediaTarget = Triple(uri, name, path)
                            }
                        }
                    }
                    searchQuery.isNotEmpty() && searchScope == SearchScope.ALL_MEDIA && (selectedTab == MediaTab.VIDEOS || selectedTab == MediaTab.AUDIOS) -> {
                        val searchItem = sortedAllMedia.firstOrNull { it.uri.toString() == singleKey }
                        if (searchItem != null) {
                            renameMediaTarget = Triple(searchItem.uri, searchItem.name, searchItem.path)
                        }
                    }
                    selectedTab == MediaTab.VIDEOS -> {
                        val vid = sortedVideos.firstOrNull { it.uri.toString() == singleKey } ?: videosByUri[singleKey]
                        if (vid != null) {
                            renameMediaTarget = Triple(vid.uri, vid.name, vid.path)
                        }
                    }
                    selectedTab == MediaTab.AUDIOS -> {
                        val aud = sortedAudios.firstOrNull { it.uri.toString() == singleKey } ?: audiosByUri[singleKey]
                        if (aud != null) {
                            renameMediaTarget = Triple(aud.uri, aud.name, aud.path)
                        }
                    }
                }
            }
        }
    }

    val handleBatchInfo: () -> Unit = {
        if (selectedKeys.size == 1) {
            val singleKey = selectedKeys.first()
            if (isFolderLevel) {
                val count = if (selectedTab == MediaTab.VIDEOS) {
                    sortedVideoFolders.firstOrNull { it.path == singleKey }?.videoCount ?: viewModel.getVideosInFolder(singleKey).size
                } else {
                    sortedAudioFolders.firstOrNull { it.path == singleKey }?.videoCount ?: audioViewModel.getAudiosInFolder(singleKey).size
                }
                val name = singleKey.substringAfterLast("/")
                folderInfoTarget = Triple(name, singleKey, count)
            } else {
                when {
                    isHistoryContext -> {
                        val hist = sortedHistory.firstOrNull { it.uri == singleKey }
                        if (hist != null) {
                            val mediaPath = videosByUri[hist.uri]?.path ?: audiosByUri[hist.uri]?.path ?: ""
                            scope.launch {
                                detailedInfoTarget = extractDetailedMediaInfo(
                                    context = context,
                                    uri = Uri.parse(hist.uri),
                                    fileName = hist.name,
                                    path = mediaPath,
                                    sizeBytes = hist.size,
                                    durationMs = hist.duration
                                )
                            }
                        }
                    }
                    selectedPlaylistForDetails != null -> {
                        val pItem = sortedPlaylistItems.firstOrNull { it.id.toString() == singleKey }
                        if (pItem != null) {
                            val isVid = pItem.mediaType == "video"
                            val vid = if (isVid) videosByUri[pItem.mediaUri] else null
                            val aud = if (!isVid) audiosByUri[pItem.mediaUri] else null
                            val uri = vid?.uri ?: aud?.uri ?: Uri.parse(pItem.mediaUri)
                            val name = vid?.name ?: aud?.name ?: ""
                            val path = vid?.path ?: aud?.path ?: ""
                            val size = vid?.size ?: aud?.size ?: 0L
                            val duration = vid?.duration ?: aud?.duration ?: 0L
                            scope.launch {
                                detailedInfoTarget = extractDetailedMediaInfo(
                                    context = context,
                                    uri = uri,
                                    fileName = name,
                                    path = path,
                                    sizeBytes = size,
                                    durationMs = duration
                                )
                            }
                        }
                    }
                    searchQuery.isNotEmpty() && searchScope == SearchScope.ALL_MEDIA && (selectedTab == MediaTab.VIDEOS || selectedTab == MediaTab.AUDIOS) -> {
                        val searchItem = sortedAllMedia.firstOrNull { it.uri.toString() == singleKey }
                        if (searchItem != null) {
                            scope.launch {
                                detailedInfoTarget = extractDetailedMediaInfo(
                                    context = context,
                                    uri = searchItem.uri,
                                    fileName = searchItem.name,
                                    path = searchItem.path,
                                    sizeBytes = searchItem.size,
                                    durationMs = searchItem.duration
                                )
                            }
                        }
                    }
                    selectedTab == MediaTab.VIDEOS -> {
                        val vid = sortedVideos.firstOrNull { it.uri.toString() == singleKey } ?: videosByUri[singleKey]
                        if (vid != null) {
                            scope.launch {
                                detailedInfoTarget = extractDetailedMediaInfo(
                                    context = context,
                                    uri = vid.uri,
                                    fileName = vid.name,
                                    path = vid.path,
                                    sizeBytes = vid.size,
                                    durationMs = vid.duration
                                )
                            }
                        }
                    }
                    selectedTab == MediaTab.AUDIOS -> {
                        val aud = sortedAudios.firstOrNull { it.uri.toString() == singleKey } ?: audiosByUri[singleKey]
                        if (aud != null) {
                            scope.launch {
                                detailedInfoTarget = extractDetailedMediaInfo(
                                    context = context,
                                    uri = aud.uri,
                                    fileName = aud.name,
                                    path = aud.path,
                                    sizeBytes = aud.size,
                                    durationMs = aud.duration
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val handleBatchToggleLock: () -> Unit = {
        if (isFolderLevel) {
            val anyLocked = selectedKeys.any { securityLockedFolders.contains(it) }
            val action = {
                if (anyLocked) {
                    selectedKeys.forEach { path -> viewModel.unlockFolder(path) }
                    Toast.makeText(context, context.getString(R.string.folder_unlocked_toast), Toast.LENGTH_SHORT).show()
                } else {
                    selectedKeys.forEach { path -> viewModel.toggleFolderLock(path) }
                    Toast.makeText(context, context.getString(R.string.folder_locked_toast), Toast.LENGTH_SHORT).show()
                }
                selectedKeys = emptySet()
            }
            if (anyLocked) {
                if (securityLockType == SecurityLockType.NONE) {
                    action()
                } else {
                    pendingAuthAction = action
                }
            } else {
                if (securityLockType == SecurityLockType.NONE) {
                    Toast.makeText(context, context.getString(R.string.security_setup_required), Toast.LENGTH_LONG).show()
                } else {
                    action()
                }
            }
        } else {
            val anyLocked = selectedKeys.any { securityLockedMedia.contains(it) }
            val action = {
                if (anyLocked) {
                    selectedKeys.forEach { uri -> viewModel.unlockMedia(uri) }
                    Toast.makeText(context, context.getString(R.string.media_unlocked_toast), Toast.LENGTH_SHORT).show()
                } else {
                    selectedKeys.forEach { uri -> viewModel.toggleMediaLock(uri) }
                    Toast.makeText(context, context.getString(R.string.media_locked_toast), Toast.LENGTH_SHORT).show()
                }
                selectedKeys = emptySet()
            }
            if (anyLocked) {
                if (securityLockType == SecurityLockType.NONE) {
                    action()
                } else {
                    pendingAuthAction = action
                }
            } else {
                if (securityLockType == SecurityLockType.NONE) {
                    Toast.makeText(context, context.getString(R.string.security_setup_required), Toast.LENGTH_LONG).show()
                } else {
                    action()
                }
            }
        }
    }

    val handleBatchRemoveFromHistory: () -> Unit = {
        selectedKeys.forEach { uri ->
            viewModel.deleteHistoryItem(uri)
        }
        selectedKeys = emptySet()
    }

    var pendingMediaRename by remember { mutableStateOf<com.mohamedaminelouati.oplayer.util.PendingMediaRename?>(null) }
    var pendingFolderRename by remember { mutableStateOf<com.mohamedaminelouati.oplayer.util.PendingFolderRename?>(null) }

    val renameLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pendingMedia = pendingMediaRename
            val pendingFolder = pendingFolderRename
            pendingMediaRename = null
            pendingFolderRename = null
            scope.launch {
                isProcessing = true
                val success = withContext(Dispatchers.IO) {
                    when {
                        pendingMedia != null -> com.mohamedaminelouati.oplayer.util.MediaUtils.completePendingRename(context, pendingMedia)
                        pendingFolder != null -> com.mohamedaminelouati.oplayer.util.MediaUtils.completePendingFolderRename(context, pendingFolder)
                        else -> false
                    }
                }
                if (success) {
                    viewModel.manualRefresh()
                    audioViewModel.manualRefresh()
                } else {
                    Toast.makeText(context, context.getString(R.string.error_rename_failed), Toast.LENGTH_SHORT).show()
                }
                isProcessing = false
            }
        } else {
            pendingMediaRename = null
            pendingFolderRename = null
            isProcessing = false
        }
    }
    
    fun checkPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    var hasPermission by remember { mutableStateOf(checkPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasPermission = permissions.values.all { it }
        if (hasPermission) { viewModel.fetchVideos(context); audioViewModel.fetchAudios(context) }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.autoSync()
        }
    }

    var selectedVideoForDelete by remember { mutableStateOf<VideoModel?>(null) }
    var selectedAudioForDelete by remember { mutableStateOf<AudioModel?>(null) }
    var selectedHistoryForDelete by remember { mutableStateOf<RecentPlayback?>(null) }
    var folderToDelete by remember { mutableStateOf<String?>(null) }

    folderToDelete?.let { folderName ->
        DeleteFolderDialog(
            folderName = folderName,
            isVideoTab = selectedTab == MediaTab.VIDEOS,
            viewModel = viewModel,
            audioViewModel = audioViewModel,
            context = context,
            scope = scope,
            onProcessingChange = { isProcessing = it },
            onDismiss = { folderToDelete = null }
        )
    }

    selectedVideoForDelete?.let { video ->
        DeleteVideoDialog(
            video = video,
            viewModel = viewModel,
            context = context,
            scope = scope,
            onProcessingChange = { isProcessing = it },
            onDismiss = { selectedVideoForDelete = null }
        )
    }

    selectedAudioForDelete?.let { audio ->
        DeleteAudioDialog(
            audio = audio,
            audioViewModel = audioViewModel,
            context = context,
            scope = scope,
            onProcessingChange = { isProcessing = it },
            onDismiss = { selectedAudioForDelete = null }
        )
    }

    selectedHistoryForDelete?.let { item ->
        DeleteHistoryItemDialog(
            item = item,
            viewModel = viewModel,
            audioViewModel = audioViewModel,
            context = context,
            scope = scope,
            onProcessingChange = { isProcessing = it },
            onDismiss = { selectedHistoryForDelete = null }
        )
    }

    if (isProcessing) ModernLoadingDialog()

    if (showBatchDeletePlaylistConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeletePlaylistConfirm = false },
            title = { Text(text = stringResource(R.string.action_delete_playlist)) },
            text = { Text(text = stringResource(R.string.delete_selected_playlists_confirm, selectedKeys.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatchDeletePlaylistConfirm = false
                        handleBatchDeletePlaylists()
                    }
                ) {
                    Text(text = stringResource(R.string.action_delete_playlist), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeletePlaylistConfirm = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showAddToPlaylistDialog) {
        if (batchPendingPlaylist != null) {
            AddToPlaylistFromHomeDialog(
                playlists = playlists,
                onDismiss = { showAddToPlaylistDialog = false; batchPendingPlaylist = null },
                onPlaylistSelected = { playlistId ->
                    batchPendingPlaylist?.let { items ->
                        items.forEach { (uri, type) ->
                            audioViewModel.addToPlaylist(playlistId, uri, type)
                        }
                        Toast.makeText(context, context.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                    }
                    showAddToPlaylistDialog = false
                    batchPendingPlaylist = null
                    selectedKeys = emptySet()
                },
                onCreatePlaylist = { name -> audioViewModel.createPlaylist(name) }
            )
        } else if (mediaPendingPlaylist != null) {
            AddToPlaylistFromHomeDialog(
                playlists = playlists,
                onDismiss = { showAddToPlaylistDialog = false; mediaPendingPlaylist = null },
                onPlaylistSelected = { playlistId ->
                    mediaPendingPlaylist?.let { (uri, type) ->
                        audioViewModel.addToPlaylist(playlistId, uri, type)
                        Toast.makeText(context, context.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                    }
                    showAddToPlaylistDialog = false
                    mediaPendingPlaylist = null
                },
                onCreatePlaylist = { name -> audioViewModel.createPlaylist(name) }
            )
        } else if (folderPendingPlaylist != null) {
            AddToPlaylistFromHomeDialog(
                playlists = playlists,
                onDismiss = { showAddToPlaylistDialog = false; folderPendingPlaylist = null },
                onPlaylistSelected = { playlistId ->
                    folderPendingPlaylist?.let { (folderPath, type) ->
                        scope.launch {
                            isProcessing = true
                            if (type == "video") {
                                val videosInFolder = viewModel.getVideosInFolder(folderPath)
                                videosInFolder.forEach {
                                    audioViewModel.addToPlaylist(playlistId, it.uri.toString(), "video")
                                }
                            } else {
                                val audiosInFolder = audioViewModel.getAudiosInFolder(folderPath)
                                audiosInFolder.forEach {
                                    audioViewModel.addToPlaylist(playlistId, it.uri.toString(), "audio")
                                }
                            }
                            Toast.makeText(context, context.getString(R.string.added_folder_to_playlist), Toast.LENGTH_SHORT).show()
                            isProcessing = false
                        }
                    }
                    showAddToPlaylistDialog = false
                    folderPendingPlaylist = null
                },
                onCreatePlaylist = { name -> audioViewModel.createPlaylist(name) }
            )
        }
    }

    val currentItemCount = when {
        showFullHistoryScreen -> sortedHistory.size
        selectedPlaylistForDetails != null -> sortedPlaylistItems.size
        searchQuery.isNotEmpty() && searchScope == SearchScope.ALL_MEDIA && (selectedTab == MediaTab.VIDEOS || selectedTab == MediaTab.AUDIOS) -> sortedAllMedia.size
        selectedTab == MediaTab.VIDEOS -> {
            if (selectedVideoFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) sortedVideos.size
            else sortedVideoFolders.size
        }
        selectedTab == MediaTab.AUDIOS -> {
            if (selectedAudioFolder != null || (searchQuery.isNotEmpty() && searchScope == SearchScope.CURRENT_TYPE)) sortedAudios.size
            else sortedAudioFolders.size
        }
        selectedTab == MediaTab.PLAYLISTS -> sortedPlaylists.size
        selectedTab == MediaTab.HISTORY -> sortedHistory.size
        else -> currentFolders.size
    }

    val onHeaderBack: () -> Unit = {
        if (selectedKeys.isNotEmpty()) {
            selectedKeys = emptySet()
        } else if (showFullHistoryScreen) {
            showFullHistoryScreen = false
        } else if (previousTabBeforeHistory != null && selectedTab == MediaTab.HISTORY) {
            selectedTab = previousTabBeforeHistory ?: MediaTab.VIDEOS
            previousTabBeforeHistory = null
        } else if (isCurrentFolderOpen) {
            if (selectedTab == MediaTab.VIDEOS) viewModel.setSelectedFolder(null)
            else if (selectedTab == MediaTab.AUDIOS) audioViewModel.setSelectedFolder(null)
            else selectedPlaylistForDetails = null
        } else if (isNavPathOpen) {
            if (selectedTab == MediaTab.VIDEOS) viewModel.navigateUp(videoFolderTree)
            else if (selectedTab == MediaTab.AUDIOS) audioViewModel.navigateUp(audioFolderTree)
        }
    }

    val onOpenPlaylist: (Playlist) -> Unit = { selectedPlaylistForDetails = it }
    val onOpenHistory: () -> Unit = {
        if (showHistoryTab) {
            previousTabBeforeHistory = selectedTab
            selectedTab = MediaTab.HISTORY
        } else {
            showFullHistoryScreen = true
        }
    }
    val onMediaPendingPlaylist: (String, String) -> Unit = { uri, type -> mediaPendingPlaylist = uri to type; showAddToPlaylistDialog = true }
    val onFolderPendingPlaylist: (String, String) -> Unit = { path, type -> folderPendingPlaylist = path to type; showAddToPlaylistDialog = true }
    val onFolderExclude: (String, Boolean) -> Unit = { path, isVideo ->
        if (isVideo) viewModel.excludeFolder(path) else audioViewModel.excludeFolder(path)
        Toast.makeText(context, context.getString(R.string.folder_excluded_toast), Toast.LENGTH_SHORT).show()
    }
    val onHistoryDeviceDelete: (RecentPlayback) -> Unit = { item ->
        val uriToDel = Uri.parse(item.uri)
        pendingUrisToDelete = listOf(uriToDel)
        com.mohamedaminelouati.oplayer.util.MediaUtils.requestMediaDelete(context = context, uris = listOf(uriToDel), deleteLauncher = deleteLauncher, onRequireInternalPopup = { selectedHistoryForDelete = item })
    }
    val onFolderDeviceDelete: (String, Boolean) -> Unit = { path, isVideo ->
        scope.launch {
            val uris = if (isVideo) viewModel.getVideosInFolder(path).map { it.uri } else audioViewModel.getAudiosInFolder(path).map { it.uri }
            if (uris.isNotEmpty()) {
                pendingUrisToDelete = uris
                com.mohamedaminelouati.oplayer.util.MediaUtils.requestMediaDelete(context = context, uris = uris, deleteLauncher = deleteLauncher, onRequireInternalPopup = { folderToDelete = path })
            }
        }
    }
    val onVideoDeviceDelete: (VideoModel) -> Unit = { video ->
        pendingUrisToDelete = listOf(video.uri)
        com.mohamedaminelouati.oplayer.util.MediaUtils.requestMediaDelete(context = context, uris = listOf(video.uri), deleteLauncher = deleteLauncher, onRequireInternalPopup = { selectedVideoForDelete = video })
    }
    val onAudioDeviceDelete: (AudioModel) -> Unit = { audio ->
        pendingUrisToDelete = listOf(audio.uri)
        com.mohamedaminelouati.oplayer.util.MediaUtils.requestMediaDelete(context = context, uris = listOf(audio.uri), deleteLauncher = deleteLauncher, onRequireInternalPopup = { selectedAudioForDelete = audio })
    }

    val onMediaRename: (Uri, String, String, String) -> Unit = { uri, name, path, newName ->
        scope.launch {
            val pending = com.mohamedaminelouati.oplayer.util.MediaUtils.renameMedia(
                context = context,
                uri = uri,
                currentName = name,
                newNameInput = newName,
                oldPath = path,
                renameLauncher = renameLauncher
            ) { success ->
                if (success) {
                    viewModel.manualRefresh()
                    audioViewModel.manualRefresh()
                } else {
                    Toast.makeText(context, context.getString(R.string.error_rename_failed), Toast.LENGTH_SHORT).show()
                }
            }
            if (pending != null) {
                pendingMediaRename = pending
            }
        }
    }

    val onVideoRename: (VideoModel, String) -> Unit = { video, newName ->
        onMediaRename(video.uri, video.name, video.path, newName)
    }

    val onAudioRename: (AudioModel, String) -> Unit = { audio, newName ->
        onMediaRename(audio.uri, audio.name, audio.path, newName)
    }

    val onFolderRename: (String, String, Boolean) -> Unit = { path, newName, isVideo ->
        scope.launch {
            val videosInFolder = viewModel.getVideosInFolder(path)
            val audiosInFolder = audioViewModel.getAudiosInFolder(path)
            val allItems = videosInFolder.map { it.uri to it.path } + audiosInFolder.map { it.uri to it.path }
            val pending = com.mohamedaminelouati.oplayer.util.MediaUtils.renameFolder(
                context = context,
                folderPath = path,
                newFolderName = newName,
                mediaItems = allItems,
                renameLauncher = renameLauncher
            ) { success ->
                if (success) {
                    viewModel.manualRefresh()
                    audioViewModel.manualRefresh()
                } else {
                    Toast.makeText(context, context.getString(R.string.error_rename_failed), Toast.LENGTH_SHORT).show()
                }
            }
            if (pending != null) {
                pendingFolderRename = pending
            }
        }
    }

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

    renameMediaTarget?.let { target ->
        MediaRenameDialog(
            currentName = target.second,
            onDismiss = { renameMediaTarget = null },
            onConfirm = { newName ->
                val (uri, name, path) = target
                renameMediaTarget = null
                onMediaRename(uri, name, path, newName)
                selectedKeys = emptySet()
            }
        )
    }

    renameFolderTarget?.let { target ->
        MediaRenameDialog(
            currentName = target.first.substringAfterLast("/"),
            onDismiss = { renameFolderTarget = null },
            onConfirm = { newName ->
                val (path, isVideo) = target
                renameFolderTarget = null
                onFolderRename(path, newName, isVideo)
                selectedKeys = emptySet()
            }
        )
    }

    detailedInfoTarget?.let { info ->
        MediaInfoDialog(
            info = info,
            onDismiss = { detailedInfoTarget = null }
        )
    }

    folderInfoTarget?.let { info ->
        FolderInfoDialog(
            name = info.first,
            path = info.second,
            count = info.third,
            onDismiss = { folderInfoTarget = null }
        )
    }

    CompositionLocalProvider(LocalPlaybackProgress provides playbackProgressMap) {
        Scaffold(
        topBar = {
            HomeHeader(
                pagerState = pagerState,
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab != MediaTab.HISTORY) previousTabBeforeHistory = null
                    selectedTab = tab
                },
                showHistoryTab = showHistoryTab,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchScope = searchScope,
                onSearchScopeChange = { searchScope = it },
                isCurrentFolderOpen = isCurrentFolderOpen,
                isNavPathOpen = isNavPathOpen,
                showFullHistoryScreen = showFullHistoryScreen,
                currentSelectedFolder = currentSelectedFolder,
                selectedPlaylistForDetails = selectedPlaylistForDetails,
                videoNavPath = videoNavPath,
                audioNavPath = audioNavPath,
                currentContextKey = currentContextKey,
                currentSortOrder = currentSortOrder,
                currentTabViewMode = currentTabViewMode,
                itemCount = currentItemCount,
                onBackClick = onHeaderBack,
                onSettingsClick = onSettingsClick,
                viewModel = viewModel,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedKeys.size,
                totalCount = visibleItemKeys.size,
                onClearSelection = { selectedKeys = emptySet() },
                onSelectAllToggle = {
                    if (visibleItemKeys.isNotEmpty() && selectedKeys.containsAll(visibleItemKeys)) {
                        selectedKeys = emptySet()
                    } else {
                        selectedKeys = visibleItemKeys.toSet()
                    }
                },
                isAllSelected = visibleItemKeys.isNotEmpty() && selectedKeys.containsAll(visibleItemKeys)
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SelectionFooterBar(
                    onPlay = if (!isPlaylistTopLevel) handleBatchPlay else null,
                    onAddToPlaylist = if (!isPlaylistTopLevel) handleBatchAddToPlaylist else null,
                    onRemoveFromPlaylist = if (selectedPlaylistForDetails != null) handleBatchRemoveFromPlaylist else null,
                    onRename = if (!isPlaylistTopLevel && !isHistoryContext) handleBatchRename else null,
                    onShare = if (!isPlaylistTopLevel) handleBatchShare else null,
                    onInformation = if (!isPlaylistTopLevel) handleBatchInfo else null,
                    onToggleLock = if (canShowLock) handleBatchToggleLock else null,
                    onRemoveFromHistory = if (isHistoryContext) handleBatchRemoveFromHistory else null,
                    onDelete = {
                        if (isPlaylistTopLevel) {
                            showBatchDeletePlaylistConfirm = true
                        } else {
                            handleBatchDelete()
                        }
                    },
                    deleteLabel = when {
                        isPlaylistTopLevel -> stringResource(R.string.action_delete_playlist)
                        isFolderLevel -> stringResource(R.string.menu_delete_folder)
                        else -> stringResource(R.string.delete_from_device)
                    },
                    canPlay = canBatchPlay,
                    canAddToPlaylist = canBatchAddToPlaylist,
                    canRemoveFromPlaylist = selectedKeys.isNotEmpty(),
                    canRename = canBatchRename,
                    canShare = canBatchShare,
                    canInformation = canBatchInformation,
                    canToggleLock = selectedKeys.isNotEmpty(),
                    canRemoveFromHistory = selectedKeys.isNotEmpty(),
                    canDelete = canBatchDelete,
                    isLocked = isSelectionLocked,
                    isFolder = isFolderLevel,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        },
        floatingActionButton = {
            if (!hasPermission) {
                ExtendedFloatingActionButton(modifier = Modifier.padding(bottom = 80.dp), text = { Text(stringResource(R.string.grant_access)) }, icon = { Icon(Icons.Default.AddCircle, null) }, onClick = { launcher.launch(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)) }, containerColor = Color.White, contentColor = Color(0xFF1A1A1A), shape = RoundedCornerShape(16.dp))
            }
        }
    ) { padding ->
        if (showFullHistoryScreen) {
            val pageViewMode by remember("tab_history") {
                viewModel.getFolderViewMode("tab_history")
            }.collectAsStateWithLifecycle(initialValue = viewModel.defaultViewMode.value)

            val historyScrollKey = remember(currentSortOrder, searchQuery) {
                "history_${currentSortOrder.name}_$searchQuery"
            }
            val historyListState = remember(historyScrollKey) { LazyListState() }
            val historyGridState = remember(historyScrollKey) { LazyGridState() }
            LaunchedEffect(currentSortOrder, searchQuery) {
                historyListState.scrollToItem(0)
                historyGridState.scrollToItem(0)
            }

            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (sortedHistory.isEmpty()) {
                    EmptyState(searchQuery.isNotEmpty(), false)
                } else {
                    if (pageViewMode == 1 || pageViewMode == 2) {
                            val cols = if (pageViewMode == 1) 2 else 1
                            val ratio = if (pageViewMode == 1) 1f else (16f / 9f)
                            LazyVerticalGrid(state = historyGridState, columns = GridCells.Fixed(cols), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(sortedHistory, key = { it.uri }) { item ->
                                    HistoryGridCard(
                                        item = item,
                                        onClick = {
                                            val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                            viewModel.playHistory(sortedHistory, index, videos, audios, audiosByUri = audiosByUri)
                                            val encodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                            if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                        },
                                        onDelete = {
                                            viewModel.deleteHistoryItem(item.uri)
                                        },
                                        onDeleteFromDevice = { onHistoryDeviceDelete(item) },
                                        onAddToPlaylist = { onMediaPendingPlaylist(item.uri, item.mediaType) },
                                        aspectRatio = ratio,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedKeys.contains(item.uri),
                                        onToggleSelect = { onToggleSelect(item.uri) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(state = historyListState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(sortedHistory, key = { it.uri }) { item ->
                                    HistoryItem(
                                        item = item,
                                        onClick = {
                                            val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                            viewModel.playHistory(sortedHistory, index, videos, audios, audiosByUri = audiosByUri)
                                            val encodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                            if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
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
                        }
                }
            }
        } else {
            HomeTabPager(
                pagerState = pagerState,
                activeTabs = activeTabs,
                isLoading = isLoading,
                searchQuery = searchQuery,
                searchScope = searchScope,
                sortedAllMedia = sortedAllMedia,
                bottomPadding = bottomPadding,
                showRecentHistoryOnHome = showRecentHistoryOnHome,
                currentSortOrder = currentSortOrder,
                currentSelectedFolder = currentSelectedFolder,
                selectedVideoFolder = selectedVideoFolder,
                selectedAudioFolder = selectedAudioFolder,
                selectedPlaylistForDetails = selectedPlaylistForDetails,
                recentPlayback = recentPlayback,
                sortedVideoFolders = sortedVideoFolders,
                sortedAudioFolders = sortedAudioFolders,
                sortedVideos = sortedVideos,
                sortedAudios = sortedAudios,
                sortedPlaylists = sortedPlaylists,
                sortedHistory = sortedHistory,
                sortedPlaylistItems = sortedPlaylistItems,
                videos = videos,
                audios = audios,
                videosByUri = videosByUri,
                audiosByUri = audiosByUri,
                isVideoPlaying = isVideoPlaying,
                isAudioPlaying = isAudioPlaying,
                activeVideoUri = activeVideoUri,
                activeAudioUri = activeAudioUri,
                padding = padding,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                viewModel = viewModel,
                audioViewModel = audioViewModel,
                onVideoClick = onVideoClick,
                onAudioClick = onAudioClick,
                onOpenPlaylist = onOpenPlaylist,
                onOpenHistory = onOpenHistory,
                onMediaPendingPlaylist = onMediaPendingPlaylist,
                onFolderPendingPlaylist = onFolderPendingPlaylist,
                onFolderExclude = onFolderExclude,
                onHistoryDeviceDelete = onHistoryDeviceDelete,
                onFolderDeviceDelete = onFolderDeviceDelete,
                onVideoDeviceDelete = onVideoDeviceDelete,
                onAudioDeviceDelete = onAudioDeviceDelete,
                onVideoRename = onVideoRename,
                onAudioRename = onAudioRename,
                onFolderRename = onFolderRename,
                selectedKeys = selectedKeys,
                isSelectionMode = isSelectionMode,
                onToggleSelect = onToggleSelect
            )
        }
    }
}
}
