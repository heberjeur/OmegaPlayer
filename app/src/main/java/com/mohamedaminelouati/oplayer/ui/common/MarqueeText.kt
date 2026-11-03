package com.mohamedaminelouati.oplayer.ui.common

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import com.mohamedaminelouati.oplayer.data.TextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.LocalTextTruncationMode
import com.mohamedaminelouati.oplayer.ui.theme.maxLines
import com.mohamedaminelouati.oplayer.ui.theme.overflow

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

    val resolvedStyle = if (style.textDirection == TextDirection.Unspecified) {
        style.copy(textDirection = TextDirection.Content)
    } else {
        style
    }

    if (isOneLine) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = text,
                color = color,
                style = resolvedStyle,
                fontWeight = fontWeight,
                textAlign = textAlign ?: TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
        }
    } else {
        Text(
            text = text,
            color = color,
            style = resolvedStyle,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = truncation.maxLines,
            overflow = truncation.overflow,
            modifier = modifier
        )
    }
}

