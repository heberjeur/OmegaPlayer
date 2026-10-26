package com.mohamedaminelouati.oplayer.ui.feature.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedaminelouati.oplayer.R
import com.mohamedaminelouati.oplayer.viewmodel.ThemeViewModel
import com.mohamedaminelouati.oplayer.viewmodel.VideoViewModel

enum class SettingsPage(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector
) {
    APPEARANCE(R.string.section_appearance, R.string.settings_appearance_desc, Icons.Default.Palette),
    PLAYBACK(R.string.section_playback, R.string.settings_playback_desc, Icons.Default.PlayCircle),
    PLAYER_HUD(R.string.setting_player_hud, R.string.settings_hud_desc, Icons.Default.Fullscreen),
    GESTURES(R.string.section_gestures, R.string.settings_gestures_desc, Icons.Default.TouchApp),
    SUBTITLES(R.string.section_subtitles, R.string.settings_subtitles_desc, Icons.Default.Subtitles),
    DEFAULTS(R.string.section_defaults, R.string.settings_defaults_desc, Icons.Default.Tune),
    PRIVACY(R.string.section_privacy, R.string.settings_privacy_desc, Icons.Default.History),
    SECURITY(R.string.section_security, R.string.settings_security_desc, Icons.Default.Lock),
    EXCLUDED_FOLDERS(R.string.section_excluded_folders, R.string.settings_excluded_desc, Icons.Default.FolderOff),
    NOTIFICATIONS(R.string.section_notification_controls, R.string.settings_notifications_desc, Icons.Default.Notifications),
    ABOUT(R.string.section_about, R.string.settings_about_desc, Icons.Default.Info)
}

data class SearchableSettingItem(
    val titleRes: Int,
    val subtitleRes: Int? = null,
    val page: SettingsPage
)

