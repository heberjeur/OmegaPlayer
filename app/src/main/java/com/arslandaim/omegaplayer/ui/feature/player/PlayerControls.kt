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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslandaim.omegaplayer.R
import kotlinx.coroutines.launch

@Composable
fun CenterPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
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
    modifier: Modifier = Modifier
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
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(36.dp))
    }
}
