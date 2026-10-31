package com.mohamedaminelouati.oplayer.data

import androidx.compose.ui.graphics.Color
import com.mohamedaminelouati.oplayer.R

enum class AppIconStyle(
    val id: String,
    val titleRes: Int,
    val aliasName: String,
    val startColor: Color,
    val endColor: Color
) {
    DEFAULT(
        id = "default",
        titleRes = R.string.app_icon_default,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityDefault",
        startColor = Color(0xFF87CEEB),
        endColor = Color(0xFFFF6600)
    ),
    BLUE(
        id = "blue",
        titleRes = R.string.app_icon_blue,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityBlue",
        startColor = Color(0xFF38BDF8),
        endColor = Color(0xFF0284C7)
    ),
    ORANGE(
        id = "orange",
        titleRes = R.string.app_icon_orange,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityOrange",
        startColor = Color(0xFFF59E0B),
        endColor = Color(0xFFFF5500)
    ),
    GREEN(
        id = "green",
        titleRes = R.string.app_icon_green,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityGreen",
        startColor = Color(0xFF10B981),
        endColor = Color(0xFF047857)
    ),
    PURPLE(
        id = "purple",
        titleRes = R.string.app_icon_purple,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityPurple",
        startColor = Color(0xFFA78BFA),
        endColor = Color(0xFF6366F1)
    ),
    PINK(
        id = "pink",
        titleRes = R.string.app_icon_pink,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityPink",
        startColor = Color(0xFFF43F5E),
        endColor = Color(0xFFEC4899)
    ),
    RED(
        id = "red",
        titleRes = R.string.app_icon_red,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityRed",
        startColor = Color(0xFFEF4444),
        endColor = Color(0xFF991B1B)
    ),
    DARK(
        id = "dark",
        titleRes = R.string.app_icon_dark,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityDark",
        startColor = Color(0xFF3F3F46),
        endColor = Color(0xFF09090B)
    );

    companion object {
        fun fromId(id: String): AppIconStyle = entries.find { it.id == id } ?: DEFAULT
    }
}

