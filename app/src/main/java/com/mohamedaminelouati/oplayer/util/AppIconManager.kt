package com.mohamedaminelouati.oplayer.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.mohamedaminelouati.oplayer.MainActivity
import com.mohamedaminelouati.oplayer.R
import com.mohamedaminelouati.oplayer.data.AppIconStyle

object AppIconManager {

    fun setAppIcon(context: Context, style: AppIconStyle) {
        val packageManager = context.packageManager
        val packageName = context.packageName

        try {
            val targetComponent = ComponentName(packageName, style.aliasName)
            if (packageManager.getComponentEnabledSetting(targetComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                packageManager.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            AppIconStyle.entries.forEach { iconStyle ->
                if (iconStyle != style) {
                    val componentName = ComponentName(packageName, iconStyle.aliasName)
                    if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        packageManager.setComponentEnabledSetting(
                            componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyIconForColors(context: Context, primaryColor: Int, secondaryColor: Int) {
        val style = AppIconStyle.findClosestStyle(primaryColor, secondaryColor)
        setAppIcon(context, style)
        updateShortcutIfPinned(context, primaryColor, secondaryColor)
    }

    private fun updateShortcutIfPinned(context: Context, primaryColor: Int, secondaryColor: Int) {
        try {
            val size = 192
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    secondaryColor, primaryColor,
                    Shader.TileMode.CLAMP
                )
            }
            val cornerRadius = size * 0.22f
            canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), cornerRadius, cornerRadius, paint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = size * 0.52f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val textBounds = Rect()
            textPaint.getTextBounds("Ω", 0, 1, textBounds)
            val yOffset = (size / 2f) + (textBounds.height() / 2f) - textBounds.bottom - 4f
            canvas.drawText("Ω", size / 2f, yOffset, textPaint)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val shortcutInfo = ShortcutInfoCompat.Builder(context, "custom_omega_player_shortcut")
                .setShortLabel(context.getString(R.string.app_name))
                .setIcon(IconCompat.createWithBitmap(bitmap))
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.updateShortcuts(context, listOf(shortcutInfo))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
