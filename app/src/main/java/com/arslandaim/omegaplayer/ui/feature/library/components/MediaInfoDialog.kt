package com.arslandaim.omegaplayer.ui.feature.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.util.DetailedMediaInfo
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration

@Composable
fun MediaInfoDialog(
    info: DetailedMediaInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.menu_information),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                SectionTitle(stringResource(R.string.info_file))
                InfoRow(stringResource(R.string.info_file), info.fileName)
                InfoRow(stringResource(R.string.info_location), info.path)
                InfoRow(stringResource(R.string.info_size), formatSize(info.sizeBytes))
                InfoRow(stringResource(R.string.info_duration), formatDuration(info.durationMs))
                InfoRow(stringResource(R.string.info_format), info.format)

                if (info.videoTrack != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle(stringResource(R.string.info_video_track))
                    InfoRow(stringResource(R.string.info_codec), info.videoTrack.codec)
                    InfoRow(stringResource(R.string.info_resolution), info.videoTrack.resolution)
                    InfoRow(stringResource(R.string.info_frame_rate), "${info.videoTrack.frameRate}")
                    if (info.videoTrack.bitrate > 0) {
                        InfoRow(stringResource(R.string.info_bitrate), stringResource(R.string.info_bitrate_format, info.videoTrack.bitrate / 1000))
                    }
                }

                info.audioTracks.forEach { track ->
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle(stringResource(R.string.info_audio_track, track.index))
                    InfoRow(stringResource(R.string.info_codec), track.codec)
                    InfoRow(stringResource(R.string.info_sample_rate), stringResource(R.string.info_sample_rate_format, track.sampleRate))
                    if (track.bitrate > 0) {
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
                        InfoRow(stringResource(R.string.info_channels), channelsStr)
                    }
                    if (track.language != "Unknown") {
                        InfoRow(stringResource(R.string.info_language), track.language)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
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
