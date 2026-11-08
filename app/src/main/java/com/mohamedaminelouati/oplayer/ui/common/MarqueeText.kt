package com.mohamedaminelouati.oplayer.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mohamedaminelouati.oplayer.data.TextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.LocalTextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.maxLines
import com.mohamedaminelouati.oplayer.ui.theme.overflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal fun isRtlText(text: String, isLayoutRtl: Boolean): Boolean {
    var i = 0
    while (i < text.length) {
        val codePoint = text.codePointAt(i)
        val directionality = Character.getDirectionality(codePoint)
        if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
            directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
        ) {
            return true
        }
        if (directionality == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
            return false
        }
        i += Character.charCount(codePoint)
    }
    return isLayoutRtl
}

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    forceMarquee: Boolean = false
) {
    val truncation = LocalTextTruncationMode.current
    val isOneLine = forceMarquee || truncation == TextTruncationMode.ONE_LINE

    if (!isOneLine) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            style = style,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = truncation.maxLines,
            overflow = truncation.overflow
        )
        return
    }

    if (text.isEmpty()) {
        Text(
            text = "",
            modifier = modifier,
            style = style
        )
        return
    }

    val density = LocalDensity.current
    val isLayoutRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val isRtl = remember(text, isLayoutRtl) { isRtlText(text, isLayoutRtl) }

    val resolvedStyle = remember(style, isRtl) {
        if (style.textDirection == TextDirection.Unspecified) {
            style.copy(textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr)
        } else {
            style
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = remember(text, resolvedStyle, fontWeight) {
        textMeasurer.measure(
            text = text,
            style = if (fontWeight != null) resolvedStyle.copy(fontWeight = fontWeight) else resolvedStyle,
            maxLines = 1,
            softWrap = false
        )
    }

    val textWidthPx = textLayoutResult.size.width
    val textHeightPx = textLayoutResult.size.height
    val spacingPx = with(density) { 36.dp.roundToPx() }
    val heightDp = with(density) { textHeightPx.coerceAtLeast(1).toDp() }

    val coroutineScope = rememberCoroutineScope()
    val autoOffsetAnim = remember(text) { Animatable(0f) }
    var manualOffsetPx by remember(text) { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    val textColor = if (color != Color.Unspecified) {
        color
    } else if (style.color != Color.Unspecified) {
        style.color
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {
        val containerWidthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else textWidthPx
        val shouldScroll = containerWidthPx in 1 until textWidthPx
        val scrollDistance = textWidthPx + spacingPx

        LaunchedEffect(text, shouldScroll, scrollDistance, isDragging, lastInteractionTime) {
            if (!shouldScroll || isDragging) {
                if (!shouldScroll) {
                    autoOffsetAnim.snapTo(0f)
                }
                return@LaunchedEffect
            }
            val durationMs = (scrollDistance / (density.density * 35f) * 1000f).roundToInt().coerceAtLeast(1000)
            if (lastInteractionTime > 0L) {
                delay(1500L)
            } else {
                delay(1200L)
            }
            while (!isDragging) {
                val startVal = autoOffsetAnim.value
                val targetVal = startVal + scrollDistance
                autoOffsetAnim.animateTo(
                    targetValue = targetVal,
                    animationSpec = tween(
                        durationMillis = durationMs,
                        easing = LinearEasing
                    )
                )
                autoOffsetAnim.snapTo(targetVal % scrollDistance)
                delay(600L)
            }
        }

        val draggableState = rememberDraggableState { delta ->
            val step = if (isRtl) delta else -delta
            manualOffsetPx += step
        }

        val dragModifier = if (shouldScroll) {
            Modifier.draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    isDragging = true
                },
                onDragStopped = {
                    val combined = autoOffsetAnim.value + manualOffsetPx
                    val modVal = if (scrollDistance > 0) {
                        ((combined % scrollDistance) + scrollDistance) % scrollDistance
                    } else 0f
                    coroutineScope.launch {
                        autoOffsetAnim.snapTo(modVal)
                        manualOffsetPx = 0f
                        isDragging = false
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
            )
        } else {
            Modifier
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .then(dragModifier)
        ) {
            val cw = size.width
            val tw = textWidthPx.toFloat()
            if (tw <= 0f || cw <= 0f) return@Canvas

            if (!shouldScroll) {
                val x = if (isRtl) {
                    cw - tw
                } else when (textAlign) {
                    TextAlign.Center -> (cw - tw) / 2f
                    TextAlign.End -> cw - tw
                    else -> 0f
                }
                drawText(textLayoutResult, color = textColor, topLeft = Offset(x, 0f))
                return@Canvas
            }

            val dist = scrollDistance.toFloat()
            val rawShift = if (isDragging) {
                autoOffsetAnim.value + manualOffsetPx
            } else {
                autoOffsetAnim.value
            }
            val shift = if (dist > 0f) {
                ((rawShift % dist) + dist) % dist
            } else 0f

            if (isRtl) {
                val xStart = cw - tw
                val x1 = xStart + shift
                val x2 = x1 - dist
                drawText(textLayoutResult, color = textColor, topLeft = Offset(x1, 0f))
                drawText(textLayoutResult, color = textColor, topLeft = Offset(x2, 0f))
            } else {
                val x1 = -shift
                val x2 = x1 + dist
                drawText(textLayoutResult, color = textColor, topLeft = Offset(x1, 0f))
                drawText(textLayoutResult, color = textColor, topLeft = Offset(x2, 0f))
            }
        }
    }
}
