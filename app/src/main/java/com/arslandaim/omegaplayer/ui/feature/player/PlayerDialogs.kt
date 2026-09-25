package com.arslandaim.omegaplayer.ui.feature.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable
import com.arslandaim.omegaplayer.data.Playlist
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Switch
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay

@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    stopAfterCurrent: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onStopAfterCurrentToggle: (Boolean) -> Unit
) {
    var minutes by remember { mutableIntStateOf(if (currentMinutes > 0) currentMinutes else 30) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!stopAfterCurrent) {
                    Text(stringResource(R.string.minutes_format, minutes))
                    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                    Slider(
                        value = minutes.toFloat(),
                        onValueChange = { minutes = it.toInt() },
                        valueRange = 0f..120f,
                        steps = 23,
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        },
                        track = { sliderState ->
                            androidx.compose.material3.SliderDefaults.Track(
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                sliderState = sliderState,
                                modifier = Modifier.height(2.dp)
                            )
                        }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = stopAfterCurrent,
                        onCheckedChange = { onStopAfterCurrentToggle(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.stop_after_current))
                }

                if (currentMinutes > 0 || stopAfterCurrent) {
                    TextButton(onClick = { 
                        onConfirm(0)
                        onStopAfterCurrentToggle(false)
                    }) {
                        Text(stringResource(R.string.action_turn_off), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (!stopAfterCurrent) {
                Button(onClick = { onConfirm(minutes) }) {
                    Text(stringResource(R.string.action_set))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Int) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.menu_new_playlist)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        onCreatePlaylist(newPlaylistName)
                        showCreateDialog = false
                    }
                }) { Text(stringResource(R.string.action_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_add_to_playlist)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.menu_new_playlist))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.no_playlists_yet), modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                leadingContent = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                                modifier = Modifier.clickable { onPlaylistSelected(playlist.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
fun AudioEqualizerDialog(
    eqManager: com.arslandaim.omegaplayer.media.EqManager,
    onDismiss: () -> Unit
) {
    val enabled by eqManager.enabled.collectAsState()
    val bands by eqManager.bands.collectAsState()
    val presets by eqManager.presets.collectAsState()
    
    var selectedPreset by remember { mutableStateOf("Custom") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.audio_equalizer), fontWeight = FontWeight.Bold)
                }
                Switch(checked = enabled, onCheckedChange = { eqManager.setEnabled(it) })
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (presets.isNotEmpty()) {
                    Text(stringResource(R.string.presets), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presets) { presetName ->
                            val index = presets.indexOf(presetName).toShort()
                            FilterChip(
                                selected = selectedPreset == presetName,
                                onClick = {
                                    selectedPreset = presetName
                                    eqManager.usePreset(index)
                                },
                                label = { Text(presetName) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (bands.isNotEmpty()) {
                    bands.forEach { band ->
                        val freqStr = if (band.centerFreq >= 1000000) {
                            "${band.centerFreq / 1000000} kHz"
                        } else {
                            "${band.centerFreq / 1000} Hz"
                        }
                        Text(freqStr, style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = band.level.toFloat(),
                            valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                            onValueChange = { 
                                eqManager.setBandLevel(band.id, it.toInt().toShort())
                                selectedPreset = "Custom"
                            },
                            enabled = enabled
                        )
                    }
                } else {
                    Text(stringResource(R.string.error_not_supported), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
