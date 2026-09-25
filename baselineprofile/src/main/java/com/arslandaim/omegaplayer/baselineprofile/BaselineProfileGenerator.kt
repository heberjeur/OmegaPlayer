/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
    ) {
        device.executeShellCommand("pm grant $PACKAGE_NAME android.permission.READ_MEDIA_VIDEO")
        device.executeShellCommand("pm grant $PACKAGE_NAME android.permission.READ_MEDIA_AUDIO")
        device.executeShellCommand("pm grant $PACKAGE_NAME android.permission.POST_NOTIFICATIONS")
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        const val PACKAGE_NAME = "com.arslandaim.omegaplayer"
    }
}
