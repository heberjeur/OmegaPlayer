package com.mohamedaminelouati.oplayer.util

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import androidx.core.content.ContextCompat
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
        (context as? Activity)?.let {
            updateTaskDescription(it, primaryColor, secondaryColor)
        }
    }

    fun createIconBitmap(context: Context, primaryColor: Int, secondaryColor: Int): Bitmap {
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

        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        drawable?.let {
            it.setBounds(0, 0, size, size)
            it.draw(canvas)
        }
        return bitmap
    }

    fun updateTaskDescription(activity: Activity, primaryColor: Int, secondaryColor: Int) {
        try {
            val bitmap = createIconBitmap(activity, primaryColor, secondaryColor)
            val appName = activity.getString(R.string.app_name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val icon = android.graphics.drawable.Icon.createWithBitmap(bitmap)
                val taskDescription = ActivityManager.TaskDescription.Builder()
                    .setIcon(icon)
                    .setPrimaryColor(primaryColor)
                    .build()
                activity.setTaskDescription(taskDescription)
            } else {
                @Suppress("DEPRECATION")
                activity.setTaskDescription(ActivityManager.TaskDescription(appName, bitmap, primaryColor))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateShortcutIfPinned(context: Context, primaryColor: Int, secondaryColor: Int) {
        try {
            val bitmap = createIconBitmap(context, primaryColor, secondaryColor)

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
