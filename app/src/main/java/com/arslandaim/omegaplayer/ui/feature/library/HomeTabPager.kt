package com.arslandaim.omegaplayer.ui.feature.library

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.arslandaim.omegaplayer.util.StartupTrace
import kotlinx.coroutines.delay
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.unit.Dp
import com.arslandaim.omegaplayer.data.model.FolderNode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeTabPager(
    pagerState: PagerState,
    activeTabs: List<MediaTab>,
    isLoading: Boolean,
    searchQuery: String,
    bottomPadding: Dp,
    showRecentHistoryOnHome: Boolean,
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
    onVideoDeleteRequest: (VideoModel) -> Unit,
    onAudioDeleteRequest: (AudioModel) -> Unit,
    onHistoryDeviceDelete: (RecentPlayback) -> Unit,
    onFolderDeviceDelete: (String, Boolean) -> Unit,
    onVideoDeviceDelete: (VideoModel) -> Unit,
    onAudioDeviceDelete: (AudioModel) -> Unit
) {
    var preloadNeighborPages by remember { mutableStateOf(0) }
    LaunchedEffect(videos.isNotEmpty(), audios.isNotEmpty()) {
        if (videos.isEmpty() && audios.isEmpty()) return@LaunchedEffect
        delay(1200)
        preloadNeighborPages = 1
        StartupTrace.markOnce("pager.prefetch") { "pager neighbour prefetch enabled (idle)" }
    }
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(padding), beyondViewportPageCount = preloadNeighborPages, userScrollEnabled = currentSelectedFolder == null && selectedPlaylistForDetails == null) { page ->
        val pageTab = activeTabs.getOrNull(page) ?: activeTabs.first()
        remember(pageTab) { StartupTrace.markOnce("page.composed.${pageTab.name}") { "page composed: ${pageTab.name}" }; true }
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

        val pageSortedFolders = remember(pageTab, sortedVideoFolders, sortedAudioFolders) {
            if (pageTab == MediaTab.VIDEOS) sortedVideoFolders else if (pageTab == MediaTab.AUDIOS) sortedAudioFolders else emptyList()
        }

        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = {
                if (pageTab == MediaTab.VIDEOS) viewModel.manualRefresh()
                else audioViewModel.manualRefresh()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            if (currentSelectedFolder == null && selectedPlaylistForDetails == null && pageSortedFolders.isEmpty() && pageTab != MediaTab.PLAYLISTS && pageTab != MediaTab.HISTORY) {
                EmptyState(searchQuery.isNotEmpty(), true)
            } else if ((currentSelectedFolder != null || selectedPlaylistForDetails != null) && (if (pageTab == MediaTab.VIDEOS) sortedVideos.isEmpty() else if (pageTab == MediaTab.AUDIOS) sortedAudios.isEmpty() else sortedPlaylistItems.isEmpty())) {
                EmptyState(searchQuery.isNotEmpty(), false)
            } else {
                if (pageViewMode == 1 || pageViewMode == 2) {
                    val cols = if (pageViewMode == 1) 2 else 1
                    val ratio = if (pageViewMode == 1) 1f else (16f / 9f)
                    LazyVerticalGrid(columns = GridCells.Fixed(cols), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (currentSelectedFolder == null && selectedPlaylistForDetails == null) {
                            if (showRecentHistoryOnHome && pageTab != MediaTab.PLAYLISTS && pageTab != MediaTab.HISTORY && recentPlayback.isNotEmpty()) {
                                item(span = { GridItemSpan(cols) }) {
                                    RecentPlaybackSection(
                                        recentPlayback = recentPlayback,
                                        onItemClick = { item, index ->
                                            viewModel.playHistory(recentPlayback, index, videos, audios, audiosByUri = audiosByUri)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                            if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
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
                                        HistoryGridCard(item = item, onClick = {
                                            val index = sortedHistory.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                            viewModel.playHistory(sortedHistory, index, videos, audios, audiosByUri = audiosByUri)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                            if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                        }, aspectRatio = ratio)
                                    }
                                }
                            } else if (pageTab == MediaTab.PLAYLISTS) {
                                if (sortedPlaylists.isNotEmpty()) {
                                    items(sortedPlaylists, key = { it.id }) { playlist ->
                                        PlaylistGridCard(playlist = playlist, onClick = { onOpenPlaylist(playlist) }, onDelete = { audioViewModel.deletePlaylist(playlist) }, aspectRatio = ratio)
                                    }
                                }
                            } else {
                                items(pageSortedFolders, key = { it.path }) { folderNode ->
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
                                        onExclude = { onFolderExclude(folderNode.path, pageTab == MediaTab.VIDEOS) },
                                        onAddToPlaylist = { onFolderPendingPlaylist(folderNode.path, if (pageTab == MediaTab.VIDEOS) "video" else "audio") },
                                        aspectRatio = ratio
                                    )
                                }
                            }
                        } else if (selectedPlaylistForDetails != null) {
                            val currentPlaylist = selectedPlaylistForDetails!!
                            items(sortedPlaylistItems, key = { it.id }) { item ->
                                PlaylistGridItem(
                                    item = item,
                                    videosByUri = videosByUri,
                                    audiosByUri = audiosByUri,
                                    isPlaying = if (item.mediaType == "video") isVideoPlaying && activeVideoUri == item.mediaUri else isAudioPlaying && activeAudioUri == item.mediaUri,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onPlayItem = { clickedItem ->
                                        val index = sortedPlaylistItems.indexOfFirst { it.id == clickedItem.id }.coerceAtLeast(0)
                                        audioViewModel.playPlaylist(sortedPlaylistItems, index, videos, audios, videosByUri = videosByUri, audiosByUri = audiosByUri)
                                        val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(clickedItem.mediaUri)
                                        if (clickedItem.mediaType == "video") onVideoClick(encodedUri, -1L, "playlist") else onAudioClick(encodedUri, -1L, "playlist")
                                    },
                                    playlist = currentPlaylist,
                                    aspectRatio = ratio
                                )
                            }
                        } else if (pageTab == MediaTab.VIDEOS) {
                            items(sortedVideos, key = { it.id }) { video -> VideoGridItem(video, isVideoPlaying && activeVideoUri == video.uri.toString(), sharedTransitionScope, animatedVisibilityScope, { uri ->
                                val index = sortedVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                viewModel.playVideos(sortedVideos, index)
                                onVideoClick(uri, -1L, "folder")
                            }, { onVideoDeleteRequest(video) }, { onMediaPendingPlaylist(video.uri.toString(), "video") }, aspectRatio = ratio) }
                        } else {
                            items(sortedAudios, key = { it.id }) { audio -> AudioGridItem(audio, isAudioPlaying && activeAudioUri == audio.uri.toString(), { uri ->
                                val index = sortedAudios.indexOfFirst { it.id == audio.id }.coerceAtLeast(0)
                                audioViewModel.playAudios(sortedAudios, index)
                                onAudioClick(uri, -1L, "folder")
                            }, { onAudioDeleteRequest(audio) }, { onMediaPendingPlaylist(audio.uri.toString(), "audio") }, aspectRatio = ratio) }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + bottomPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (showRecentHistoryOnHome && currentSelectedFolder == null && selectedPlaylistForDetails == null && pageTab != MediaTab.PLAYLISTS && pageTab != MediaTab.HISTORY && recentPlayback.isNotEmpty()) {
                            item {
                                RecentPlaybackSection(
                                    recentPlayback = recentPlayback,
                                    onItemClick = { item, index ->
                                        viewModel.playHistory(recentPlayback, index, videos, audios, audiosByUri = audiosByUri)
                                        val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                        if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                    },
                                    onViewAllClick = {
                                        onOpenHistory()
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
                                            viewModel.playHistory(sortedHistory, index, videos, audios, audiosByUri = audiosByUri)
                                            val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(item.uri)
                                            if (item.mediaType == "video") onVideoClick(encodedUri, item.position, "history") else onAudioClick(encodedUri, item.position, "history")
                                        }, onDelete = {
                                            viewModel.deleteHistoryItem(item.uri)
                                        }, onDeleteFromDevice = { onHistoryDeviceDelete(item) })
                                    }
                                }
                            } else if (pageTab == MediaTab.PLAYLISTS) {
                                if (sortedPlaylists.isEmpty()) { item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_playlists_yet), color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                                else { items(sortedPlaylists, key = { it.id }) { playlist -> PlaylistListItem(playlist = playlist, onClick = { onOpenPlaylist(playlist) }, onDelete = { audioViewModel.deletePlaylist(playlist) }) } }
                            } else {
                                items(pageSortedFolders, key = { it.path }) { folderNode ->
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
                                        onDelete = { onFolderDeviceDelete(folderNode.path, pageTab == MediaTab.VIDEOS) },
                                        onExclude = { onFolderExclude(folderNode.path, pageTab == MediaTab.VIDEOS) },
                                        onAddToPlaylist = { onFolderPendingPlaylist(folderNode.path, if (pageTab == MediaTab.VIDEOS) "video" else "audio") }
                                    )
                                }
                            }
                        } else if (selectedPlaylistForDetails != null) {
                            val currentPlaylist = selectedPlaylistForDetails!!
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
                                        val index = sortedPlaylistItems.indexOfFirst { it.id == clickedItem.id }.coerceAtLeast(0)
                                        audioViewModel.playPlaylist(sortedPlaylistItems, index, videos, audios, videosByUri = videosByUri, audiosByUri = audiosByUri)
                                        val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(clickedItem.mediaUri)
                                        if (clickedItem.mediaType == "video") onVideoClick(encodedUri, -1L, "playlist") else onAudioClick(encodedUri, -1L, "playlist")
                                    },
                                    playlist = currentPlaylist,
                                    onVideoDelete = { video -> onVideoDeviceDelete(video) },
                                    onAudioDelete = { audio -> onAudioDeviceDelete(audio) }
                                )
                            }
                        } else if (pageTab == MediaTab.VIDEOS) {
                            items(sortedVideos, key = { it.id }) { video -> VideoListItem(video = video, isPlaying = isVideoPlaying && activeVideoUri == video.uri.toString(), sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, onClick = {
                                val index = sortedVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                viewModel.playVideos(sortedVideos, index)
                                val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(video.uri.toString()); onVideoClick(encodedUri, -1L, "folder")
                            }, onDeleteClick = { onVideoDeviceDelete(video) }, onPlaylistClick = { onMediaPendingPlaylist(video.uri.toString(), "video") }) }
                        } else {
                            items(sortedAudios, key = { it.id }) { audio -> AudioListItem(audio = audio, isPlaying = isAudioPlaying && activeAudioUri == audio.uri.toString(), onClick = {
                                val index = sortedAudios.indexOfFirst { it.id == audio.id }.coerceAtLeast(0)
                                audioViewModel.playAudios(sortedAudios, index)
                                val encodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeEncodeUri(audio.uri.toString()); onAudioClick(encodedUri, -1L, "folder")
                            }, onDeleteClick = { onAudioDeviceDelete(audio) }, onPlaylistClick = { onMediaPendingPlaylist(audio.uri.toString(), "audio") }) }
                        }
                    }
                }
        }
    }
    }
}
