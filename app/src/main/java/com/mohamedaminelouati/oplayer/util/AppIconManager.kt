package com.mohamedaminelouati.oplayer.util

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.mohamedaminelouati.oplayer.R
import com.mohamedaminelouati.oplayer.data.AppIconStyle

object AppIconManager {

    fun setAppIcon(context: Context, style: AppIconStyle) {
        val packageManager = context.packageManager
        val packageName = context.packageName

        try {
            val targetComponent = ComponentName(packageName, style.aliasName)
            val currentState = packageManager.getComponentEnabledSetting(targetComponent)
            if (currentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return
            }

            AppIconStyle.entries.forEach { iconStyle ->
                if (iconStyle != style) {
                    val componentName = ComponentName(packageName, iconStyle.aliasName)
                    val state = packageManager.getComponentEnabledSetting(componentName)
                    if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                        (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && iconStyle == AppIconStyle.DEFAULT)
                    ) {
                        packageManager.setComponentEnabledSetting(
                            componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }

            packageManager.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            cleanupLegacyShortcuts(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyIconForColors(context: Context, primaryColor: Int, secondaryColor: Int) {
        val style = AppIconStyle.findClosestStyle(primaryColor, secondaryColor)
        setAppIcon(context, style)
        (context as? Activity)?.let {
            updateTaskDescription(it, primaryColor, secondaryColor)
        }
    }

    private fun cleanupLegacyShortcuts(context: Context) {
        try {
            ShortcutManagerCompat.disableShortcuts(context, listOf("custom_omega_player_shortcut"), null)
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf("custom_omega_player_shortcut"))
        } catch (e: Exception) {
            e.printStackTrace()
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
}
