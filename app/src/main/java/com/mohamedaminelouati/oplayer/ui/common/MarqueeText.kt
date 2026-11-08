package com.mohamedaminelouati.oplayer.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import com.mohamedaminelouati.oplayer.data.TextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.LocalTextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.maxLines
import com.mohamedaminelouati.oplayer.ui.theme.overflow
import kotlinx.coroutines.delay
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

    val scrollState = rememberScrollState()

    LaunchedEffect(text, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(0)
            while (true) {
                while (scrollState.isScrollInProgress) {
                    delay(300L)
                }
                delay(1200L)
                if (scrollState.isScrollInProgress) continue

                val target = scrollState.maxValue
                val remaining = (target - scrollState.value).coerceAtLeast(0)
                if (remaining > 0) {
                    val durationMs = (remaining / (density.density * 35f) * 1000f).roundToInt().coerceAtLeast(1000)
                    scrollState.animateScrollTo(
                        value = target,
                        animationSpec = tween(
                            durationMillis = durationMs,
                            easing = LinearEasing
                        )
                    )
                }
                delay(1200L)
                if (scrollState.isScrollInProgress) continue
                scrollState.scrollTo(0)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
        Row(
            modifier = modifier
                .clipToBounds()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = color,
                style = resolvedStyle,
                fontWeight = fontWeight,
                textAlign = textAlign,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
