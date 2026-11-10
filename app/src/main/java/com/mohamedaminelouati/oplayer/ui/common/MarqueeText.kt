package com.mohamedaminelouati.oplayer.ui.common

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mohamedaminelouati.oplayer.data.TextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.LocalTextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.maxLines
import com.mohamedaminelouati.oplayer.ui.theme.overflow
import kotlinx.coroutines.delay

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
    forceMarquee: Boolean = true
) {
    if (text.isEmpty()) {
        Text(
            text = "",
            modifier = modifier,
            style = style
        )
        return
    }

    val isLayoutRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val isRtl = remember(text, isLayoutRtl) { isRtlText(text, isLayoutRtl) }

    val resolvedStyle = remember(style, isRtl) {
        if (style.textDirection == TextDirection.Unspecified) {
            style.copy(textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr)
        } else {
            style
        }
    }

    val truncation = LocalTextTruncationMode.current
    val isOneLine = forceMarquee || truncation == TextTruncationMode.ONE_LINE

    if (!isOneLine) {
        CompositionLocalProvider(LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
            Text(
                text = text,
                modifier = modifier,
                color = color,
                style = resolvedStyle,
                fontWeight = fontWeight,
                textAlign = textAlign,
                maxLines = truncation.maxLines,
                overflow = truncation.overflow
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = remember(text, resolvedStyle, textMeasurer) {
        textMeasurer.measure(
            text = text,
            style = resolvedStyle,
            maxLines = 1,
            softWrap = false
        )
    }

    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {
        val containerWidthPx = constraints.maxWidth
        val textWidthPx = textLayoutResult.size.width
        val shouldScroll = textWidthPx > containerWidthPx && containerWidthPx > 0

        if (!shouldScroll) {
            CompositionLocalProvider(LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                Text(
                    text = text,
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    style = resolvedStyle,
                    fontWeight = fontWeight,
                    textAlign = textAlign ?: (if (isRtl) TextAlign.End else TextAlign.Start),
                    maxLines = 1,
                    softWrap = false
                )
            }
            return@BoxWithConstraints
        }

        val density = LocalDensity.current
        val spacingPx = with(density) { 36.dp.toPx() }
        val cycleWidth = textWidthPx + spacingPx

        var currentOffset by remember(text, cycleWidth) { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }
        var lastTouchTime by remember { mutableLongStateOf(0L) }

        LaunchedEffect(text, cycleWidth, isRtl) {
            currentOffset = 0f
            lastTouchTime = 0L

            val velocityPxPerSec = with(density) { 30.dp.toPx() }
            val direction = if (isRtl) 1f else -1f

            while (true) {
                delay(1200L)

                var cycleProgress = 0f
                var lastFrameNanos = 0L

                while (cycleProgress < cycleWidth) {
                    if (isDragging || System.currentTimeMillis() - lastTouchTime < 1500L) {
                        lastFrameNanos = 0L
                        delay(50L)
                        continue
                    }

                    withFrameNanos { frameTimeNanos ->
                        if (lastFrameNanos != 0L) {
                            val dt = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                            val delta = velocityPxPerSec * dt
                            cycleProgress += delta
                            currentOffset += direction * delta
                        }
                        lastFrameNanos = frameTimeNanos
                    }
                }
            }
        }

        val (x1, x2) = if (isRtl) {
            val startX = (containerWidthPx - textWidthPx).toFloat()
            val rem = ((currentOffset % cycleWidth) + cycleWidth) % cycleWidth
            val first = startX + rem
            val second = first - cycleWidth
            Pair(first, second)
        } else {
            val rem = ((currentOffset % cycleWidth) - cycleWidth) % cycleWidth
            val first = if (rem > 0f) rem - cycleWidth else rem
            val second = first + cycleWidth
            Pair(first, second)
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(cycleWidth) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging = true
                            },
                            onDragEnd = {
                                isDragging = false
                                lastTouchTime = System.currentTimeMillis()
                            },
                            onDragCancel = {
                                isDragging = false
                                lastTouchTime = System.currentTimeMillis()
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                currentOffset += dragAmount
                            }
                        )
                    }
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .layout { measurable, childConstraints ->
                                val placeable = measurable.measure(childConstraints.copy(maxWidth = Constraints.Infinity))
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                            .graphicsLayer {
                                translationX = x1
                            },
                        color = color,
                        style = resolvedStyle,
                        fontWeight = fontWeight,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .layout { measurable, childConstraints ->
                                val placeable = measurable.measure(childConstraints.copy(maxWidth = Constraints.Infinity))
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                            .graphicsLayer {
                                translationX = x2
                            },
                        color = color,
                        style = resolvedStyle,
                        fontWeight = fontWeight,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
