package com.arslandaim.omegaplayer.ui.feature.player

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.OptIn as AndroidOptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.unit.sp
import com.arslandaim.omegaplayer.R
import java.util.Locale
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration

@Composable
fun PlaybackErrorOverlay(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.playback_error),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PlayerTopHUD(
    showClock: Boolean,
    currentTime: String,
    showBattery: Boolean,
    batteryPercentage: Int,
    showVolume: Boolean,
    volumePercentage: Int,
    showBrightness: Boolean,
    brightnessPercentage: Int,
    showMediaInfo: Boolean,
    resolution: String?,
    modifier: Modifier = Modifier
) {
    val itemsVisible = (showClock && currentTime.isNotEmpty()) ||
            showBattery ||
            showVolume ||
            showBrightness ||
            (showMediaInfo && !resolution.isNullOrBlank())

    if (!itemsVisible) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showClock && currentTime.isNotEmpty()) {
            HudBadge(
                icon = Icons.Default.AccessTime,
                text = currentTime
            )
        }
        if (showBattery) {
            HudBadge(
                icon = Icons.Default.BatteryChargingFull,
                text = "$batteryPercentage%"
            )
        }
        if (showVolume) {
            HudBadge(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                text = "$volumePercentage%"
            )
        }
        if (showBrightness) {
            HudBadge(
                icon = Icons.Default.BrightnessMedium,
                text = "$brightnessPercentage%"
            )
        }
        if (showMediaInfo && !resolution.isNullOrBlank()) {
            HudBadge(
                icon = Icons.Default.HighQuality,
                text = resolution
            )
        }
    }
}

@Composable
private fun HudBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.95f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlayerDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    playbackSpeed: Float,
    sleepTimerActive: Boolean,
    sleepTimerTimeLeft: Long,
    onPlaylistClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_add_to_playlist)) },
            onClick = {
                onDismiss()
                onPlaylistClick()
            },
            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
        )
        DropdownMenuItem(
            text = {
                val text = if (sleepTimerActive) stringResource(R.string.sleep_timer_format, formatDuration(sleepTimerTimeLeft))
                else stringResource(R.string.sleep_timer)
                Text(text)
            },
            onClick = {
                onDismiss()
                onSleepTimerClick()
            },
            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = if (sleepTimerActive) MaterialTheme.colorScheme.primary else Color.White) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.playback_speed_format, String.format(java.util.Locale.US, "%.2f", playbackSpeed))) },
            onClick = {
                onDismiss()
                onSpeedClick()
            },
            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.equalizer)) },
            onClick = {
                onDismiss()
                onEqualizerClick()
            },
            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_information)) },
            onClick = {
                onDismiss()
                onInfoClick()
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                onDeleteClick()
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}

@Composable
fun VerticalIndicator(
    value: Float, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    visible: Boolean, 
    text: String,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(targetValue = value)
    AnimatedVisibility(
        visible = visible, 
        enter = fadeIn() + slideInHorizontally { if (modifier.toString().contains("CenterStart")) -20 else 20 }, 
        exit = fadeOut() + slideOutHorizontally { if (modifier.toString().contains("CenterStart")) -20 else 20 }, 
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            modifier = Modifier
                .width(40.dp)
                .height(200.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .weight(1f)
                    .background(Color(0xFF424242), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp)), 
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedValue)
                        .background(color)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}
