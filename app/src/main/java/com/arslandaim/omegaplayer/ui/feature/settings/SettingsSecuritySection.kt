package com.arslandaim.omegaplayer.ui.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.data.SecurityLockType
import com.arslandaim.omegaplayer.ui.feature.security.ManageLockedItemsDialog
import com.arslandaim.omegaplayer.ui.feature.security.PatternSetupDialog
import com.arslandaim.omegaplayer.ui.feature.security.PinSetupDialog
import com.arslandaim.omegaplayer.ui.feature.security.SecurityAuthDialog
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel

@Composable
fun SettingsSecuritySection(videoViewModel: VideoViewModel) {
    val context = LocalContext.current
    val currentLockType by videoViewModel.securityLockType.collectAsStateWithLifecycle()
    val pinHash by videoViewModel.securityPinHash.collectAsStateWithLifecycle()
    val patternHash by videoViewModel.securityPatternHash.collectAsStateWithLifecycle()
    val appLockEnabled by videoViewModel.securityAppLockEnabled.collectAsStateWithLifecycle()
    val lockedFolders by videoViewModel.securityLockedFolders.collectAsStateWithLifecycle()
    val lockedMedia by videoViewModel.securityLockedMedia.collectAsStateWithLifecycle()

    var showLockTypeDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPatternSetupDialog by remember { mutableStateOf(false) }
    var showManageLockedItemsDialog by remember { mutableStateOf(false) }
    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingLockTypeActivation by remember { mutableStateOf<SecurityLockType?>(null) }

    val runWithSecurityAuth: (() -> Unit) -> Unit = { action ->
        if (currentLockType != SecurityLockType.NONE) {
            pendingAuthAction = action
        } else {
            action()
        }
    }

    if (pendingAuthAction != null && currentLockType != SecurityLockType.NONE) {
        SecurityAuthDialog(
            lockType = currentLockType,
            videoViewModel = videoViewModel,
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

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = {
                showPinSetupDialog = false
                pendingLockTypeActivation = null
            },
            onPinConfirmed = { pin ->
                videoViewModel.setSecurityPin(pin)
                val pending = pendingLockTypeActivation
                if (pending != null) {
                    videoViewModel.setSecurityLockType(pending)
                    pendingLockTypeActivation = null
                }
            }
        )
    }

    if (showPatternSetupDialog) {
        PatternSetupDialog(
            onDismiss = {
                showPatternSetupDialog = false
                pendingLockTypeActivation = null
            },
            onPatternConfirmed = { pattern ->
                videoViewModel.setSecurityPattern(pattern)
                val pending = pendingLockTypeActivation
                if (pending != null) {
                    videoViewModel.setSecurityLockType(pending)
                    pendingLockTypeActivation = null
                }
            }
        )
    }

    if (showManageLockedItemsDialog) {
        ManageLockedItemsDialog(
            videoViewModel = videoViewModel,
            onDismiss = { showManageLockedItemsDialog = false }
        )
    }

    if (showLockTypeDialog) {
        AlertDialog(
            onDismissRequest = { showLockTypeDialog = false },
            title = { Text(stringResource(R.string.setting_lock_type)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val options = listOf(
                        SecurityLockType.NONE to stringResource(R.string.lock_type_none),
                        SecurityLockType.PIN to stringResource(R.string.lock_type_pin),
                        SecurityLockType.PATTERN to stringResource(R.string.lock_type_pattern)
                    )

                    for ((type, label) in options) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLockTypeDialog = false
                                    when (type) {
                                        SecurityLockType.NONE -> {
                                            videoViewModel.setSecurityLockType(SecurityLockType.NONE)
                                            videoViewModel.setSecurityAppLockEnabled(false)
                                        }
                                        SecurityLockType.PIN -> {
                                            if (pinHash.isEmpty()) {
                                                pendingLockTypeActivation = SecurityLockType.PIN
                                                showPinSetupDialog = true
                                            } else {
                                                videoViewModel.setSecurityLockType(SecurityLockType.PIN)
                                            }
                                        }
                                        SecurityLockType.PATTERN -> {
                                            if (patternHash.isEmpty()) {
                                                pendingLockTypeActivation = SecurityLockType.PATTERN
                                                showPatternSetupDialog = true
                                            } else {
                                                videoViewModel.setSecurityLockType(SecurityLockType.PATTERN)
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLockType == type,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLockTypeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = AlertDialogDefaults.containerColor
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val currentLockLabel = when (currentLockType) {
                SecurityLockType.NONE -> stringResource(R.string.lock_type_none)
                SecurityLockType.PIN -> stringResource(R.string.lock_type_pin)
                SecurityLockType.PATTERN -> stringResource(R.string.lock_type_pattern)
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_lock_type), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(currentLockLabel) },
                leadingContent = {
                    SettingIcon(
                        imageVector = when (currentLockType) {
                            SecurityLockType.NONE -> Icons.Default.LockReset
                            SecurityLockType.PIN -> Icons.Default.Password
                            SecurityLockType.PATTERN -> Icons.Default.Pattern
                        }
                    )
                },
                modifier = Modifier.clickable { runWithSecurityAuth { showLockTypeDialog = true } },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            if (currentLockType != SecurityLockType.NONE) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.setting_app_lock), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.setting_app_lock_sub)) },
                    leadingContent = { SettingIcon(Icons.Default.Lock) },
                    trailingContent = {
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { isChecked ->
                                runWithSecurityAuth { videoViewModel.setSecurityAppLockEnabled(isChecked) }
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                if (currentLockType == SecurityLockType.PIN) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.change_pin), fontWeight = FontWeight.Medium) },
                        leadingContent = { SettingIcon(Icons.Default.Password) },
                        modifier = Modifier.clickable { runWithSecurityAuth { showPinSetupDialog = true } },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                if (currentLockType == SecurityLockType.PATTERN) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.change_pattern), fontWeight = FontWeight.Medium) },
                        leadingContent = { SettingIcon(Icons.Default.Pattern) },
                        modifier = Modifier.clickable { runWithSecurityAuth { showPatternSetupDialog = true } },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                val totalLocked = lockedFolders.size + lockedMedia.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { runWithSecurityAuth { showManageLockedItemsDialog = true } }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingIcon(Icons.Default.Lock)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.manage_locked_items),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.manage_locked_items_sub),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (totalLocked > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$totalLocked",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
