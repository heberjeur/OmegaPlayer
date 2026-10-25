package com.arslandaim.omegaplayer.ui.feature.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.data.SecurityLockType
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinConfirmed: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var firstPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }

                if (step == 1) {
                    PinLockView(
                        title = stringResource(R.string.dialog_setup_pin_title),
                        subtitle = stringResource(R.string.pin_too_short),
                        errorMessage = errorMessage,
                        onPinComplete = { pin ->
                            firstPin = pin
                            errorMessage = null
                            step = 2
                        },
                        onClearError = { errorMessage = null }
                    )
                } else {
                    PinLockView(
                        title = stringResource(R.string.dialog_confirm_pin_title),
                        errorMessage = errorMessage,
                        onPinComplete = { pin ->
                            if (pin == firstPin) {
                                onPinConfirmed(pin)
                                onDismiss()
                            } else {
                                errorMessage = context.getString(R.string.pin_mismatch)
                                step = 1
                                firstPin = ""
                            }
                        },
                        onClearError = { errorMessage = null }
                    )
                }
            }
        }
    }
}

@Composable
fun PatternSetupDialog(
    onDismiss: () -> Unit,
    onPatternConfirmed: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var firstPattern by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }

                if (step == 1) {
                    PatternLockView(
                        title = stringResource(R.string.dialog_setup_pattern_title),
                        subtitle = stringResource(R.string.pattern_too_short),
                        errorMessage = errorMessage,
                        onPatternComplete = { pattern ->
                            val dotsCount = pattern.split("-").size
                            if (dotsCount < 4) {
                                errorMessage = context.getString(R.string.pattern_too_short)
                            } else {
                                firstPattern = pattern
                                errorMessage = null
                                step = 2
                            }
                        },
                        onClearError = { errorMessage = null }
                    )
                } else {
                    PatternLockView(
                        title = stringResource(R.string.dialog_confirm_pattern_title),
                        errorMessage = errorMessage,
                        onPatternComplete = { pattern ->
                            if (pattern == firstPattern) {
                                onPatternConfirmed(pattern)
                                onDismiss()
                            } else {
                                errorMessage = context.getString(R.string.pattern_mismatch)
                                step = 1
                                firstPattern = ""
                            }
                        },
                        onClearError = { errorMessage = null }
                    )
                }
            }
        }
    }
}

@Composable
fun ManageLockedItemsDialog(
    videoViewModel: VideoViewModel,
    onDismiss: () -> Unit
) {
    val lockedFolders by videoViewModel.securityLockedFolders.collectAsState()
    val lockedMedia by videoViewModel.securityLockedMedia.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.manage_locked_items),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (lockedFolders.isEmpty() && lockedMedia.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_locked_items),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (lockedFolders.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.locked_folders_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(lockedFolders.toList(), key = { "folder_$it" }) { folderPath ->
                                val folderName = folderPath.substringAfterLast("/")
                                ListItem(
                                    headlineContent = { Text(folderName, fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text(folderPath, style = MaterialTheme.typography.bodySmall) },
                                    leadingContent = {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { videoViewModel.unlockFolder(folderPath) }) {
                                            Icon(
                                                Icons.Default.LockOpen,
                                                contentDescription = stringResource(R.string.action_unlock),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                )
                            }
                        }

                        if (lockedMedia.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.locked_media_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(lockedMedia.toList(), key = { "media_$it" }) { mediaUri ->
                                val fileName = mediaUri.substringAfterLast("/")
                                ListItem(
                                    headlineContent = { Text(fileName, fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text(mediaUri, style = MaterialTheme.typography.bodySmall) },
                                    leadingContent = {
                                        Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { videoViewModel.unlockMedia(mediaUri) }) {
                                            Icon(
                                                Icons.Default.LockOpen,
                                                contentDescription = stringResource(R.string.action_unlock),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { videoViewModel.clearAllLocks() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_unlock_all))
                    }
                }
            }
        }
    }
}
