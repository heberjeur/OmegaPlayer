package com.arslandaim.omegaplayer.ui.feature.player

import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.annotation.OptIn as AndroidOptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arslandaim.omegaplayer.R
import java.util.Locale

@Composable
fun SubtitleDialog(
    tracks: Tracks,
    subtitleTextSize: Int,
    onSubtitleTextSizeChange: (Int) -> Unit,
    subtitleTextColor: Int,
    onSubtitleTextColorChange: (Int) -> Unit,
    subtitleBgStyle: Int,
    onSubtitleBgStyleChange: (Int) -> Unit,
    subtitleDelay: Float,
    onSubtitleDelayChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onDisableSubtitles: () -> Unit,
    onSelectTrack: (Tracks.Group, Int) -> Unit,
    onLoadExternal: () -> Unit
) {
    val textTrackGroups = remember(tracks) {
        tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    }
    val hasTracks = textTrackGroups.isNotEmpty()
    val isAnySelected = textTrackGroups.any { it.isSelected }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                stringResource(R.string.subtitles_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDisableSubtitles()
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isAnySelected,
                        onClick = {
                            onDisableSubtitles()
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.subtitles_off),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (!isAnySelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (!hasTracks) {
                    Text(
                        stringResource(R.string.subtitles_none_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                } else {
                    textTrackGroups.forEach { group ->
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val isSelected = group.isTrackSelected(i)
                            val trackName = format.label
                                ?: format.language?.uppercase(Locale.getDefault())
                                ?: "Track ${i + 1}"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectTrack(group, i)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectTrack(group, i)
                                        onDismiss()
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    trackName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLoadExternal()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.subtitles_load_external))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Text(
                    stringResource(R.string.section_subtitles),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    stringResource(R.string.subtitle_size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sizeRow1 = listOf(
                        14 to stringResource(R.string.size_small),
                        18 to stringResource(R.string.size_normal)
                    )
                    val sizeRow2 = listOf(
                        22 to stringResource(R.string.size_large),
                        26 to stringResource(R.string.size_extra_large)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizeRow1.forEach { (size, label) ->
                            val isSelected = (subtitleTextSize == size)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSubtitleTextSizeChange(size) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizeRow2.forEach { (size, label) ->
                            val isSelected = (subtitleTextSize == size)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSubtitleTextSizeChange(size) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.subtitle_color),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colorList = listOf(
                        0 to stringResource(R.string.color_white),
                        1 to stringResource(R.string.color_yellow),
                        2 to stringResource(R.string.color_cyan)
                    )
                    colorList.forEach { (col, label) ->
                        val isSelected = (subtitleTextColor == col)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSubtitleTextColorChange(col) },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Text(
                    stringResource(R.string.subtitle_background),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val bgRow1 = listOf(
                        0 to stringResource(R.string.bg_transparent),
                        2 to stringResource(R.string.bg_black)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bgRow1.forEach { (bg, label) ->
                            val isSelected = (subtitleBgStyle == bg)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSubtitleBgStyleChange(bg) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    val isSemiSelected = (subtitleBgStyle == 1)
                    FilterChip(
                        selected = isSemiSelected,
                        onClick = { onSubtitleBgStyleChange(1) },
                        label = { Text(stringResource(R.string.bg_semi_transparent), maxLines = 1) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                Text(
                    stringResource(R.string.subtitle_delay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSubtitleDelayChange(-0.5f) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.subtitle_delay_decrease))
                    }
                    Text(
                        stringResource(R.string.delay_format, subtitleDelay),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = { onSubtitleDelayChange(0.5f) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.subtitle_delay_increase))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
