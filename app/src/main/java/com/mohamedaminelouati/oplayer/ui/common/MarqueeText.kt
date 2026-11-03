package com.mohamedaminelouati.oplayer.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import kotlin.math.roundToInt

private fun isRtlText(text: String, isLayoutRtl: Boolean): Boolean {
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

    val offsetAnim = remember(text) { Animatable(0f) }

    LaunchedEffect(text, shouldScroll, scrollDistance, isRtl) {
        if (!shouldScroll) {
            offsetAnim.snapTo(0f)
            return@LaunchedEffect
        }
        val durationMs = (scrollDistance / (density.density * 35f) * 1000f).roundToInt().coerceAtLeast(1000)
        while (true) {
            delay(1200L)
            offsetAnim.animateTo(
                targetValue = scrollDistance.toFloat(),
                animationSpec = tween(
                    durationMillis = durationMs,
                    easing = LinearEasing
                )
            )
            offsetAnim.snapTo(0f)
            delay(600L)
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .drawWithContent {
                val shift = offsetAnim.value
                val cw = size.width
                val tw = textWidthPx.toFloat()
                if (tw <= 0f || cw <= 0f) {
                    drawContent()
                } else if (!shouldScroll) {
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
                } else if (isRtl) {
                    val xStart = cw - tw
                    val x1 = xStart + shift
                    val x2 = xStart + shift - scrollDistance
                    translate(left = x1, top = 0f) {
                        this@drawWithContent.drawContent()
                    }
                    translate(left = x2, top = 0f) {
                        this@drawWithContent.drawContent()
                    }
                } else {
                    val x1 = -shift
                    val x2 = -shift + scrollDistance
                    translate(left = x1, top = 0f) {
                        this@drawWithContent.drawContent()
                    }
                    translate(left = x2, top = 0f) {
                        this@drawWithContent.drawContent()
                    }
                }
            }
    ) {
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
