package com.mohamedaminelouati.oplayer.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.mohamedaminelouati.oplayer.data.AppIconStyle

object AppIconManager {
    fun setAppIcon(context: Context, style: AppIconStyle) {
        val packageManager = context.packageManager
        val packageName = context.packageName

        AppIconStyle.entries.forEach { iconStyle ->
            val componentName = ComponentName(packageName, iconStyle.aliasName)
            val targetState = if (iconStyle == style) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            try {
                if (packageManager.getComponentEnabledSetting(componentName) != targetState) {
                    packageManager.setComponentEnabledSetting(
                        componentName,
                        targetState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

