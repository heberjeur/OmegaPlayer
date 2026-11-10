package com.mohamedaminelouati.oplayer.ui.feature.library.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mohamedaminelouati.oplayer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.mohamedaminelouati.oplayer.ui.theme.LocalTextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.maxLines
import com.mohamedaminelouati.oplayer.ui.theme.overflow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListItem(
    name: String,
    path: String,
    count: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPlay: (() -> Unit)? = null,
    onExclude: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onRename: (String) -> Unit = {},
    isLocked: Boolean = false,
    onToggleLock: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showInfo) {
        FolderInfoDialog(name = name, path = path, count = count, onDismiss = { showInfo = false })
    }

    if (showRenameDialog) {
        MediaRenameDialog(
            currentName = name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { onRename(it) }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect() else onClick()
                },
                onLongClick = onToggleSelect
            ),
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                if (isLocked) {
                    Surface(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
            
            val truncation = LocalTextTruncationMode.current
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = truncation.maxLines,
                        overflow = truncation.overflow,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.items_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelectionMode) {
                IconButton(onClick = onToggleSelect) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onPlay != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_play)) },
                            onClick = {
                                showMenu = false
                                onPlay()
                            },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_to_playlist)) },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share)) },
                        onClick = {
                            showMenu = false
                            android.widget.Toast.makeText(context, context.getString(R.string.error_folder_share), android.widget.Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_information)) },
                        onClick = {
                            showMenu = false
                            showInfo = true
                        },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isLocked) stringResource(R.string.action_unlock_folder) else stringResource(R.string.action_lock_folder)) },
                        onClick = {
                            showMenu = false
                            onToggleLock()
                        },
                        leadingIcon = { Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_delete_folder)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridItem(
    name: String,
    path: String,
    count: Int,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null,
    onExclude: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRename: ((String) -> Unit)? = null,
    isLocked: Boolean = false,
    onToggleLock: (() -> Unit)? = null,
    aspectRatio: Float = 1f,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showInfo) {
        FolderInfoDialog(name = name, path = path, count = count, onDismiss = { showInfo = false })
    }

    if (showRenameDialog) {
        MediaRenameDialog(
            currentName = name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { onRename?.invoke(it) }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect() else onClick()
                },
                onLongClick = onToggleSelect
            ),
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLocked) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(24.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            if (isSelectionMode) {
                IconButton(
                    onClick = onToggleSelect,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (onPlay != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_play)) },
                                onClick = {
                                    showMenu = false
                                    onPlay()
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_add_to_playlist)) },
                                onClick = {
                                    showMenu = false
                                    onAddToPlaylist()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            onClick = {
                                showMenu = false
                                showRenameDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = {
                                showMenu = false
                                android.widget.Toast.makeText(context, context.getString(R.string.error_folder_share), android.widget.Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_information)) },
                            onClick = {
                                showMenu = false
                                showInfo = true
                            },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        if (onToggleLock != null) {
                            DropdownMenuItem(
                                text = { Text(if (isLocked) stringResource(R.string.action_unlock_folder) else stringResource(R.string.action_lock_folder)) },
                                onClick = {
                                    showMenu = false
                                    onToggleLock()
                                },
                                leadingIcon = { Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_delete_folder)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    if (isLocked) {
                        Surface(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val truncation = LocalTextTruncationMode.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = truncation.maxLines,
                        overflow = truncation.overflow
                    )
                }
                Text(
                    text = stringResource(R.string.items_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderInfoDialog(name: String, path: String, count: Int, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.menu_information),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_close),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(stringResource(R.string.action_close), color = Color.White)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.menu_information),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(
                                        text = stringResource(R.string.info_name),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        softWrap = true
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(
                                        text = stringResource(R.string.info_path),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = path,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        softWrap = true
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(
                                        text = stringResource(R.string.items_count, count),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