private val allSearchableSettings = listOf(
    SearchableSettingItem(R.string.setting_language, R.string.setting_language_sub, SettingsPage.APPEARANCE),
    SearchableSettingItem(R.string.app_theme, R.string.theme_system, SettingsPage.APPEARANCE),
    SearchableSettingItem(R.string.dynamic_color, R.string.dynamic_color_sub, SettingsPage.APPEARANCE),
    SearchableSettingItem(R.string.theme_colors, R.string.theme_colors_sub, SettingsPage.APPEARANCE),
    SearchableSettingItem(R.string.setting_text_truncation, R.string.setting_text_truncation_sub, SettingsPage.APPEARANCE),
    SearchableSettingItem(R.string.setting_folder_flatten_threshold, R.string.setting_folder_flatten_threshold_sub, SettingsPage.APPEARANCE),
    SearchableSettingItem(R.string.setting_player_orientation, R.string.setting_player_orientation_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_auto_play_next, R.string.setting_auto_play_next_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_volume_boost, R.string.setting_volume_boost_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_audio_normalization, R.string.setting_audio_normalization_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_normalization_level, R.string.setting_normalization_level_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_up_next_expanded, R.string.setting_up_next_expanded_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_keep_screen_on_when_paused, R.string.setting_keep_screen_on_when_paused_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_controls_timeout, R.string.setting_controls_timeout_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_speed_scope, R.string.setting_speed_scope_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_brightness_scope, R.string.setting_brightness_scope_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_save_position_on_quit, R.string.setting_save_position_on_quit_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_close_after_queue_ends, R.string.setting_close_after_queue_ends_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.setting_reset_playback_positions, R.string.setting_reset_playback_positions_sub, SettingsPage.PLAYBACK),
    SearchableSettingItem(R.string.hud_show_system_status_bar, null, SettingsPage.PLAYER_HUD),
    SearchableSettingItem(R.string.hud_show_clock, null, SettingsPage.PLAYER_HUD),
    SearchableSettingItem(R.string.hud_show_battery, null, SettingsPage.PLAYER_HUD),
    SearchableSettingItem(R.string.hud_show_media_info, null, SettingsPage.PLAYER_HUD),
    SearchableSettingItem(R.string.hud_show_volume, null, SettingsPage.PLAYER_HUD),
    SearchableSettingItem(R.string.hud_show_brightness, null, SettingsPage.PLAYER_HUD),
    SearchableSettingItem(R.string.gesture_volume, R.string.gesture_volume_sub, SettingsPage.GESTURES),
    SearchableSettingItem(R.string.gesture_brightness, R.string.gesture_brightness_sub, SettingsPage.GESTURES),
    SearchableSettingItem(R.string.gesture_seek_forward, R.string.gesture_seek_forward_sub, SettingsPage.GESTURES),
    SearchableSettingItem(R.string.gesture_seek_backward, R.string.gesture_seek_backward_sub, SettingsPage.GESTURES),
    SearchableSettingItem(R.string.gesture_play_pause, R.string.gesture_play_pause_sub, SettingsPage.GESTURES),
    SearchableSettingItem(R.string.gesture_tap_controls, R.string.gesture_tap_controls_sub, SettingsPage.GESTURES),
    SearchableSettingItem(R.string.setting_seek_increment, R.string.setting_seek_increment_sub, SettingsPage.GESTURES),
    SearchableSettingItem(R.string.setting_volume_boost, R.string.setting_volume_boost_sub, SettingsPage.GESTURES),
    SearchableSettingItem(R.string.subtitle_size, null, SettingsPage.SUBTITLES),
    SearchableSettingItem(R.string.subtitle_color, null, SettingsPage.SUBTITLES),
    SearchableSettingItem(R.string.subtitle_background, null, SettingsPage.SUBTITLES),
    SearchableSettingItem(R.string.setting_default_speed, R.string.setting_default_speed_sub, SettingsPage.DEFAULTS),
    SearchableSettingItem(R.string.setting_default_view_mode, R.string.setting_default_view_mode_sub, SettingsPage.DEFAULTS),
    SearchableSettingItem(R.string.setting_default_sort_order, R.string.setting_default_sort_order_sub, SettingsPage.DEFAULTS),
    SearchableSettingItem(R.string.setting_default_search_scope, R.string.setting_default_search_scope_sub, SettingsPage.DEFAULTS),
    SearchableSettingItem(R.string.watch_history_setting, R.string.history_active_sub, SettingsPage.PRIVACY),
    SearchableSettingItem(R.string.setting_show_recent_history, R.string.setting_show_recent_history_sub, SettingsPage.PRIVACY),
    SearchableSettingItem(R.string.setting_show_history_tab, R.string.setting_show_history_tab_sub, SettingsPage.PRIVACY),
    SearchableSettingItem(R.string.setting_app_lock, R.string.setting_app_lock_sub, SettingsPage.SECURITY),
    SearchableSettingItem(R.string.setting_lock_type, R.string.setting_lock_type_sub, SettingsPage.SECURITY),
    SearchableSettingItem(R.string.manage_locked_items, R.string.manage_locked_items_sub, SettingsPage.SECURITY),
    SearchableSettingItem(R.string.section_excluded_folders, R.string.settings_excluded_desc, SettingsPage.EXCLUDED_FOLDERS),
    SearchableSettingItem(R.string.section_notification_controls, R.string.settings_notifications_desc, SettingsPage.NOTIFICATIONS),
    SearchableSettingItem(R.string.about_developer, R.string.about_dev_sub, SettingsPage.ABOUT),
    SearchableSettingItem(R.string.view_source_code, R.string.view_source_sub, SettingsPage.ABOUT)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    isFocused: Boolean = true
) {
    val context = LocalContext.current
    var currentSection by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = isFocused) {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else if (currentSection != null) {
            currentSection = null
        } else {
            onBack()
        }
    }

    val versionName = remember {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName
    }

    val themeViewModel: ThemeViewModel = hiltViewModel()
    val videoViewModel: VideoViewModel = hiltViewModel()
    val defaultSortOrder by videoViewModel.defaultSortOrder.collectAsStateWithLifecycle()

    var showSortOrderDialog by remember { mutableStateOf(false) }
    var showAboutDeveloperDialog by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = currentSection,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally(tween(220)) { width -> width / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { width -> -width / 3 } + fadeOut(tween(220)))
            } else {
                (slideInHorizontally(tween(220)) { width -> -width / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { width -> width / 3 } + fadeOut(tween(220)))
            }
        },
        label = "SettingsScreenTransition"
    ) { activeSection ->
        if (activeSection == null) {
            val matchingItems = remember(searchQuery) {
                if (searchQuery.isBlank()) {
                    emptyList()
                } else {
                    allSearchableSettings.filter { item ->
                        val title = context.getString(item.titleRes)
                        val subtitle = item.subtitleRes?.let { context.getString(it) } ?: ""
                        val pageName = context.getString(item.page.titleRes)
                        title.contains(searchQuery, ignoreCase = true) ||
                                subtitle.contains(searchQuery, ignoreCase = true) ||
                                pageName.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        placeholder = { Text(stringResource(R.string.search_settings)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_placeholder),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )

                    if (searchQuery.isNotBlank()) {
                        if (matchingItems.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.no_settings_found),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp + bottomPadding)
                            ) {
                                items(matchingItems) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentSection = item.page
                                                searchQuery = ""
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        ListItem(
                                            leadingContent = { SettingIcon(item.page.icon) },
                                            headlineContent = { Text(stringResource(item.titleRes), fontWeight = FontWeight.Medium) },
                                            supportingContent = {
                                                Text(
                                                    text = stringResource(item.page.titleRes) + (item.subtitleRes?.let { " • " + stringResource(it) } ?: ""),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            trailingContent = {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(top = 8.dp, bottom = 16.dp + bottomPadding),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column {
                                    SettingsPage.entries.forEachIndexed { index, page ->
                                        ListItem(
                                            leadingContent = { SettingIcon(page.icon) },
                                            headlineContent = {
                                                Text(
                                                    stringResource(page.titleRes),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            supportingContent = {
                                                Text(
                                                    stringResource(page.subtitleRes),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            trailingContent = {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            modifier = Modifier.clickable { currentSection = page },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                        )
                                        if (index < SettingsPage.entries.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.version_format, versionName ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val sectionScrollState = rememberScrollState()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(activeSection.titleRes), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { currentSection = null }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(sectionScrollState)
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (activeSection) {
                        SettingsPage.APPEARANCE -> SettingsAppearanceSection(themeViewModel)
                        SettingsPage.PLAYBACK -> SettingsPlaybackSection(themeViewModel, videoViewModel)
                        SettingsPage.PLAYER_HUD -> SettingsPlayerHudSection(videoViewModel)
                        SettingsPage.GESTURES -> SettingsGesturesSection(videoViewModel)
                        SettingsPage.SUBTITLES -> SettingsSubtitlesSection(videoViewModel)
                        SettingsPage.DEFAULTS -> SettingsDefaultsSection(videoViewModel, onOpenSortOrderDialog = { showSortOrderDialog = true })
                        SettingsPage.PRIVACY -> SettingsPrivacySection(videoViewModel)
                        SettingsPage.SECURITY -> SettingsSecuritySection(videoViewModel)
                        SettingsPage.EXCLUDED_FOLDERS -> SettingsExcludedFoldersSection(videoViewModel)
                        SettingsPage.NOTIFICATIONS -> SettingsNotificationSection(videoViewModel)
                        SettingsPage.ABOUT -> SettingsAboutSection(
                            onShowDeveloper = { showAboutDeveloperDialog = true },
                            versionName = versionName,
                            context = context
                        )
                    }
                }
            }
        }
    }

    if (showAboutDeveloperDialog) {
        SettingsAboutDeveloperDialog(onDismiss = { showAboutDeveloperDialog = false })
    }

    if (showSortOrderDialog) {
        SettingsSortOrderDialog(
            currentSortOrder = defaultSortOrder,
            onSortOrderSelected = { videoViewModel.setDefaultSortOrder(it); showSortOrderDialog = false },
            onDismiss = { showSortOrderDialog = false }
        )
    }
}

@Composable
fun SettingsAboutSection(
    onShowDeveloper: () -> Unit,
    versionName: String?,
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_developer), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.about_dev_sub)) },
                leadingContent = { SettingIcon(Icons.Default.Person) },
                modifier = Modifier.clickable { onShowDeveloper() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.view_source_code), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.view_source_sub)) },
                leadingContent = { SettingIcon(Icons.Default.Code) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/heberjeur/OmegaPlayer"))
                    context.startActivity(intent)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.version_format, versionName ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

