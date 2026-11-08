package com.mohamedaminelouati.oplayer.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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

    var textWidthPx by remember(text) { mutableIntStateOf(0) }
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val spacingPx = with(density) { 36.dp.roundToPx() }

    val shouldScroll = containerWidthPx > 0 && textWidthPx > containerWidthPx
    val scrollDistance = textWidthPx + spacingPx

    val coroutineScope = rememberCoroutineScope()
    val autoOffsetAnim = remember(text) { Animatable(0f) }
    var manualOffsetPx by remember(text) { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(text, shouldScroll, isDragging, lastInteractionTime) {
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
        manualOffsetPx += delta
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                enabled = shouldScroll,
                onDragStarted = {
                    isDragging = true
                },
                onDragStopped = {
                    val combined = autoOffsetAnim.value - manualOffsetPx
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
            .drawWithContent {
                val cw = size.width
                val tw = textWidthPx.toFloat()
                if (tw <= 0f || cw <= 0f) {
                    drawContent()
                    return@drawWithContent
                }

                if (!shouldScroll) {
                    val x = if (isRtl) {
                        cw - tw
                    } else when (textAlign) {
                        TextAlign.Center -> (cw - tw) / 2f
                        TextAlign.End -> cw - tw
                        else -> 0f
                    }
                    translate(left = x, top = 0f) {
                        this@drawWithContent.drawContent()
                    }
                    return@drawWithContent
                }

                val dist = scrollDistance.toFloat()
                val rawShift = if (isDragging) {
                    autoOffsetAnim.value - manualOffsetPx
                } else {
                    autoOffsetAnim.value
                }
                val shift = if (dist > 0f) {
                    ((rawShift % dist) + dist) % dist
                } else 0f

                if (isRtl) {
                    val xStart = cw - tw
                    val x1 = xStart - shift
                    val x2 = x1 + dist
                    translate(left = x1, top = 0f) {
                        this@drawWithContent.drawContent()
                    }
                    translate(left = x2, top = 0f) {
                        this@drawWithContent.drawContent()
                    }
                } else {
                    val x1 = -shift
                    val x2 = x1 + dist
                    translate(left = x1, top = 0f) {
                        this@drawWithContent.drawContent()
                    }
                    translate(left = x2, top = 0f) {
                        this@drawWithContent.drawContent()
                    }
                }
            }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
            Text(
                text = text,
                color = color,
                style = resolvedStyle,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
                    )
                    val parentWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                    if (placeable.width != textWidthPx || parentWidth != containerWidthPx) {
                        textWidthPx = placeable.width
                        containerWidthPx = parentWidth
                    }
                    layout(parentWidth, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
            )
        }
    }
}
