package com.arslandaim.omegaplayer.ui.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.data.AppTheme
import com.arslandaim.omegaplayer.data.MediaSortOrder
import com.arslandaim.omegaplayer.data.PlaybackSpeedScope
import com.arslandaim.omegaplayer.viewmodel.ThemeViewModel
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceSection(themeViewModel: ThemeViewModel) {
    val currentTheme by themeViewModel.theme.collectAsState()
    val dynamicColorEnabled by themeViewModel.dynamicColor.collectAsState()
    val playerOrientation by themeViewModel.playerOrientation.collectAsState()
    val upNextFullyExpanded by themeViewModel.upNextFullyExpanded.collectAsState()
    val folderFlattenThreshold by themeViewModel.folderFlattenThreshold.collectAsState()

    Text(stringResource(R.string.section_appearance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.app_theme), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppTheme.entries.forEachIndexed { index, theme ->
                    val themeLabel = when (theme) {
                        AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                        AppTheme.LIGHT -> stringResource(R.string.theme_light)
                        AppTheme.DARK -> stringResource(R.string.theme_dark)
                    }
                    SegmentedButton(
                        modifier = Modifier.weight(1f),
                        selected = currentTheme == theme,
                        onClick = { themeViewModel.setTheme(theme) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = AppTheme.entries.size),
                        label = { Text(themeLabel, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(modifier = Modifier.height(16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.dynamic_color), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.dynamic_color_sub)) },
                    trailingContent = {
                        Switch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = { themeViewModel.setDynamicColor(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.setting_player_orientation), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    stringResource(R.string.orientation_auto),
                    stringResource(R.string.orientation_sensor),
                    stringResource(R.string.orientation_fixed)
                )
                options.forEachIndexed { index, option ->
                    val isSelected = playerOrientation == index
                    OutlinedButton(
                        onClick = { themeViewModel.setPlayerOrientation(index) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(option, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_up_next_expanded), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_up_next_expanded_sub)) },
                trailingContent = {
                    Switch(
                        checked = upNextFullyExpanded,
                        onCheckedChange = { themeViewModel.setUpNextFullyExpanded(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.setting_folder_flatten_threshold), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.setting_folder_flatten_threshold_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                @OptIn(ExperimentalMaterial3Api::class)
                Slider(
                    value = folderFlattenThreshold.toFloat(),
                    onValueChange = { themeViewModel.setFolderFlattenThreshold(it.toInt()) },
                    valueRange = 1f..15f,
                    steps = 13,
                    modifier = Modifier.weight(1f),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
                            ),
                            sliderState = sliderState,
                            modifier = Modifier.height(2.dp)
                        )
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = folderFlattenThreshold.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(24.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsPrivacySection(videoViewModel: VideoViewModel) {
    val isHistoryPaused by videoViewModel.isHistoryPaused.collectAsStateWithLifecycle()
    val showRecentHistoryOnHome by videoViewModel.showRecentHistoryOnHome.collectAsStateWithLifecycle()
    val showHistoryTab by videoViewModel.showHistoryTab.collectAsStateWithLifecycle()

    Text(stringResource(R.string.section_privacy), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.watch_history_setting), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(if (isHistoryPaused) stringResource(R.string.history_paused_sub) else stringResource(R.string.history_active_sub)) },
                trailingContent = {
                    Switch(
                        checked = !isHistoryPaused,
                        onCheckedChange = { videoViewModel.toggleHistoryPause(!it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_show_recent_history), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_show_recent_history_sub)) },
                trailingContent = {
                    Switch(
                        checked = showRecentHistoryOnHome,
                        onCheckedChange = { videoViewModel.toggleShowRecentHistoryOnHome(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_show_history_tab), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_show_history_tab_sub)) },
                trailingContent = {
                    Switch(
                        checked = showHistoryTab,
                        onCheckedChange = { videoViewModel.toggleShowHistoryTab(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPlaybackSection(themeViewModel: ThemeViewModel, videoViewModel: VideoViewModel) {
    val controlsTimeout by themeViewModel.controlsTimeout.collectAsState()
    val autoPlayNext by videoViewModel.autoPlayNext.collectAsStateWithLifecycle()
    val volumeBoostEnabled by videoViewModel.volumeBoostEnabled.collectAsStateWithLifecycle()
    val autoPip by videoViewModel.autoPip.collectAsStateWithLifecycle()
    val speedScope by videoViewModel.speedScope.collectAsStateWithLifecycle()

    Text(stringResource(R.string.section_playback), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_auto_play_next), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_auto_play_next_sub)) },
                trailingContent = {
                    Switch(
                        checked = autoPlayNext,
                        onCheckedChange = { videoViewModel.setAutoPlayNext(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_volume_boost), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_volume_boost_sub)) },
                trailingContent = {
                    Switch(
                        checked = volumeBoostEnabled,
                        onCheckedChange = { videoViewModel.toggleVolumeBoost(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_auto_pip), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_auto_pip_sub)) },
                trailingContent = {
                    Switch(
                        checked = autoPip,
                        onCheckedChange = { videoViewModel.setAutoPip(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.setting_speed_scope), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.setting_speed_scope_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PlaybackSpeedScope.entries.forEachIndexed { index, scope ->
                    val label = when (scope) {
                        PlaybackSpeedScope.GLOBAL -> stringResource(R.string.speed_scope_global)
                        PlaybackSpeedScope.PER_FOLDER -> stringResource(R.string.speed_scope_per_folder)
                    }
                    SegmentedButton(
                        modifier = Modifier.weight(1f),
                        selected = speedScope == scope,
                        onClick = { videoViewModel.setSpeedScope(scope) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = PlaybackSpeedScope.entries.size),
                        label = { Text(label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.setting_controls_timeout), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.setting_controls_timeout_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            val timeoutOptions = listOf(0, 3, 5, 10)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                timeoutOptions.forEachIndexed { index, option ->
                    val label = if (option == 0) stringResource(R.string.controls_manual) else stringResource(R.string.controls_sec, option)
                    SegmentedButton(
                        modifier = Modifier.weight(1f),
                        selected = controlsTimeout == option,
                        onClick = { themeViewModel.setControlsTimeout(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = timeoutOptions.size),
                        label = { Text(label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDefaultsSection(videoViewModel: VideoViewModel, onOpenSortOrderDialog: () -> Unit) {
    val defaultSpeed by videoViewModel.defaultPlaybackSpeed.collectAsStateWithLifecycle()
    val defaultViewMode by videoViewModel.defaultViewMode.collectAsStateWithLifecycle()
    val defaultSortOrder by videoViewModel.defaultSortOrder.collectAsStateWithLifecycle()

    Text(stringResource(R.string.section_defaults), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.setting_default_speed), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.setting_default_speed_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                @OptIn(ExperimentalMaterial3Api::class)
                Slider(
                    value = defaultSpeed,
                    onValueChange = { videoViewModel.setDefaultPlaybackSpeed(it) },
                    valueRange = 0.25f..3.0f,
                    modifier = Modifier.weight(1f),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
                            ),
                            sliderState = sliderState,
                            modifier = Modifier.height(2.dp)
                        )
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = String.format(java.util.Locale.US, "%.2fx", defaultSpeed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.setting_default_view_mode), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.setting_default_view_mode_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    0 to stringResource(R.string.view_mode_list),
                    1 to stringResource(R.string.view_mode_grid),
                    2 to stringResource(R.string.view_mode_card)
                )
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        modifier = Modifier.weight(1f),
                        selected = defaultViewMode == mode,
                        onClick = { videoViewModel.setDefaultViewMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        label = { Text(label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            val currentSortLabel = MediaSortOrder.values().firstOrNull { it.name == defaultSortOrder }?.label ?: defaultSortOrder

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_default_sort_order), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(currentSortLabel) },
                trailingContent = {
                    TextButton(onClick = { onOpenSortOrderDialog() }) {
                        Text(stringResource(R.string.action_filter))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}
