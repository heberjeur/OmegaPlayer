package com.arslandaim.omegaplayer.ui.feature.player

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.scale
import androidx.annotation.OptIn as AndroidOptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.media.PlayerRepeatMode
import kotlinx.coroutines.launch

enum class CenterActionType {
    NONE,
    REWIND,
    PLAY_PAUSE,
    FORWARD
}

@Composable
fun CenterPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trigger: Int = 0
) {
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0.5f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            launch {
                scale.animateTo(1.3f, animationSpec = tween(150))
                scale.animateTo(1f, animationSpec = tween(150))
            }
            launch {
                alpha.animateTo(0.8f, animationSpec = tween(150))
                alpha.animateTo(0.5f, animationSpec = tween(150))
            }
        }
    }

    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .scale(scale.value)
            .background(Color.Black.copy(alpha = alpha.value), CircleShape),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        )
    ) {
        Icon(
            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play),
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun SeekControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    trigger: Int,
    consumedTrigger: Int,
    onConsume: (Int) -> Unit,
    modifier: Modifier = Modifier,
    secondsText: String? = null
) {
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0.5f) }

    LaunchedEffect(trigger) {
        if (trigger > consumedTrigger) {
            onConsume(trigger)
            launch {
                scale.animateTo(1.3f, animationSpec = tween(150))
                scale.animateTo(1f, animationSpec = tween(150))
            }
            launch {
                alpha.animateTo(0.8f, animationSpec = tween(150))
                alpha.animateTo(0.5f, animationSpec = tween(150))
            }
        }
    }

    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .scale(scale.value)
            .background(Color.Black.copy(alpha = alpha.value), CircleShape),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        )
    ) {
        if (secondsText != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
                Text(
                    text = secondsText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        } else {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun RepeatControlButton(
    repeatMode: PlayerRepeatMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(repeatMode.iconRes),
            contentDescription = stringResource(R.string.action_repeat),
            tint = if (repeatMode.isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CenterPlaybackControlsRow(
    isPlaying: Boolean,
    activeCenterAction: CenterActionType,
    isCenterControlsVisible: Boolean,
    playPausePulse: Int,
    seekBackwardPulse: Int,
    seekForwardPulse: Int,
    consumedSeekBackwardPulse: Int,
    consumedSeekForwardPulse: Int,
    onConsumeSeekBackward: (Int) -> Unit,
    onConsumeSeekForward: (Int) -> Unit,
    lastSeekAmountSeconds: Int,
    onRewindClick: () -> Unit,
    onForwardClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp = 32.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = activeCenterAction == CenterActionType.REWIND || isCenterControlsVisible,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f, animationSpec = tween(150)),
                exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.7f, animationSpec = tween(250))
            ) {
                SeekControlButton(
                    icon = Icons.Default.FastRewind,
                    contentDescription = stringResource(R.string.action_rewind),
                    onClick = onRewindClick,
                    trigger = seekBackwardPulse,
                    consumedTrigger = consumedSeekBackwardPulse,
                    onConsume = onConsumeSeekBackward,
                    secondsText = "${lastSeekAmountSeconds}s"
                )
            }
        }

        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = activeCenterAction == CenterActionType.PLAY_PAUSE || isCenterControlsVisible,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f, animationSpec = tween(150)),
                exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.7f, animationSpec = tween(250))
            ) {
                CenterPlayPauseButton(
                    isPlaying = isPlaying,
                    trigger = playPausePulse,
                    onClick = onPlayPauseClick
                )
            }
        }

        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = activeCenterAction == CenterActionType.FORWARD || isCenterControlsVisible,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f, animationSpec = tween(150)),
                exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.7f, animationSpec = tween(250))
            ) {
                SeekControlButton(
                    icon = Icons.Default.FastForward,
                    contentDescription = stringResource(R.string.action_forward),
                    onClick = onForwardClick,
                    trigger = seekForwardPulse,
                    consumedTrigger = consumedSeekForwardPulse,
                    onConsume = onConsumeSeekForward,
                    secondsText = "${lastSeekAmountSeconds}s"
                )
            }
        }
    }
}

