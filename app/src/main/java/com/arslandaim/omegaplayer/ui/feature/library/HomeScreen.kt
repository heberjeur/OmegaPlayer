package com.arslandaim.omegaplayer.ui.feature.library

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
import androidx.compose.foundation.lazy.grid.GridCells
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
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.data.AudioModel
import com.arslandaim.omegaplayer.data.Playlist
import com.arslandaim.omegaplayer.data.PlaylistItem
import com.arslandaim.omegaplayer.viewmodel.AudioViewModel
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel
import com.arslandaim.omegaplayer.ui.common.ModernLoadingDialog
import com.arslandaim.omegaplayer.data.MediaSortOrder
import com.arslandaim.omegaplayer.data.RecentPlayback
import com.arslandaim.omegaplayer.ui.feature.library.components.*
import com.arslandaim.omegaplayer.util.StartupTrace
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

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
    initialTab: MediaTab? = null
) {
    remember { StartupTrace.mark("HomeScreen first composition"); true }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab ?: MediaTab.VIDEOS) }

    val showRecentHistoryOnHome by viewModel.showRecentHistoryOnHome.collectAsStateWithLifecycle()
    val showHistoryTab by viewModel.showHistoryTab.collectAsStateWithLifecycle()
    var showFullHistoryScreen by remember { mutableStateOf(false) }

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
            selectedTab = activeTabs[pagerState.currentPage]
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
        try {
            MediaSortOrder.valueOf(currentSortOrderName)
        } catch (_: IllegalArgumentException) {
            MediaSortOrder.DATE_DESC
        }
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

    var showExitConfirmDialog by remember { mutableStateOf(false) }
    val activity = context as? Activity
    BackHandler(enabled = isFocused) {
        if (showFullHistoryScreen) {
            showFullHistoryScreen = false
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

    val sortedVideoFolders = remember(videoFolders, searchQuery, selectedVideoFolder, currentSortOrder) {
        if (selectedVideoFolder != null) emptyList()
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

    val sortedAudioFolders = remember(audioFolders, searchQuery, selectedAudioFolder, currentSortOrder) {
        if (selectedAudioFolder != null) emptyList()
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

    val sortedVideos = remember(videosInFolder, searchQuery, selectedVideoFolder, currentSortOrder) {
        if (selectedVideoFolder == null) emptyList()
        else {
            val list = if (searchQuery.isEmpty()) videosInFolder
            else videosInFolder.filter { it.name.contains(searchQuery, ignoreCase = true) }
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
    
    val sortedAudios = remember(audiosInFolder, searchQuery, selectedAudioFolder, currentSortOrder) {
        if (selectedAudioFolder == null) emptyList()
        else {
            val list = if (searchQuery.isEmpty()) audiosInFolder
            else audiosInFolder.filter { it.name.contains(searchQuery, ignoreCase = true) }
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

    if (showAddToPlaylistDialog) {
        if (mediaPendingPlaylist != null) {
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
        selectedTab == MediaTab.VIDEOS -> if (selectedVideoFolder != null) sortedVideos.size else currentFolders.size
        selectedTab == MediaTab.AUDIOS -> if (selectedAudioFolder != null) sortedAudios.size else currentFolders.size
        selectedTab == MediaTab.PLAYLISTS -> sortedPlaylists.size
        selectedTab == MediaTab.HISTORY -> sortedHistory.size
        else -> currentFolders.size
    }

    val onHeaderBack: () -> Unit = {
        if (showFullHistoryScreen) {
            showFullHistoryScreen = false
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
    val onOpenHistory: () -> Unit = { showFullHistoryScreen = true }
    val onMediaPendingPlaylist: (String, String) -> Unit = { uri, type -> mediaPendingPlaylist = uri to type; showAddToPlaylistDialog = true }
    val onFolderPendingPlaylist: (String, String) -> Unit = { path, type -> folderPendingPlaylist = path to type; showAddToPlaylistDialog = true }
    val onFolderExclude: (String, Boolean) -> Unit = { path, isVideo ->
        if (isVideo) viewModel.excludeFolder(path) else audioViewModel.excludeFolder(path)
        Toast.makeText(context, context.getString(R.string.folder_excluded_toast), Toast.LENGTH_SHORT).show()
    }
    val onVideoDeleteRequest: (VideoModel) -> Unit = { selectedVideoForDelete = it }
    val onAudioDeleteRequest: (AudioModel) -> Unit = { selectedAudioForDelete = it }
    val onHistoryDeviceDelete: (RecentPlayback) -> Unit = { item ->
        val uriToDel = Uri.parse(item.uri)
        pendingUrisToDelete = listOf(uriToDel)
        com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(context = context, uris = listOf(uriToDel), deleteLauncher = deleteLauncher, onRequireInternalPopup = { selectedHistoryForDelete = item })
    }
    val onFolderDeviceDelete: (String, Boolean) -> Unit = { path, isVideo ->
        scope.launch {
            val uris = if (isVideo) viewModel.getVideosInFolder(path).map { it.uri } else audioViewModel.getAudiosInFolder(path).map { it.uri }
            if (uris.isNotEmpty()) {
                pendingUrisToDelete = uris
                com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(context = context, uris = uris, deleteLauncher = deleteLauncher, onRequireInternalPopup = { folderToDelete = path })
            }
        }
    }
    val onVideoDeviceDelete: (VideoModel) -> Unit = { video ->
        pendingUrisToDelete = listOf(video.uri)
        com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(context = context, uris = listOf(video.uri), deleteLauncher = deleteLauncher, onRequireInternalPopup = { selectedVideoForDelete = video })
    }
    val onAudioDeviceDelete: (AudioModel) -> Unit = { audio ->
        pendingUrisToDelete = listOf(audio.uri)
        com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(context = context, uris = listOf(audio.uri), deleteLauncher = deleteLauncher, onRequireInternalPopup = { selectedAudioForDelete = audio })
    }

    Scaffold(
        topBar = {
            HomeHeader(
                pagerState = pagerState,
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab },
                showHistoryTab = showHistoryTab,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
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
                viewModel = viewModel
            )
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

            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (sortedHistory.isEmpty()) {
                    EmptyState(searchQuery.isNotEmpty(), false)
                } else {
                    if (pageViewMode == 1 || pageViewMode == 2) {
                            val cols = if (pageViewMode == 1) 2 else 1
                            val ratio = if (pageViewMode == 1) 1f else (16f / 9f)
                            LazyVerticalGrid(columns = GridCells.Fixed(cols), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(sortedHistory, key = { it.uri }) { item ->
                                    HistoryGridCard(item = item, onClick = {
                                        val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                        viewModel.playHistory(sortedHistory, index, videos, audios, audiosByUri = audiosByUri)
                                        val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                        if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                    }, aspectRatio = ratio)
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(sortedHistory, key = { it.uri }) { item ->
                                    HistoryItem(item = item, onClick = {
                                        val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                        viewModel.playHistory(sortedHistory, index, videos, audios, audiosByUri = audiosByUri)
                                        val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                        if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                    }, onDelete = {
                                        viewModel.deleteHistoryItem(item.uri)
                                    }, onDeleteFromDevice = {
                                        val uriToDel = Uri.parse(item.uri)
                                        pendingUrisToDelete = listOf(uriToDel)
                                        com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
                                            context = context,
                                            uris = listOf(uriToDel),
                                            deleteLauncher = deleteLauncher,
                                            onRequireInternalPopup = { selectedHistoryForDelete = item }
                                        )
                                    })
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
                bottomPadding = bottomPadding,
                showRecentHistoryOnHome = showRecentHistoryOnHome,
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
                onVideoDeleteRequest = onVideoDeleteRequest,
                onAudioDeleteRequest = onAudioDeleteRequest,
                onHistoryDeviceDelete = onHistoryDeviceDelete,
                onFolderDeviceDelete = onFolderDeviceDelete,
                onVideoDeviceDelete = onVideoDeviceDelete,
                onAudioDeviceDelete = onAudioDeviceDelete
            )
    }
}
}
