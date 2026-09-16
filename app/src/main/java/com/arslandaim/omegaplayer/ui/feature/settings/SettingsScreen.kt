package com.arslandaim.omegaplayer.ui.feature.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.data.AppTheme
import com.arslandaim.omegaplayer.data.PlaybackSpeedScope
import com.arslandaim.omegaplayer.viewmodel.ThemeViewModel
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel

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
        } catch (_: Exception) {
            "1.4.2"
        }
    }

    val themeViewModel: ThemeViewModel = hiltViewModel()
    val videoViewModel: VideoViewModel = hiltViewModel()
    val currentTheme by themeViewModel.theme.collectAsState()
    val dynamicColorEnabled by themeViewModel.dynamicColor.collectAsState()
    val isHistoryPaused by videoViewModel.isHistoryPaused.collectAsStateWithLifecycle()
    val excludedFolders by videoViewModel.excludedFolders.collectAsStateWithLifecycle(initialValue = emptySet())
    val showRecentHistoryOnHome by videoViewModel.showRecentHistoryOnHome.collectAsStateWithLifecycle()
    val showHistoryTab by videoViewModel.showHistoryTab.collectAsStateWithLifecycle()

    val speedScope by videoViewModel.speedScope.collectAsStateWithLifecycle(initialValue = PlaybackSpeedScope.GLOBAL)
    val showPlayerClock by videoViewModel.showPlayerClock.collectAsStateWithLifecycle(initialValue = true)
    val showPlayerBattery by videoViewModel.showPlayerBattery.collectAsStateWithLifecycle(initialValue = true)
    val showPlayerMediaInfo by videoViewModel.showPlayerMediaInfo.collectAsStateWithLifecycle(initialValue = true)
    val showPlayerVolume by videoViewModel.showPlayerVolume.collectAsStateWithLifecycle(initialValue = true)
    val showPlayerBrightness by videoViewModel.showPlayerBrightness.collectAsStateWithLifecycle(initialValue = true)

    var showAboutDeveloperDialog by remember { mutableStateOf(false) }
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
                                selected = currentTheme == theme,
                                onClick = { themeViewModel.setTheme(theme) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = AppTheme.entries.size),
                                label = { Text(themeLabel) }
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
                                selected = speedScope == scope,
                                onClick = { videoViewModel.setSpeedScope(scope) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = PlaybackSpeedScope.entries.size),
                                label = { Text(label, maxLines = 1) }
                            )
                        }
                    }
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
    }
}
