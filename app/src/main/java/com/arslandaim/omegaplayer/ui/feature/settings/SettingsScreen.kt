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
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.omegaplayer.R
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
        } catch (_: PackageManager.NameNotFoundException) {
            "1.4.2"
        }
    }

    val themeViewModel: ThemeViewModel = hiltViewModel()
    val videoViewModel: VideoViewModel = hiltViewModel()
    val defaultSortOrder by videoViewModel.defaultSortOrder.collectAsStateWithLifecycle()

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
            SettingsAppearanceSection(themeViewModel)
            SettingsPrivacySection(videoViewModel)
            SettingsPlaybackSection(themeViewModel, videoViewModel)
            SettingsDefaultsSection(videoViewModel, onOpenSortOrderDialog = { showSortOrderDialog = true })
            SettingsPlayerHudSection(videoViewModel)
            SettingsSubtitlesSection(videoViewModel)
            SettingsExcludedFoldersSection(videoViewModel)
            SettingsNotificationSection(videoViewModel)

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
            SettingsAboutDeveloperDialog(onDismiss = { showAboutDeveloperDialog = false })
        }

        if (showStartupReportDialog) {
            SettingsStartupReportDialog(onDismiss = { showStartupReportDialog = false })
        }

        if (showSortOrderDialog) {
            SettingsSortOrderDialog(
                currentSortOrder = defaultSortOrder,
                onSortOrderSelected = { videoViewModel.setDefaultSortOrder(it); showSortOrderDialog = false },
                onDismiss = { showSortOrderDialog = false }
            )
        }
    }
}
