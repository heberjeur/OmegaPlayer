package com.mohamedaminelouati.oplayer.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    ORANGE(
        id = "orange",
        titleRes = R.string.app_icon_orange,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityOrange",
        startColor = Color(0xFFF59E0B),
        endColor = Color(0xFFFF5500)
    ),
    BLUE(
        id = "blue",
        titleRes = R.string.app_icon_blue,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityBlue",
        startColor = Color(0xFF38BDF8),
        endColor = Color(0xFF0284C7)
    ),
    SKY_BLUE(
        id = "skyblue",
        titleRes = R.string.app_icon_skyblue,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivitySkyBlue",
        startColor = Color(0xFF7DD3FC),
        endColor = Color(0xFF38BDF8)
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
    AMBER(
        id = "amber",
        titleRes = R.string.app_icon_amber,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityAmber",
        startColor = Color(0xFFFBBF24),
        endColor = Color(0xFFF59E0B)
    ),
    TEAL(
        id = "teal",
        titleRes = R.string.app_icon_teal,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityTeal",
        startColor = Color(0xFF2DD4BF),
        endColor = Color(0xFF14B8A6)
    ),
    INDIGO(
        id = "indigo",
        titleRes = R.string.app_icon_indigo,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityIndigo",
        startColor = Color(0xFF818CF8),
        endColor = Color(0xFF6366F1)
    ),
    RED(
        id = "red",
        titleRes = R.string.app_icon_red,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityRed",
        startColor = Color(0xFFEF4444),
        endColor = Color(0xFF991B1B)
    ),
    LIME(
        id = "lime",
        titleRes = R.string.app_icon_lime,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivityLime",
        startColor = Color(0xFFA3E635),
        endColor = Color(0xFF84CC16)
    ),
    SLATE(
        id = "slate",
        titleRes = R.string.app_icon_slate,
        aliasName = "com.mohamedaminelouati.oplayer.MainActivitySlate",
        startColor = Color(0xFF94A3B8),
        endColor = Color(0xFF64748B)
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

        fun findClosestStyle(primaryColorInt: Int): AppIconStyle {
            val r1 = android.graphics.Color.red(primaryColorInt)
            val g1 = android.graphics.Color.green(primaryColorInt)
            val b1 = android.graphics.Color.blue(primaryColorInt)

            return entries.filter { it != DEFAULT && it != DARK }.minByOrNull { style ->
                val c = style.endColor.toArgb()
                val r2 = android.graphics.Color.red(c)
                val g2 = android.graphics.Color.green(c)
                val b2 = android.graphics.Color.blue(c)
                val dr = r1 - r2
                val dg = g1 - g2
                val db = b1 - b2
                dr * dr + dg * dg + db * db
            } ?: DEFAULT
        }
    }
}
