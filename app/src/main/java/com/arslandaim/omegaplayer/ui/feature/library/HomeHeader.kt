package com.arslandaim.omegaplayer.ui.feature.library

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslandaim.omegaplayer.data.Playlist
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel
import com.arslandaim.omegaplayer.data.MediaSortOrder
import com.arslandaim.omegaplayer.ui.feature.library.components.*
import androidx.compose.foundation.pager.PagerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeHeader(
    pagerState: PagerState,
    selectedTab: MediaTab,
    onTabSelected: (MediaTab) -> Unit,
    showHistoryTab: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isCurrentFolderOpen: Boolean,
    isNavPathOpen: Boolean,
    showFullHistoryScreen: Boolean,
    currentSelectedFolder: String?,
    selectedPlaylistForDetails: Playlist?,
    videoNavPath: String?,
    audioNavPath: String?,
    currentContextKey: String,
    currentSortOrder: MediaSortOrder,
    currentTabViewMode: Int,
    itemCount: Int,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: VideoViewModel
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)).statusBarsPadding()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    if (isCurrentFolderOpen || isNavPathOpen || showFullHistoryScreen) {
                        IconButton(modifier = Modifier.size(50.dp), onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp)) }
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
            HomeDashboard(pagerState = pagerState, selectedTab = selectedTab, onTabSelected = onTabSelected, showHistoryTab = showHistoryTab)
        }
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = searchQuery, onValueChange = { onSearchQueryChange(it) }, modifier = Modifier.weight(1f), placeholder = { Text(stringResource(R.string.search_placeholder), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) }, trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, null) } } }, shape = RoundedCornerShape(20.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)), singleLine = true, textStyle = MaterialTheme.typography.bodyLarge)
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
                        else -> Icons.AutoMirrored.Filled.ViewList
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
            Text(text = stringResource(R.string.items_count, itemCount), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
