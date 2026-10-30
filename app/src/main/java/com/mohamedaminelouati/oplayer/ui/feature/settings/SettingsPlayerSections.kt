package com.mohamedaminelouati.oplayer.ui.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedaminelouati.oplayer.R
import com.mohamedaminelouati.oplayer.viewmodel.VideoViewModel

@Composable
fun SettingsPlayerHudSection(videoViewModel: VideoViewModel) {
    val showSystemStatusBar by videoViewModel.showSystemStatusBar.collectAsStateWithLifecycle()
    val showPlayerClock by videoViewModel.showPlayerClock.collectAsStateWithLifecycle()
    val showPlayerBattery by videoViewModel.showPlayerBattery.collectAsStateWithLifecycle()
    val showPlayerMediaInfo by videoViewModel.showPlayerMediaInfo.collectAsStateWithLifecycle()
    val showPlayerVolume by videoViewModel.showPlayerVolume.collectAsStateWithLifecycle()
    val showPlayerBrightness by videoViewModel.showPlayerBrightness.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.hud_show_system_status_bar), fontWeight = FontWeight.Medium) },
                leadingContent = { SettingIcon(Icons.Default.Fullscreen) },
                trailingContent = {
                    Switch(
                        checked = showSystemStatusBar,
                        onCheckedChange = { videoViewModel.toggleSystemStatusBar(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.hud_show_clock), fontWeight = FontWeight.Medium) },
                leadingContent = { SettingIcon(Icons.Default.Schedule) },
                trailingContent = {
                    Switch(
                        checked = showPlayerClock,
                        onCheckedChange = { videoViewModel.togglePlayerClock(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.hud_show_battery), fontWeight = FontWeight.Medium) },
                leadingContent = { SettingIcon(Icons.Default.BatteryChargingFull) },
                trailingContent = {
                    Switch(
                        checked = showPlayerBattery,
                        onCheckedChange = { videoViewModel.togglePlayerBattery(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.hud_show_media_info), fontWeight = FontWeight.Medium) },
                leadingContent = { SettingIcon(Icons.Default.Info) },
                trailingContent = {
                    Switch(
                        checked = showPlayerMediaInfo,
                        onCheckedChange = { videoViewModel.togglePlayerMediaInfo(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.hud_show_volume), fontWeight = FontWeight.Medium) },
                leadingContent = { SettingIcon(Icons.AutoMirrored.Filled.VolumeMute) },
                trailingContent = {
                    Switch(
                        checked = showPlayerVolume,
                        onCheckedChange = { videoViewModel.togglePlayerVolume(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.hud_show_brightness), fontWeight = FontWeight.Medium) },
                leadingContent = { SettingIcon(Icons.Default.Brightness5) },
                trailingContent = {
                    Switch(
                        checked = showPlayerBrightness,
                        onCheckedChange = { videoViewModel.togglePlayerBrightness(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun SettingsGesturesSection(videoViewModel: VideoViewModel) {
    val gestureVolumeEnabled by videoViewModel.gestureVolumeEnabled.collectAsStateWithLifecycle()
    val gestureBrightnessEnabled by videoViewModel.gestureBrightnessEnabled.collectAsStateWithLifecycle()
    val gesturePinchToZoomEnabled by videoViewModel.gesturePinchToZoomEnabled.collectAsStateWithLifecycle()
    val gestureSeekForwardEnabled by videoViewModel.gestureSeekForwardEnabled.collectAsStateWithLifecycle()
    val gestureSeekBackwardEnabled by videoViewModel.gestureSeekBackwardEnabled.collectAsStateWithLifecycle()
    val gesturePlayPauseEnabled by videoViewModel.gesturePlayPauseEnabled.collectAsStateWithLifecycle()
    val gestureLongPressSpeedEnabled by videoViewModel.gestureLongPressSpeedEnabled.collectAsStateWithLifecycle()
    val gestureTapControlsEnabled by videoViewModel.gestureTapControlsEnabled.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.gesture_volume), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.gesture_volume_sub)) },
                leadingContent = { SettingIcon(Icons.AutoMirrored.Filled.VolumeUp) },
                trailingContent = {
                    Switch(
                        checked = gestureVolumeEnabled,
                        onCheckedChange = { videoViewModel.setGestureVolumeEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.gesture_brightness), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.gesture_brightness_sub)) },
                leadingContent = { SettingIcon(Icons.Default.Brightness6) },
                trailingContent = {
                    Switch(
                        checked = gestureBrightnessEnabled,
                        onCheckedChange = { videoViewModel.setGestureBrightnessEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.gesture_pinch_to_zoom), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.gesture_pinch_to_zoom_sub)) },
                leadingContent = { SettingIcon(Icons.Default.ZoomIn) },
                trailingContent = {
                    Switch(
                        checked = gesturePinchToZoomEnabled,
                        onCheckedChange = { videoViewModel.setGesturePinchToZoomEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.gesture_seek_forward), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.gesture_seek_forward_sub)) },
                leadingContent = { SettingIcon(Icons.Default.FastForward) },
                trailingContent = {
                    Switch(
                        checked = gestureSeekForwardEnabled,
                        onCheckedChange = { videoViewModel.setGestureSeekForwardEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.gesture_seek_backward), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.gesture_seek_backward_sub)) },
                leadingContent = { SettingIcon(Icons.Default.FastRewind) },
                trailingContent = {
                    Switch(
                        checked = gestureSeekBackwardEnabled,
                        onCheckedChange = { videoViewModel.setGestureSeekBackwardEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.gesture_play_pause), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.gesture_play_pause_sub)) },
                leadingContent = { SettingIcon(Icons.Default.PlayCircle) },
                trailingContent = {
                    Switch(
                        checked = gesturePlayPauseEnabled,
                        onCheckedChange = { videoViewModel.setGesturePlayPauseEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.gesture_long_press_speed), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.gesture_long_press_speed_sub)) },
                leadingContent = { SettingIcon(Icons.Default.TouchApp) },
                trailingContent = {
                    Switch(
                        checked = gestureLongPressSpeedEnabled,
                        onCheckedChange = { videoViewModel.setGestureLongPressSpeedEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.gesture_tap_controls), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.gesture_tap_controls_sub)) },
                leadingContent = { SettingIcon(Icons.Default.AdsClick) },
                trailingContent = {
                    Switch(
                        checked = gestureTapControlsEnabled,
                        onCheckedChange = { videoViewModel.setGestureTapControlsEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubtitlesSection(videoViewModel: VideoViewModel) {
    val subtitleTextSize by videoViewModel.subtitleTextSize.collectAsStateWithLifecycle()
    val subtitleTextColor by videoViewModel.subtitleTextColor.collectAsStateWithLifecycle()
    val subtitleBgStyle by videoViewModel.subtitleBgStyle.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingHeader(
                title = stringResource(R.string.subtitle_size),
                icon = Icons.Default.FormatSize
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSegmentedRow {
                val sizes = listOf(
                    14 to stringResource(R.string.size_small),
                    18 to stringResource(R.string.size_normal),
                    22 to stringResource(R.string.size_large),
                    26 to stringResource(R.string.size_extra_large)
                )
                sizes.forEachIndexed { index, (size, label) ->
                    SettingsSegmentedButton(
                        selected = subtitleTextSize == size,
                        onClick = { videoViewModel.setSubtitleTextSize(size) },
                        index = index,
                        count = sizes.size,
                        label = label,
                        textStyle = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            SettingHeader(
                title = stringResource(R.string.subtitle_color),
                icon = Icons.Default.FormatColorText
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSegmentedRow {
                val colors = listOf(
                    0 to stringResource(R.string.color_white),
                    1 to stringResource(R.string.color_yellow),
                    2 to stringResource(R.string.color_cyan)
                )
                colors.forEachIndexed { index, (colorKey, label) ->
                    SettingsSegmentedButton(
                        selected = subtitleTextColor == colorKey,
                        onClick = { videoViewModel.setSubtitleTextColor(colorKey) },
                        index = index,
                        count = colors.size,
                        label = label
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            SettingHeader(
                title = stringResource(R.string.subtitle_background),
                icon = Icons.Default.FormatPaint
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSegmentedRow {
                val backgrounds = listOf(
                    0 to stringResource(R.string.bg_transparent),
                    1 to stringResource(R.string.bg_semi_transparent),
                    2 to stringResource(R.string.bg_black)
                )
                backgrounds.forEachIndexed { index, (bgKey, label) ->
                    SettingsSegmentedButton(
                        selected = subtitleBgStyle == bgKey,
                        onClick = { videoViewModel.setSubtitleBgStyle(bgKey) },
                        index = index,
                        count = backgrounds.size,
                        label = label,
                        textStyle = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsExcludedFoldersSection(videoViewModel: VideoViewModel) {
    val excludedFolders by videoViewModel.excludedFolders.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingHeader(
                title = stringResource(R.string.section_excluded_folders),
                subtitle = stringResource(R.string.excluded_folders_sub),
                icon = Icons.Default.FolderOff
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (excludedFolders.isEmpty()) {
                Text(
                    stringResource(R.string.no_excluded_folders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                excludedFolders.forEach { folderName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                folderName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { videoViewModel.restoreFolder(folderName) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                stringResource(R.string.restore_folder),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { videoViewModel.clearExcludedFolders() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.restore_all_folders))
                }
            }
        }
    }
}

@Composable
fun SettingsNotificationSection(videoViewModel: VideoViewModel) {
    val notifShowPrev by videoViewModel.notifShowPrevious.collectAsStateWithLifecycle()
    val notifShowRewind by videoViewModel.notifShowRewind.collectAsStateWithLifecycle()
    val notifShowForward by videoViewModel.notifShowForward.collectAsStateWithLifecycle()
    val notifShowNext by videoViewModel.notifShowNext.collectAsStateWithLifecycle()
    val notifShowSpeed by videoViewModel.notifShowSpeed.collectAsStateWithLifecycle()
    val notifShowStop by videoViewModel.notifShowStop.collectAsStateWithLifecycle()
    val notifShowClose by videoViewModel.notifShowClose.collectAsStateWithLifecycle()
    val notifShowRepeat by videoViewModel.notifShowRepeat.collectAsStateWithLifecycle()
    val notifShowShuffle by videoViewModel.notifShowShuffle.collectAsStateWithLifecycle()
    val activeNotifCount = listOf(notifShowPrev, notifShowRewind, notifShowForward, notifShowNext, notifShowSpeed, notifShowStop, notifShowClose, notifShowRepeat, notifShowShuffle).count { it }

    Text(stringResource(R.string.notif_limit_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SwitchPreference(
                title = stringResource(R.string.pref_notif_prev),
                checked = notifShowPrev,
                enabled = notifShowPrev || activeNotifCount < 5,
                icon = Icons.Default.SkipPrevious,
                onCheckedChange = { videoViewModel.setNotifShowPrevious(it) }
            )
            SwitchPreference(
                title = stringResource(R.string.pref_notif_rewind),
                checked = notifShowRewind,
                enabled = notifShowRewind || activeNotifCount < 5,
                icon = Icons.Default.FastRewind,
                onCheckedChange = { videoViewModel.setNotifShowRewind(it) }
            )
            SwitchPreference(
                title = stringResource(R.string.pref_notif_forward),
                checked = notifShowForward,
                enabled = notifShowForward || activeNotifCount < 5,
                icon = Icons.Default.FastForward,
                onCheckedChange = { videoViewModel.setNotifShowForward(it) }
            )
            SwitchPreference(
                title = stringResource(R.string.pref_notif_next),
                checked = notifShowNext,
                enabled = notifShowNext || activeNotifCount < 5,
                icon = Icons.Default.SkipNext,
                onCheckedChange = { videoViewModel.setNotifShowNext(it) }
            )
            SwitchPreference(
                title = stringResource(R.string.pref_notif_speed),
                checked = notifShowSpeed,
                enabled = notifShowSpeed || activeNotifCount < 5,
                icon = Icons.Default.Speed,
                onCheckedChange = { videoViewModel.setNotifShowSpeed(it) }
            )
            SwitchPreference(
                title = stringResource(R.string.pref_notif_stop),
                checked = notifShowStop,
                enabled = notifShowStop || activeNotifCount < 5,
                icon = Icons.Default.Stop,
                onCheckedChange = { videoViewModel.setNotifShowStop(it) }
            )
            SwitchPreference(
                title = stringResource(R.string.pref_notif_close),
                checked = notifShowClose,
                enabled = notifShowClose || activeNotifCount < 5,
                icon = Icons.Default.Close,
                onCheckedChange = { videoViewModel.setNotifShowClose(it) }
            )
            SwitchPreference(
                title = stringResource(R.string.pref_notif_repeat),
                checked = notifShowRepeat,
                enabled = notifShowRepeat || activeNotifCount < 5,
                icon = Icons.Default.Repeat,
                onCheckedChange = { videoViewModel.setNotifShowRepeat(it) }
            )
            SwitchPreference(
                title = stringResource(R.string.pref_notif_shuffle),
                checked = notifShowShuffle,
                enabled = notifShowShuffle || activeNotifCount < 5,
                icon = Icons.Default.Shuffle,
                onCheckedChange = { videoViewModel.setNotifShowShuffle(it) }
            )
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium, color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
        leadingContent = icon?.let { { SettingIcon(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
