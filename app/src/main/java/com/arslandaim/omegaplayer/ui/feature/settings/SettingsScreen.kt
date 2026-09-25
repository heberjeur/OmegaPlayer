package com.arslandaim.omegaplayer.ui.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.data.AppTheme
import com.arslandaim.omegaplayer.data.MediaSortOrder
import com.arslandaim.omegaplayer.data.PlaybackSpeedScope
import com.arslandaim.omegaplayer.viewmodel.ThemeViewModel
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel
import com.arslandaim.omegaplayer.util.StartupTrace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    isFocused: Boolean = true
) {
    BackHandler(enabled = isFocused, onBack = onBack)

    val context = LocalContext.current

    val versionName = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            "1.4.2"
        }
    }

    val themeViewModel: ThemeViewModel = hiltViewModel()
    val videoViewModel: VideoViewModel = hiltViewModel()
    val currentTheme by themeViewModel.theme.collectAsState()
    val dynamicColorEnabled by themeViewModel.dynamicColor.collectAsState()
    val playerOrientation by themeViewModel.playerOrientation.collectAsState()
    val upNextFullyExpanded by themeViewModel.upNextFullyExpanded.collectAsState()
    val folderFlattenThreshold by themeViewModel.folderFlattenThreshold.collectAsState()
    val controlsTimeout by themeViewModel.controlsTimeout.collectAsState()
    val isHistoryPaused by videoViewModel.isHistoryPaused.collectAsStateWithLifecycle()
    val excludedFolders by videoViewModel.excludedFolders.collectAsStateWithLifecycle()
    val volumeBoostEnabled by videoViewModel.volumeBoostEnabled.collectAsStateWithLifecycle()
    val showRecentHistoryOnHome by videoViewModel.showRecentHistoryOnHome.collectAsStateWithLifecycle()
    val showHistoryTab by videoViewModel.showHistoryTab.collectAsStateWithLifecycle()

    val autoPlayNext by videoViewModel.autoPlayNext.collectAsStateWithLifecycle()
    val autoPip by videoViewModel.autoPip.collectAsStateWithLifecycle()
    val speedScope by videoViewModel.speedScope.collectAsStateWithLifecycle()
    val showSystemStatusBar by videoViewModel.showSystemStatusBar.collectAsStateWithLifecycle()
    val showPlayerClock by videoViewModel.showPlayerClock.collectAsStateWithLifecycle()
    val showPlayerBattery by videoViewModel.showPlayerBattery.collectAsStateWithLifecycle()
    val showPlayerMediaInfo by videoViewModel.showPlayerMediaInfo.collectAsStateWithLifecycle()
    val showPlayerVolume by videoViewModel.showPlayerVolume.collectAsStateWithLifecycle()
    val showPlayerBrightness by videoViewModel.showPlayerBrightness.collectAsStateWithLifecycle()

    val defaultSpeed by videoViewModel.defaultPlaybackSpeed.collectAsStateWithLifecycle()
    val defaultViewMode by videoViewModel.defaultViewMode.collectAsStateWithLifecycle()
    val defaultSortOrder by videoViewModel.defaultSortOrder.collectAsStateWithLifecycle()

    val subtitleTextSize by videoViewModel.subtitleTextSize.collectAsStateWithLifecycle()
    val subtitleTextColor by videoViewModel.subtitleTextColor.collectAsStateWithLifecycle()
    val subtitleBgStyle by videoViewModel.subtitleBgStyle.collectAsStateWithLifecycle()

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

    var showSortOrderDialog by remember { mutableStateOf(false) }
    var showAboutDeveloperDialog by remember { mutableStateOf(false) }
    var showStartupReportDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

                    val currentSortLabel = try {
                        MediaSortOrder.valueOf(defaultSortOrder).label
                    } catch (_: IllegalArgumentException) {
                        defaultSortOrder
                    }

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.setting_default_sort_order), fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(currentSortLabel) },
                        trailingContent = {
                            TextButton(onClick = { showSortOrderDialog = true }) {
                                Text(stringResource(R.string.action_filter))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            Text(stringResource(R.string.setting_player_hud), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.hud_show_system_status_bar), fontWeight = FontWeight.Medium) },
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

            Text(stringResource(R.string.section_subtitles), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.subtitle_size), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val sizes = listOf(
                            14 to stringResource(R.string.size_small),
                            18 to stringResource(R.string.size_normal),
                            22 to stringResource(R.string.size_large),
                            26 to stringResource(R.string.size_extra_large)
                        )
                        sizes.forEachIndexed { index, (size, label) ->
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                selected = subtitleTextSize == size,
                                onClick = { videoViewModel.setSubtitleTextSize(size) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = sizes.size),
                                label = { Text(label, fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.subtitle_color), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val colors = listOf(
                            0 to stringResource(R.string.color_white),
                            1 to stringResource(R.string.color_yellow),
                            2 to stringResource(R.string.color_cyan)
                        )
                        colors.forEachIndexed { index, (colorKey, label) ->
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                selected = subtitleTextColor == colorKey,
                                onClick = { videoViewModel.setSubtitleTextColor(colorKey) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = colors.size),
                                label = { Text(label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.subtitle_background), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val backgrounds = listOf(
                            0 to stringResource(R.string.bg_transparent),
                            1 to stringResource(R.string.bg_semi_transparent),
                            2 to stringResource(R.string.bg_black)
                        )
                        backgrounds.forEachIndexed { index, (bgKey, label) ->
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                selected = subtitleBgStyle == bgKey,
                                onClick = { videoViewModel.setSubtitleBgStyle(bgKey) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = backgrounds.size),
                                label = { Text(label, fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }

            Text(stringResource(R.string.section_excluded_folders), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.excluded_folders_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        tint = MaterialTheme.colorScheme.error,
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

            Text(stringResource(R.string.section_notification_controls), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                        onCheckedChange = { videoViewModel.setNotifShowPrevious(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_notif_rewind),
                        checked = notifShowRewind,
                        enabled = notifShowRewind || activeNotifCount < 5,
                        onCheckedChange = { videoViewModel.setNotifShowRewind(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_notif_forward),
                        checked = notifShowForward,
                        enabled = notifShowForward || activeNotifCount < 5,
                        onCheckedChange = { videoViewModel.setNotifShowForward(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_notif_next),
                        checked = notifShowNext,
                        enabled = notifShowNext || activeNotifCount < 5,
                        onCheckedChange = { videoViewModel.setNotifShowNext(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_notif_speed),
                        checked = notifShowSpeed,
                        enabled = notifShowSpeed || activeNotifCount < 5,
                        onCheckedChange = { videoViewModel.setNotifShowSpeed(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_notif_stop),
                        checked = notifShowStop,
                        enabled = notifShowStop || activeNotifCount < 5,
                        onCheckedChange = { videoViewModel.setNotifShowStop(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_notif_close),
                        checked = notifShowClose,
                        enabled = notifShowClose || activeNotifCount < 5,
                        onCheckedChange = { videoViewModel.setNotifShowClose(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_notif_repeat),
                        checked = notifShowRepeat,
                        enabled = notifShowRepeat || activeNotifCount < 5,
                        onCheckedChange = { videoViewModel.setNotifShowRepeat(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_notif_shuffle),
                        checked = notifShowShuffle,
                        enabled = notifShowShuffle || activeNotifCount < 5,
                        onCheckedChange = { videoViewModel.setNotifShowShuffle(it) }
                    )
                }
            }

            Text(stringResource(R.string.section_about), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.about_developer), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.about_dev_sub)) },
                    leadingContent = { 
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    modifier = Modifier.clickable { showAboutDeveloperDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.view_source_code), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.view_source_sub)) },
                    leadingContent = { 
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    modifier = Modifier.clickable { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/heberjeur/OmegaPlayer"))
                        context.startActivity(intent)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.startup_report_title), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.startup_report_sub)) },
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    modifier = Modifier.clickable { showStartupReportDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.version_format, versionName ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (showAboutDeveloperDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDeveloperDialog = false },
                title = { Text(stringResource(R.string.about_developer), fontWeight = FontWeight.Bold) },
                text = {
                    Text(stringResource(R.string.developer_info))
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDeveloperDialog = false }) {
                        Text(stringResource(R.string.action_close))
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        if (showStartupReportDialog) {
            val report = remember { StartupTrace.buildReport(context) }
            val reportPath = remember { StartupTrace.reportFile(context).absolutePath }
            AlertDialog(
                onDismissRequest = { showStartupReportDialog = false },
                title = { Text(stringResource(R.string.startup_report_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.startup_report_file, reportPath),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 380.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = report,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("OmegaPlayer startup report", report))
                            Toast.makeText(context, context.getString(R.string.startup_report_copied), Toast.LENGTH_SHORT).show()
                        }) { Text(stringResource(R.string.action_copy)) }
                        TextButton(onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "OmegaPlayer startup report")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        }) { Text(stringResource(R.string.action_share)) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartupReportDialog = false }) {
                        Text(stringResource(R.string.action_close))
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        if (showSortOrderDialog) {
            AlertDialog(
                onDismissRequest = { showSortOrderDialog = false },
                title = { Text(stringResource(R.string.setting_default_sort_order), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        MediaSortOrder.entries.forEach { order ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        videoViewModel.setDefaultSortOrder(order.name)
                                        showSortOrderDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = defaultSortOrder == order.name,
                                    onClick = {
                                        videoViewModel.setDefaultSortOrder(order.name)
                                        showSortOrderDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(order.label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSortOrderDialog = false }) {
                        Text(stringResource(R.string.action_close))
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium, color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
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
