package com.mohamedaminelouati.oplayer.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mohamedaminelouati.oplayer.R

enum class AppIconStyle(
    val id: String,
    val titleRes: Int,
    val aliasName: String,
    val secondaryColor: Color,
    val primaryColor: Color
) {
    DEFAULT(
        id = "default",
        titleRes = R.string.app_icon_default,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityDefault",
        secondaryColor = Color(0xFF38BDF8),
        primaryColor = Color(0xFFFF5500)
    ),
    ORANGE(
        id = "orange",
        titleRes = R.string.app_icon_orange,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityOrange",
        secondaryColor = Color(0xFF0284C7),
        primaryColor = Color(0xFFFF5500)
    ),
    BLUE(
        id = "blue",
        titleRes = R.string.app_icon_blue,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityBlue",
        secondaryColor = Color(0xFFF59E0B),
        primaryColor = Color(0xFF0284C7)
    ),
    SKY_BLUE(
        id = "skyblue",
        titleRes = R.string.app_icon_skyblue,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivitySkyBlue",
        secondaryColor = Color(0xFF10B981),
        primaryColor = Color(0xFF38BDF8)
    ),
    GREEN(
        id = "green",
        titleRes = R.string.app_icon_green,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityGreen",
        secondaryColor = Color(0xFF38BDF8),
        primaryColor = Color(0xFF10B981)
    ),
    PURPLE(
        id = "purple",
        titleRes = R.string.app_icon_purple,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityPurple",
        secondaryColor = Color(0xFFEC4899),
        primaryColor = Color(0xFF8B5CF6)
    ),
    PINK(
        id = "pink",
        titleRes = R.string.app_icon_pink,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityPink",
        secondaryColor = Color(0xFF38BDF8),
        primaryColor = Color(0xFFEC4899)
    ),
    AMBER(
        id = "amber",
        titleRes = R.string.app_icon_amber,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityAmber",
        secondaryColor = Color(0xFF8B5CF6),
        primaryColor = Color(0xFFF59E0B)
    ),
    TEAL(
        id = "teal",
        titleRes = R.string.app_icon_teal,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityTeal",
        secondaryColor = Color(0xFF6366F1),
        primaryColor = Color(0xFF14B8A6)
    ),
    INDIGO(
        id = "indigo",
        titleRes = R.string.app_icon_indigo,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityIndigo",
        secondaryColor = Color(0xFF38BDF8),
        primaryColor = Color(0xFF6366F1)
    ),
    RED(
        id = "red",
        titleRes = R.string.app_icon_red,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityRed",
        secondaryColor = Color(0xFFF59E0B),
        primaryColor = Color(0xFFEF4444)
    ),
    LIME(
        id = "lime",
        titleRes = R.string.app_icon_lime,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityLime",
        secondaryColor = Color(0xFF14B8A6),
        primaryColor = Color(0xFF84CC16)
    ),
    SLATE(
        id = "slate",
        titleRes = R.string.app_icon_slate,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivitySlate",
        secondaryColor = Color(0xFF38BDF8),
        primaryColor = Color(0xFF64748B)
    ),
    DARK(
        id = "dark",
        titleRes = R.string.app_icon_dark,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityDark",
        secondaryColor = Color(0xFF0284C7),
        primaryColor = Color(0xFF18181B)
    );

    companion object {
        fun fromId(id: String): AppIconStyle = entries.find { it.id == id } ?: DEFAULT

        fun findClosestStyle(primaryInt: Int, secondaryInt: Int): AppIconStyle {
            val nonDefault = entries.filter { it != DEFAULT }
            val exactDefault = primaryInt == com.mohamedaminelouati.oplayer.ui.theme.DefaultDarkPrimaryInt &&
                    secondaryInt == com.mohamedaminelouati.oplayer.ui.theme.DefaultDarkSecondaryInt
            if (exactDefault) return DEFAULT

            return nonDefault.minByOrNull { style ->
                val distPrim = colorDistance(primaryInt, style.primaryColor.toArgb())
                val distSec = colorDistance(secondaryInt, style.secondaryColor.toArgb())
                distPrim * 2 + distSec
            } ?: DEFAULT
        }

        private fun colorDistance(c1: Int, c2: Int): Int {
            val r = android.graphics.Color.red(c1) - android.graphics.Color.red(c2)
            val g = android.graphics.Color.green(c1) - android.graphics.Color.green(c2)
            val b = android.graphics.Color.blue(c1) - android.graphics.Color.blue(c2)
            return r * r + g * g + b * b
        }
    }
}
