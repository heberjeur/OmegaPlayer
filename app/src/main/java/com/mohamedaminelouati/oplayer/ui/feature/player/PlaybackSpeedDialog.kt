package com.mohamedaminelouati.oplayer.ui.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedaminelouati.oplayer.R

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    skipSilenceEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Float, Boolean, Boolean) -> Unit
) {
    var speed by remember {
        mutableFloatStateOf(round(currentSpeed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED) * 100f) / 100f)
    }
    var applyTemporarily by remember { mutableStateOf(false) }
    var skipSilence by remember { mutableStateOf(skipSilenceEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playback_speed)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.speed_multiplier_format, String.format(java.util.Locale.US, "%.2f", speed)),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                PlaybackSpeedSlider(
                    speed = speed,
                    onSpeedChange = { speed = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.speed_multiplier_format, MIN_PLAYBACK_SPEED.toString()), color = Color.Gray)
                    Text(stringResource(R.string.speed_multiplier_format, MAX_PLAYBACK_SPEED.toString()), color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { skipSilence = !skipSilence }
                ) {
                    Checkbox(
                        checked = skipSilence,
                        onCheckedChange = { skipSilence = it }
                    )
                    Text(stringResource(R.string.skip_silence), style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { applyTemporarily = !applyTemporarily }
                ) {
                    Checkbox(
                        checked = applyTemporarily,
                        onCheckedChange = { applyTemporarily = it }
                    )
                    Text(stringResource(R.string.apply_speed_temporarily), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(speed, applyTemporarily, skipSilence) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.action_set), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

fun snapPlaybackSpeed(rawSpeed: Float, threshold: Float = 0.02f): Float {
    val roundedTwoDecimals = round(rawSpeed * 100f) / 100f
    val nearestTenth = round(roundedTwoDecimals * 10f) / 10f
    return if (abs(roundedTwoDecimals - nearestTenth) <= threshold) {
        nearestTenth
    } else {
        roundedTwoDecimals
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSlider(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var lastSnappedTenth by remember { mutableFloatStateOf(round(speed * 10f) / 10f) }

    Slider(
        value = speed,
        onValueChange = {
            val snapped = snapPlaybackSpeed(it)
            val tenth = round(snapped * 10f) / 10f
            if (abs(snapped - tenth) < 0.001f && tenth != lastSnappedTenth) {
                lastSnappedTenth = tenth
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else if (abs(snapped - tenth) >= 0.001f) {
                lastSnappedTenth = -1f
            }
            onSpeedChange(snapped)
        },
        valueRange = MIN_PLAYBACK_SPEED..MAX_PLAYBACK_SPEED,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        },
        track = { sliderState ->
            val activeTrackColor = MaterialTheme.colorScheme.primary
            val inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            val activeDotColor = MaterialTheme.colorScheme.onPrimary
            val inactiveDotColor = MaterialTheme.colorScheme.onSurfaceVariant

            SliderDefaults.Track(
                colors = SliderDefaults.colors(
                    activeTrackColor = activeTrackColor,
                    inactiveTrackColor = inactiveTrackColor
                ),
                sliderState = sliderState,
                modifier = Modifier
                    .height(4.dp)
                    .drawWithContent {
                        drawContent()
                        val range = MAX_PLAYBACK_SPEED - MIN_PLAYBACK_SPEED
                        if (range > 0f) {
                            val isRtl = layoutDirection == LayoutDirection.Rtl
                            val startX = if (isRtl) size.width else 0f
                            val endX = if (isRtl) 0f else size.width
                            val activeFraction = ((sliderState.value - MIN_PLAYBACK_SPEED) / range).coerceIn(0f, 1f)

                            val minStep = round(MIN_PLAYBACK_SPEED * 10f).toInt()
                            val maxStep = round(MAX_PLAYBACK_SPEED * 10f).toInt()

                            for (step in minStep..maxStep) {
                                val pointSpeed = step / 10f
                                val fraction = (pointSpeed - MIN_PLAYBACK_SPEED) / range
                                val x = startX + (endX - startX) * fraction
                                val isPassed = fraction <= activeFraction + 0.001f
                                val isMajor = step % 10 == 0
                                val isHalf = step % 5 == 0
                                val dotRadius = when {
                                    isMajor -> 3.dp.toPx()
                                    isHalf -> 2.25.dp.toPx()
                                    else -> 1.75.dp.toPx()
                                }
                                val dotColor = if (isPassed) activeDotColor else inactiveDotColor

                                drawCircle(
                                    color = dotColor,
                                    radius = dotRadius,
                                    center = Offset(x, center.y)
                                )
                            }
                        }
                    }
            )
        }
    )
}
