package com.mohamedaminelouati.oplayer.ui.feature.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedaminelouati.oplayer.R

@Composable
fun SelectionFooterBar(
    onPlay: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onInformation: (() -> Unit)? = null,
    onToggleLock: (() -> Unit)? = null,
    onRemoveFromHistory: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String? = null,
    modifier: Modifier = Modifier,
    canPlay: Boolean = true,
    canPlayNext: Boolean = true,
    canMoveToTop: Boolean = true,
    canMoveToBottom: Boolean = true,
    canAddToPlaylist: Boolean = true,
    canRemoveFromPlaylist: Boolean = true,
    canRename: Boolean = true,
    canShare: Boolean = true,
    canInformation: Boolean = true,
    canToggleLock: Boolean = true,
    canRemoveFromHistory: Boolean = true,
    canRemoveFromQueue: Boolean = true,
    canDelete: Boolean = true,
    isLocked: Boolean = false,
    isFolder: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .widthIn(min = screenWidth)
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onPlay != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.PlayArrow,
                    label = stringResource(R.string.action_play),
                    enabled = canPlay,
                    onClick = onPlay
                )
            }
            if (onPlayNext != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.SkipNext,
                    label = stringResource(R.string.action_play_next),
                    enabled = canPlayNext,
                    onClick = onPlayNext
                )
            }
            if (onMoveToTop != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.VerticalAlignTop,
                    label = stringResource(R.string.action_move_to_top),
                    enabled = canMoveToTop,
                    onClick = onMoveToTop
                )
            }
            if (onMoveToBottom != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.VerticalAlignBottom,
                    label = stringResource(R.string.action_move_to_bottom),
                    enabled = canMoveToBottom,
                    onClick = onMoveToBottom
                )
            }
            if (onAddToPlaylist != null) {
                SelectionFooterActionItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    label = stringResource(R.string.menu_add_to_playlist),
                    enabled = canAddToPlaylist,
                    onClick = onAddToPlaylist
                )
            }
            if (onRemoveFromPlaylist != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.PlaylistRemove,
                    label = stringResource(R.string.menu_remove_from_playlist),
                    enabled = canRemoveFromPlaylist,
                    onClick = onRemoveFromPlaylist
                )
            }
            if (onRename != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.Edit,
                    label = stringResource(R.string.action_rename),
                    enabled = canRename,
                    onClick = onRename
                )
            }
            if (onShare != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.Share,
                    label = stringResource(R.string.action_share),
                    enabled = canShare,
                    onClick = onShare
                )
            }
            if (onInformation != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.menu_information),
                    enabled = canInformation,
                    onClick = onInformation
                )
            }
            if (onToggleLock != null) {
                val lockLabel = if (isFolder) {
                    if (isLocked) stringResource(R.string.action_unlock_folder) else stringResource(R.string.action_lock_folder)
                } else {
                    if (isLocked) stringResource(R.string.action_unlock_media) else stringResource(R.string.action_lock_media)
                }
                SelectionFooterActionItem(
                    icon = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    label = lockLabel,
                    enabled = canToggleLock,
                    onClick = onToggleLock
                )
            }
            if (onRemoveFromHistory != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.DeleteSweep,
                    label = stringResource(R.string.remove_from_history),
                    enabled = canRemoveFromHistory,
                    onClick = onRemoveFromHistory
                )
            }
            if (onRemoveFromQueue != null) {
                SelectionFooterActionItem(
                    icon = Icons.Default.PlaylistRemove,
                    label = stringResource(R.string.action_remove_from_queue),
                    enabled = canRemoveFromQueue,
                    onClick = onRemoveFromQueue
                )
            }
            if (onDelete != null) {
                val label = deleteLabel ?: if (isFolder) {
                    stringResource(R.string.menu_delete_folder)
                } else {
                    stringResource(R.string.delete_from_device)
                }
                SelectionFooterActionItem(
                    icon = Icons.Default.Delete,
                    label = label,
                    enabled = canDelete,
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SelectionFooterActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val alpha = if (enabled) 1f else 0.38f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .alpha(alpha)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = tint,
            maxLines = 1
        )
    }
}
