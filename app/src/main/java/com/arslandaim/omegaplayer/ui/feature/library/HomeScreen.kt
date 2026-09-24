package com.arslandaim.omegaplayer.ui.feature.library

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab ?: MediaTab.VIDEOS) }

    val showRecentHistoryOnHome by viewModel.showRecentHistoryOnHome.collectAsStateWithLifecycle()
    val showHistoryTab by viewModel.showHistoryTab.collectAsStateWithLifecycle()
    val fullHistory by viewModel.fullHistory.collectAsStateWithLifecycle()
    var showFilterMenu by remember { mutableStateOf(false) }
    var showFullHistoryScreen by remember { mutableStateOf(false) }

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
        } catch (e: Exception) {
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

    val sortedFolders = remember(currentFolders, searchQuery, currentSelectedFolder, currentSortOrder) {
        if (currentSelectedFolder != null) emptyList()
        else {
            val baseList = if (searchQuery.isEmpty()) currentFolders
            else currentFolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
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

    val sortedVideos = remember(videosInFolder, searchQuery, selectedVideoFolder, selectedTab, currentSortOrder) {
        if (selectedVideoFolder == null || selectedTab != MediaTab.VIDEOS) emptyList()
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
    
    val sortedAudios = remember(audiosInFolder, searchQuery, selectedAudioFolder, selectedTab, currentSortOrder) {
        if (selectedAudioFolder == null || selectedTab != MediaTab.AUDIOS) emptyList()
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

    val sortedPlaylistItems = remember(playlistItems, searchQuery, currentSortOrder, videos, audios) {
        fun getItemName(item: PlaylistItem): String {
            return if (item.mediaType == "video") {
                videos.find { it.uri.toString() == item.mediaUri }?.name ?: ""
            } else {
                audios.find { it.uri.toString() == item.mediaUri }?.name ?: ""
            }
        }
        val validItems = playlistItems.filter { item ->
            if (item.mediaType == "video") videos.any { it.uri.toString() == item.mediaUri }
            else audios.any { it.uri.toString() == item.mediaUri }
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
                pendingUrisToDelete.forEach { uri ->
                    viewModel.deleteHistoryItem(uri.toString())
                }
                viewModel.stopIfPlaying(pendingUrisToDelete)
                audioViewModel.stopIfPlaying(pendingUrisToDelete)
                viewModel.refreshVideos(context)
                audioViewModel.refreshAudios(context)
                pendingUrisToDelete = emptyList()
                isProcessing = false
            }
        } else {
            pendingUrisToDelete = emptyList()
            isProcessing = false
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
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

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(text = if (selectedTab == MediaTab.VIDEOS) "Delete Video Folder" else "Delete Audio Folder", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to delete folder '${folderToDelete}' and all its items?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = {
                    val folderName = folderToDelete!!
                    folderToDelete = null
                    scope.launch {
                        isProcessing = true
                        if (selectedTab == MediaTab.VIDEOS) {
                            val videosToDelete = viewModel.getVideosInFolder(folderName)
                            if (videosToDelete.isNotEmpty()) {
                                videosToDelete.forEach { viewModel.deleteHistoryItem(it.uri.toString()) }
                                viewModel.stopIfPlaying(videosToDelete.map { it.uri })
                                videosToDelete.forEach { context.contentResolver.delete(it.uri, null, null) }
                                viewModel.refreshVideos(context)
                                isProcessing = false
                            } else isProcessing = false
                        } else {
                            val audiosToDelete = audioViewModel.getAudiosInFolder(folderName)
                            if (audiosToDelete.isNotEmpty()) {
                                audiosToDelete.forEach { audioViewModel.deleteHistoryItem(it.uri.toString()) }
                                audioViewModel.stopIfPlaying(audiosToDelete.map { it.uri })
                                audiosToDelete.forEach { context.contentResolver.delete(it.uri, null, null) }
                                audioViewModel.refreshAudios(context)
                                isProcessing = false
                            } else isProcessing = false
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { folderToDelete = null }) { Text("Cancel") } }
        )
    }

    if (selectedVideoForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedVideoForDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(text = "Delete Video", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to delete '${selectedVideoForDelete?.name}'?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = {
                    val video = selectedVideoForDelete!!
                    selectedVideoForDelete = null
                    scope.launch {
                        isProcessing = true
                        viewModel.deleteHistoryItem(video.uri.toString())
                        viewModel.stopIfPlaying(video.uri)
                        context.contentResolver.delete(video.uri, null, null)
                        viewModel.refreshVideos(context)
                        isProcessing = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { selectedVideoForDelete = null }) { Text("Cancel") } }
        )
    }

    if (selectedAudioForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedAudioForDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(text = "Delete Audio", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to delete '${selectedAudioForDelete?.name}'?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = {
                    val audio = selectedAudioForDelete!!
                    selectedAudioForDelete = null
                    scope.launch {
                        isProcessing = true
                        audioViewModel.deleteHistoryItem(audio.uri.toString())
                        audioViewModel.stopIfPlaying(audio.uri)
                        context.contentResolver.delete(audio.uri, null, null)
                        audioViewModel.refreshAudios(context)
                        isProcessing = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { selectedAudioForDelete = null }) { Text("Cancel") } }
        )
    }

    if (selectedHistoryForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedHistoryForDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(text = "Delete Media", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to delete '${selectedHistoryForDelete?.name}' from your device?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = {
                    val item = selectedHistoryForDelete!!
                    selectedHistoryForDelete = null
                    scope.launch {
                        isProcessing = true
                        viewModel.deleteHistoryItem(item.uri)
                        val uriToDel = Uri.parse(item.uri)
                        if (item.mediaType == "video") viewModel.stopIfPlaying(uriToDel) else audioViewModel.stopIfPlaying(uriToDel)
                        context.contentResolver.delete(uriToDel, null, null)
                        viewModel.refreshVideos(context)
                        audioViewModel.refreshAudios(context)
                        isProcessing = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { selectedHistoryForDelete = null }) { Text("Cancel") } }
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
                        Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Added folder to playlist", Toast.LENGTH_SHORT).show()
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

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            if (isCurrentFolderOpen || isNavPathOpen || showFullHistoryScreen) {
                                IconButton(modifier = Modifier.size(50.dp), onClick = { 
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
                                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp)) }
                                Spacer(modifier = Modifier.width(4.dp))
                                val folderName = if (showFullHistoryScreen) stringResource(R.string.tab_history) else {
                                    currentSelectedFolder?.split("/")?.lastOrNull { it.isNotEmpty() } 
                                        ?: (if (selectedTab == MediaTab.VIDEOS) videoNavPath else audioNavPath)?.split("/")?.lastOrNull { it.isNotEmpty() }
                                        ?: selectedPlaylistForDetails?.name 
                                        ?: ""
                                }
                                Text(text = folderName, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(onClick = onSettingsClick)
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                                ) {
                                    ModernOmegaIcon()
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.secondary,
                                                    MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (currentSelectedFolder == null && selectedPlaylistForDetails == null && !showFullHistoryScreen) {
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.nav_settings),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                if (currentSelectedFolder == null && selectedPlaylistForDetails == null && !showFullHistoryScreen) {
                    HomeDashboard(pagerState = pagerState, selectedTab = selectedTab, onTabSelected = { tab -> selectedTab = tab }, showHistoryTab = showHistoryTab)
                }
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f), placeholder = { Text(stringResource(R.string.search_placeholder), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) }, trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } } }, shape = RoundedCornerShape(20.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)), singleLine = true, textStyle = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        IconButton(
                            onClick = { showFilterMenu = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.action_filter), tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            MediaSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    onClick = {
                                        showFilterMenu = false
                                        viewModel.setFolderSortOrder(currentContextKey, order.name)
                                    },
                                    trailingIcon = {
                                        if (currentSortOrder == order) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val nextMode = (currentTabViewMode + 1) % 3
                            viewModel.setFolderViewMode(currentContextKey, nextMode)
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = when (currentTabViewMode) {
                                1 -> Icons.Default.GridView
                                2 -> Icons.Default.ViewAgenda
                                else -> Icons.Default.ViewList
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (selectedTab == MediaTab.HISTORY || showFullHistoryScreen) {
                        Spacer(modifier = Modifier.width(8.dp))
                        var showClearConfirm by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.clear_all), tint = MaterialTheme.colorScheme.primary)
                        }

                        if (showClearConfirm) {
                            AlertDialog(
                                onDismissRequest = { showClearConfirm = false },
                                title = { Text(stringResource(R.string.clear_all)) },
                                text = { Text(stringResource(R.string.clear_history_msg)) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.clearAllHistory()
                                        showClearConfirm = false
                                    }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(android.R.string.cancel)) }
                                }
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val sectionLabel = if (showFullHistoryScreen) {
                        stringResource(R.string.tab_history)
                    } else if (currentSelectedFolder == null && selectedPlaylistForDetails == null) {
                        when (selectedTab) {
                            MediaTab.VIDEOS -> stringResource(R.string.tab_videos)
                            MediaTab.AUDIOS -> stringResource(R.string.tab_audios)
                            MediaTab.PLAYLISTS -> stringResource(R.string.tab_playlists)
                            MediaTab.HISTORY -> stringResource(R.string.tab_history)
                        }
                    } else if (selectedPlaylistForDetails != null) stringResource(R.string.tab_playlists) else if (selectedTab == MediaTab.VIDEOS) stringResource(R.string.tab_videos) else stringResource(R.string.tab_audios)
                    Text(text = sectionLabel, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                    val currentItemCount = when {
                        showFullHistoryScreen -> sortedHistory.size
                        selectedPlaylistForDetails != null -> sortedPlaylistItems.size
                        selectedTab == MediaTab.VIDEOS -> if (selectedVideoFolder != null) sortedVideos.size else currentFolders.size
                        selectedTab == MediaTab.AUDIOS -> if (selectedAudioFolder != null) sortedAudios.size else currentFolders.size
                        selectedTab == MediaTab.PLAYLISTS -> sortedPlaylists.size
                        selectedTab == MediaTab.HISTORY -> sortedHistory.size
                        else -> currentFolders.size
                    }
                    Text(text = stringResource(R.string.items_count, currentItemCount), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        floatingActionButton = {
            if (!hasPermission) {
                ExtendedFloatingActionButton(modifier = Modifier.padding(bottom = 80.dp), text = { Text("Grant Access") }, icon = { Icon(Icons.Default.AddCircle, null) }, onClick = { launcher.launch(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)) }, containerColor = Color.White, contentColor = Color(0xFF1A1A1A), shape = RoundedCornerShape(16.dp))
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
                    androidx.compose.animation.Crossfade(targetState = pageViewMode, label = "ViewModeAnimationHistory") { mode ->
                        if (mode == 1 || mode == 2) {
                            val cols = if (mode == 1) 2 else 1
                            val ratio = if (mode == 1) 1f else (16f / 9f)
                            LazyVerticalGrid(columns = GridCells.Fixed(cols), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(sortedHistory, key = { it.uri }) { item ->
                                    HistoryGridCard(item = item, onClick = {
                                        val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                        viewModel.playHistory(sortedHistory, index, videos, audios)
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
                                        viewModel.playHistory(sortedHistory, index, videos, audios)
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
            }
        } else {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(padding), beyondBoundsPageCount = 1, userScrollEnabled = currentSelectedFolder == null && selectedPlaylistForDetails == null) { page ->
            val pageTab = activeTabs.getOrNull(page) ?: activeTabs.first()
            val pageContextKey = when {
                pageTab == MediaTab.VIDEOS && selectedVideoFolder != null -> "folder_video_$selectedVideoFolder"
                pageTab == MediaTab.VIDEOS -> "tab_videos"
                pageTab == MediaTab.AUDIOS && selectedAudioFolder != null -> "folder_audio_$selectedAudioFolder"
                pageTab == MediaTab.AUDIOS -> "tab_audios"
                pageTab == MediaTab.PLAYLISTS && selectedPlaylistForDetails != null -> "playlist_${selectedPlaylistForDetails?.id}"
                pageTab == MediaTab.PLAYLISTS -> "tab_playlists"
                pageTab == MediaTab.HISTORY -> "tab_history"
                else -> "default"
            }
            val pageViewMode by remember(pageContextKey) {
                viewModel.getFolderViewMode(pageContextKey)
            }.collectAsStateWithLifecycle(initialValue = viewModel.defaultViewMode.value)

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && (if (pageTab == MediaTab.VIDEOS) videos.isEmpty() else audios.isEmpty())) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (currentSelectedFolder == null && selectedPlaylistForDetails == null && sortedFolders.isEmpty() && pageTab != MediaTab.PLAYLISTS && pageTab != MediaTab.HISTORY) {
                    EmptyState(searchQuery.isNotEmpty(), true)
                } else if ((currentSelectedFolder != null || selectedPlaylistForDetails != null) && (if (pageTab == MediaTab.VIDEOS) sortedVideos.isEmpty() else if (pageTab == MediaTab.AUDIOS) sortedAudios.isEmpty() else sortedPlaylistItems.isEmpty())) {
                    EmptyState(searchQuery.isNotEmpty(), false)
                } else {
                    androidx.compose.animation.Crossfade(targetState = pageViewMode, label = "ViewModeAnimation") { mode ->
                        if (mode == 1 || mode == 2) {
                        val cols = if (pageViewMode == 1) 2 else 1
                        val ratio = if (pageViewMode == 1) 1f else (16f / 9f)
                        LazyVerticalGrid(columns = GridCells.Fixed(cols), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (currentSelectedFolder == null && selectedPlaylistForDetails == null) {
                                if (showRecentHistoryOnHome && pageTab != MediaTab.PLAYLISTS && pageTab != MediaTab.HISTORY && recentPlayback.isNotEmpty()) {
                                    item(span = { GridItemSpan(cols) }) {
                                        RecentPlaybackSection(
                                            recentPlayback = recentPlayback,
                                            onItemClick = { item, index ->
                                                viewModel.playHistory(recentPlayback, index, videos, audios)
                                                val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                                if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                            },
                                            onViewAllClick = {
                                                showFullHistoryScreen = true
                                            }
                                        )
                                    }
                                }
                                if (pageTab == MediaTab.HISTORY) {
                                    if (sortedHistory.isEmpty()) {
                                        item(span = { GridItemSpan(cols) }) { EmptyState(searchQuery.isNotEmpty(), false) }
                                    } else {
                                        items(sortedHistory, key = { it.uri }) { item ->
                                            HistoryGridCard(item = item, onClick = {
                                                val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                                viewModel.playHistory(sortedHistory, index, videos, audios)
                                                val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                                if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                            }, aspectRatio = ratio)
                                        }
                                    }
                                } else if (pageTab == MediaTab.PLAYLISTS) {
                                    if (sortedPlaylists.isNotEmpty()) {
                                        items(sortedPlaylists, key = { it.id }) { playlist ->
                                            PlaylistGridCard(playlist = playlist, onClick = { selectedPlaylistForDetails = playlist }, onDelete = { audioViewModel.deletePlaylist(playlist) }, aspectRatio = ratio)
                                        }
                                    }
                                } else {
                                    items(sortedFolders, key = { it.path }) { folderNode ->
                                        FolderGridItem(
                                            name = folderNode.name,
                                            count = folderNode.videoCount,
                                            onClick = { 
                                                if (folderNode.subFolders.isNotEmpty() && !folderNode.isFlattened) {
                                                    if (pageTab == MediaTab.VIDEOS) viewModel.navigateIntoFolder(folderNode.path)
                                                    else audioViewModel.navigateIntoFolder(folderNode.path)
                                                } else {
                                                    if (pageTab == MediaTab.VIDEOS) viewModel.setSelectedFolder(folderNode.path)
                                                    else audioViewModel.setSelectedFolder(folderNode.path)
                                                }
                                            },
                                            onExclude = {
                                                if (pageTab == MediaTab.VIDEOS) viewModel.excludeFolder(folderNode.path) else audioViewModel.excludeFolder(folderNode.path)
                                                Toast.makeText(context, context.getString(R.string.folder_excluded_toast), Toast.LENGTH_SHORT).show()
                                            },
                                            onAddToPlaylist = {
                                                folderPendingPlaylist = folderNode.path to if (pageTab == MediaTab.VIDEOS) "video" else "audio"
                                                showAddToPlaylistDialog = true
                                            },
                                            aspectRatio = ratio
                                        )
                                    }
                                }
                            } else if (selectedPlaylistForDetails != null) {
                                val currentPlaylist = selectedPlaylistForDetails!!
                                items(sortedPlaylistItems, key = { it.id }) { item ->
                                    PlaylistGridItem(
                                        item = item,
                                        videos = videos,
                                        audios = audios,
                                        videoViewModel = viewModel,
                                        audioViewModel = audioViewModel,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onPlayItem = { clickedItem ->
                                            val index = sortedPlaylistItems.indexOfFirst { it.id == clickedItem.id }.coerceAtLeast(0)
                                            audioViewModel.playPlaylist(sortedPlaylistItems, index, videos, audios)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(clickedItem.mediaUri)
                                            if (clickedItem.mediaType == "video") onVideoClick(encodedUri, -1L, "playlist") else onAudioClick(encodedUri, -1L, "playlist")
                                        },
                                        playlist = currentPlaylist,
                                        aspectRatio = ratio
                                    )
                                }
                            } else if (pageTab == MediaTab.VIDEOS) {
                                items(sortedVideos, key = { it.id }) { video -> VideoGridItem(video, viewModel, sharedTransitionScope, animatedVisibilityScope, { uri -> 
                                    val index = sortedVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                    viewModel.playVideos(sortedVideos, index)
                                    onVideoClick(uri, -1L, "folder") 
                                }, { selectedVideoForDelete = video }, { mediaPendingPlaylist = video.uri.toString() to "video"; showAddToPlaylistDialog = true }, aspectRatio = ratio) }
                            } else {
                                items(sortedAudios, key = { it.id }) { audio -> AudioGridItem(audio, audioViewModel, { uri -> 
                                    val index = sortedAudios.indexOfFirst { it.id == audio.id }.coerceAtLeast(0)
                                    audioViewModel.playAudios(sortedAudios, index)
                                    onAudioClick(uri, -1L, "folder") 
                                }, { selectedAudioForDelete = audio }, { mediaPendingPlaylist = audio.uri.toString() to "audio"; showAddToPlaylistDialog = true }, aspectRatio = ratio) }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (showRecentHistoryOnHome && currentSelectedFolder == null && selectedPlaylistForDetails == null && pageTab != MediaTab.PLAYLISTS && pageTab != MediaTab.HISTORY && recentPlayback.isNotEmpty()) {
                                item {
                                    RecentPlaybackSection(
                                        recentPlayback = recentPlayback,
                                        onItemClick = { item, index ->
                                            viewModel.playHistory(recentPlayback, index, videos, audios)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                            if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                        },
                                        onViewAllClick = {
                                            showFullHistoryScreen = true
                                        }
                                    )
                                }
                            }
                            if (currentSelectedFolder == null && selectedPlaylistForDetails == null) {
                                if (pageTab == MediaTab.HISTORY) {
                                    if (sortedHistory.isEmpty()) {
                                        item { EmptyState(searchQuery.isNotEmpty(), false) }
                                    } else {
                                        items(sortedHistory, key = { it.uri }) { item ->
                                            HistoryItem(item = item, onClick = {
                                                val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                                viewModel.playHistory(sortedHistory, index, videos, audios)
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
                                } else if (pageTab == MediaTab.PLAYLISTS) {
                                    if (sortedPlaylists.isEmpty()) { item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("No playlists yet", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                                    else { items(sortedPlaylists, key = { it.id }) { playlist -> PlaylistListItem(playlist = playlist, onClick = { selectedPlaylistForDetails = playlist }, onDelete = { audioViewModel.deletePlaylist(playlist) }) } }
                                } else {
                                    items(sortedFolders, key = { it.path }) { folderNode ->
                                        FolderListItem(
                                            name = folderNode.name,
                                            count = folderNode.videoCount,
                                            onClick = { 
                                                if (folderNode.subFolders.isNotEmpty() && !folderNode.isFlattened) {
                                                    if (pageTab == MediaTab.VIDEOS) viewModel.navigateIntoFolder(folderNode.path)
                                                    else audioViewModel.navigateIntoFolder(folderNode.path)
                                                } else {
                                                    if (pageTab == MediaTab.VIDEOS) viewModel.setSelectedFolder(folderNode.path)
                                                    else audioViewModel.setSelectedFolder(folderNode.path)
                                                }
                                            },
                                            onDelete = { 
                                                scope.launch {
                                                    val uris = if (pageTab == MediaTab.VIDEOS) viewModel.getVideosInFolder(folderNode.path).map { it.uri }
                                                               else audioViewModel.getAudiosInFolder(folderNode.path).map { it.uri }
                                                    if (uris.isNotEmpty()) {
                                                        pendingUrisToDelete = uris
                                                        com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
                                                            context = context,
                                                            uris = uris,
                                                            deleteLauncher = deleteLauncher,
                                                            onRequireInternalPopup = { folderToDelete = folderNode.path }
                                                        )
                                                    }
                                                }
                                            },
                                            onExclude = {
                                                if (pageTab == MediaTab.VIDEOS) viewModel.excludeFolder(folderNode.path) else audioViewModel.excludeFolder(folderNode.path)
                                                Toast.makeText(context, context.getString(R.string.folder_excluded_toast), Toast.LENGTH_SHORT).show()
                                            },
                                            onAddToPlaylist = {
                                                folderPendingPlaylist = folderNode.path to if (pageTab == MediaTab.VIDEOS) "video" else "audio"
                                                showAddToPlaylistDialog = true
                                            }
                                        )
                                    }
                                }
                            } else if (selectedPlaylistForDetails != null) {
                                val currentPlaylist = selectedPlaylistForDetails!!
                                items(sortedPlaylistItems, key = { it.id }) { item ->
                                    MediaListItemInPlaylist(
                                        item = item,
                                        videos = videos,
                                        audios = audios,
                                        videoViewModel = viewModel,
                                        audioViewModel = audioViewModel,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onPlayItem = { clickedItem ->
                                            val index = sortedPlaylistItems.indexOfFirst { it.id == clickedItem.id }.coerceAtLeast(0)
                                            audioViewModel.playPlaylist(sortedPlaylistItems, index, videos, audios)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(clickedItem.mediaUri)
                                            if (clickedItem.mediaType == "video") onVideoClick(encodedUri, -1L, "playlist") else onAudioClick(encodedUri, -1L, "playlist")
                                        },
                                        playlist = currentPlaylist,
                                        onVideoDelete = { video -> 
                                            pendingUrisToDelete = listOf(video.uri)
                                            com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
                                                context = context,
                                                uris = listOf(video.uri),
                                                deleteLauncher = deleteLauncher,
                                                onRequireInternalPopup = { selectedVideoForDelete = video }
                                            )
                                        },
                                        onAudioDelete = { audio -> 
                                            pendingUrisToDelete = listOf(audio.uri)
                                            com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
                                                context = context,
                                                uris = listOf(audio.uri),
                                                deleteLauncher = deleteLauncher,
                                                onRequireInternalPopup = { selectedAudioForDelete = audio }
                                            )
                                        }
                                    )
                                }
                            } else if (pageTab == MediaTab.VIDEOS) {
                                items(sortedVideos, key = { it.id }) { video -> VideoListItem(video = video, isPlaying = viewModel.activeVideoUri.collectAsState().value == video.uri.toString() && viewModel.isPlaying.collectAsState().value, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, onClick = { 
                                    val index = sortedVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                    viewModel.playVideos(sortedVideos, index)
                                    val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(video.uri.toString()); onVideoClick(encodedUri, -1L, "folder") 
                                }, onDeleteClick = { 
                                    pendingUrisToDelete = listOf(video.uri)
                                    com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
                                        context = context,
                                        uris = listOf(video.uri),
                                        deleteLauncher = deleteLauncher,
                                        onRequireInternalPopup = { selectedVideoForDelete = video }
                                    )
                                }, onPlaylistClick = { mediaPendingPlaylist = video.uri.toString() to "video"; showAddToPlaylistDialog = true }) }
                            } else {
                                items(sortedAudios, key = { it.id }) { audio -> AudioListItem(audio = audio, isPlaying = audioViewModel.activeAudioUri.collectAsState().value == audio.uri.toString() && audioViewModel.isPlaying.collectAsStateWithLifecycle().value, onClick = { 
                                    val index = sortedAudios.indexOfFirst { it.id == audio.id }.coerceAtLeast(0)
                                    audioViewModel.playAudios(sortedAudios, index)
                                    val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(audio.uri.toString()); onAudioClick(encodedUri, -1L, "folder") 
                                }, onDeleteClick = { 
                                    pendingUrisToDelete = listOf(audio.uri)
                                    com.arslandaim.omegaplayer.util.MediaUtils.requestMediaDelete(
                                        context = context,
                                        uris = listOf(audio.uri),
                                        deleteLauncher = deleteLauncher,
                                        onRequireInternalPopup = { selectedAudioForDelete = audio }
                                    )
                                }, onPlaylistClick = { mediaPendingPlaylist = audio.uri.toString() to "audio"; showAddToPlaylistDialog = true }) }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
}
