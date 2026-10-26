package com.mohamedaminelouati.oplayer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import com.mohamedaminelouati.oplayer.data.AppTheme
import com.mohamedaminelouati.oplayer.data.TextTruncationMode

val TextTruncationMode.maxLines: Int
    get() = when (this) {
        TextTruncationMode.TWO_LINES -> 2
        TextTruncationMode.THREE_LINES -> 3
        TextTruncationMode.FULL_NAME -> Int.MAX_VALUE
    }

val TextTruncationMode.overflow: TextOverflow
    get() = when (this) {
        TextTruncationMode.TWO_LINES,
        TextTruncationMode.THREE_LINES -> TextOverflow.Ellipsis
        TextTruncationMode.FULL_NAME -> TextOverflow.Clip
    }

val LocalTextTruncationMode = compositionLocalOf { TextTruncationMode.TWO_LINES }
val LocalSavePositionOnQuit = compositionLocalOf { true }
val LocalPlaybackProgress = compositionLocalOf { emptyMap<String, Float>() }

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = Error,
    onError = OnError
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = Error,
    onError = OnError
)

@Composable
fun OMediaPlayerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = true,
    textTruncationMode: TextTruncationMode = TextTruncationMode.TWO_LINES,
    savePositionOnQuit: Boolean = true,
    playbackProgressMap: Map<String, Float> = emptyMap(),
    darkPrimaryColor: androidx.compose.ui.graphics.Color = PrimaryDark,
    darkSecondaryColor: androidx.compose.ui.graphics.Color = SecondaryDark,
    lightPrimaryColor: androidx.compose.ui.graphics.Color = PrimaryLight,
    lightSecondaryColor: androidx.compose.ui.graphics.Color = SecondaryLight,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val customDarkScheme = DarkColorScheme.copy(
        primary = darkPrimaryColor,
        onPrimary = if (darkPrimaryColor.luminance() > 0.5f) Color.Black else Color.White,
        primaryContainer = darkPrimaryColor.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = darkSecondaryColor,
        onSecondary = if (darkSecondaryColor.luminance() > 0.5f) Color.Black else Color.White,
        secondaryContainer = darkSecondaryColor.copy(alpha = 0.25f),
        onSecondaryContainer = Color.White
    )

    val customLightScheme = LightColorScheme.copy(
        primary = lightPrimaryColor,
        onPrimary = if (lightPrimaryColor.luminance() > 0.5f) Color.Black else Color.White,
        primaryContainer = lightPrimaryColor.copy(alpha = 0.15f),
        onPrimaryContainer = lightPrimaryColor,
        secondary = lightSecondaryColor,
        onSecondary = if (lightSecondaryColor.luminance() > 0.5f) Color.Black else Color.White,
        secondaryContainer = lightSecondaryColor.copy(alpha = 0.15f),
        onSecondaryContainer = lightSecondaryColor
    )

    val rawColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> customDarkScheme
        else -> customLightScheme
    }
    val colorScheme = rawColorScheme.copy(
        error = rawColorScheme.primary,
        onError = androidx.compose.ui.graphics.Color.White
    )

    CompositionLocalProvider(
        LocalTextTruncationMode provides textTruncationMode,
        LocalSavePositionOnQuit provides savePositionOnQuit,
        LocalPlaybackProgress provides playbackProgressMap
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}