package com.arslandaim.omegaplayer.ui.feature.security

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

@Composable
fun PatternLockView(
    title: String,
    subtitle: String? = null,
    errorMessage: String? = null,
    onPatternComplete: (String) -> Unit,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    val isError = !errorMessage.isNullOrEmpty()
    val activeColor = if (isError) errorColor else primaryColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!subtitle.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Spacer(modifier = Modifier.height(28.dp))
        }

        val canvasSize = 280.dp
        val hitRadiusPx = with(density) { 36.dp.toPx() }
        val dotRadiusPx = with(density) { 8.dp.toPx() }
        val outerRadiusPx = with(density) { 24.dp.toPx() }
        val lineWidthPx = with(density) { 4.dp.toPx() }

        Canvas(
            modifier = Modifier
                .size(canvasSize)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        onClearError()
                        selectedDots.clear()
                        currentTouchPos = down.position
                        val dotPositions = calculateDotPositions(size.width.toFloat(), size.height.toFloat())
                        for ((index, pos) in dotPositions.withIndex()) {
                            if (hypot((down.position.x - pos.x).toDouble(), (down.position.y - pos.y).toDouble()) <= hitRadiusPx) {
                                selectedDots.add(index)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                break
                            }
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                currentTouchPos = null
                                if (selectedDots.isNotEmpty()) {
                                    val patternString = selectedDots.joinToString("-")
                                    onPatternComplete(patternString)
                                }
                                break
                            }
                            change.consume()
                            currentTouchPos = change.position
                            for ((index, pos) in dotPositions.withIndex()) {
                                if (hypot((change.position.x - pos.x).toDouble(), (change.position.y - pos.y).toDouble()) <= hitRadiusPx) {
                                    if (!selectedDots.contains(index)) {
                                        selectedDots.add(index)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
        ) {
            val dotPositions = calculateDotPositions(size.width, size.height)

            for (i in 0 until selectedDots.size - 1) {
                val start = dotPositions[selectedDots[i]]
                val end = dotPositions[selectedDots[i + 1]]
                drawLine(
                    color = activeColor,
                    start = start,
                    end = end,
                    strokeWidth = lineWidthPx,
                    cap = StrokeCap.Round
                )
            }

            if (selectedDots.isNotEmpty() && currentTouchPos != null) {
                val lastPos = dotPositions[selectedDots.last()]
                drawLine(
                    color = activeColor.copy(alpha = 0.6f),
                    start = lastPos,
                    end = currentTouchPos!!,
                    strokeWidth = lineWidthPx,
                    cap = StrokeCap.Round
                )
            }

            for ((index, pos) in dotPositions.withIndex()) {
                val isSelected = selectedDots.contains(index)
                if (isSelected) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.2f),
                        radius = outerRadiusPx,
                        center = pos
                    )
                    drawCircle(
                        color = activeColor,
                        radius = dotRadiusPx * 1.3f,
                        center = pos
                    )
                } else {
                    drawCircle(
                        color = outlineColor,
                        radius = dotRadiusPx,
                        center = pos
                    )
                }
            }
        }
    }
}

private fun calculateDotPositions(width: Float, height: Float): List<Offset> {
    val positions = mutableListOf<Offset>()
    val cellW = width / 3f
    val cellH = height / 3f
    for (row in 0 until 3) {
        for (col in 0 until 3) {
            val x = col * cellW + cellW / 2f
            val y = row * cellH + cellH / 2f
            positions.add(Offset(x, y))
        }
    }
    return positions
}
