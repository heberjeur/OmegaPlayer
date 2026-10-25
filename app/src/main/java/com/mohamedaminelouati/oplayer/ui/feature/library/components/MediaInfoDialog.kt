package com.mohamedaminelouati.oplayer.ui.feature.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mohamedaminelouati.oplayer.R
import com.mohamedaminelouati.oplayer.util.DetailedMediaInfo
import com.mohamedaminelouati.oplayer.util.MediaUtils.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoDialog(
    info: DetailedMediaInfo,
    onDismiss: () -> Unit
) {
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
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                SectionTitle(stringResource(R.string.info_file))
                                InfoRow(stringResource(R.string.info_file), info.fileName)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                InfoRow(stringResource(R.string.info_location), info.path)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                InfoRow(stringResource(R.string.info_size), formatSize(info.sizeBytes))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                InfoRow(stringResource(R.string.info_duration), formatDuration(info.durationMs))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                InfoRow(stringResource(R.string.info_format), info.format)
                            }
                        }

                        if (info.videoTrack != null) {
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
                                    SectionTitle(stringResource(R.string.info_video_track))
                                    InfoRow(stringResource(R.string.info_codec), info.videoTrack.codec)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    InfoRow(stringResource(R.string.info_resolution), info.videoTrack.resolution)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    InfoRow(stringResource(R.string.info_frame_rate), "${info.videoTrack.frameRate}")
                                    if (info.videoTrack.bitrate > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        InfoRow(stringResource(R.string.info_bitrate), stringResource(R.string.info_bitrate_format, info.videoTrack.bitrate / 1000))
                                    }
                                }
                            }
                        }

                        info.audioTracks.forEach { track ->
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
                                    SectionTitle(stringResource(R.string.info_audio_track, track.index))
                                    InfoRow(stringResource(R.string.info_codec), track.codec)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    InfoRow(stringResource(R.string.info_sample_rate), stringResource(R.string.info_sample_rate_format, track.sampleRate))
                                    if (track.bitrate > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        InfoRow(stringResource(R.string.info_bitrate), stringResource(R.string.info_bitrate_format, track.bitrate / 1000))
                                    }
                                    if (track.channels > 0) {
                                        val channelsStr = if (track.channels == 1) {
                                            stringResource(R.string.info_channels_mono)
                                        } else if (track.channels == 2) {
                                            stringResource(R.string.info_channels_stereo)
                                        } else {
                                            "${track.channels}"
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        InfoRow(stringResource(R.string.info_channels), channelsStr)
                                    }
                                    if (track.language != "Unknown") {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        InfoRow(stringResource(R.string.info_language), track.language)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            softWrap = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun formatSize(sizeBytes: Long): String {
    val kb = sizeBytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f KB", kb)
        else -> stringResource(R.string.info_bytes_format, sizeBytes)
    }
}
