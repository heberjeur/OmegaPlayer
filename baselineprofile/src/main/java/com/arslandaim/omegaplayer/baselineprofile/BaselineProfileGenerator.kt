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

/**
 * Generates the baseline profile bundled into every build: it records the code paths
 * executed during a cold start, which ART then pre-compiles ahead of time instead of
 * JIT-compiling them on the critical startup path.
 *
 * Run once with a connected device on API 33+ (no root required):
 *
 *     .\gradlew.bat :app:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
    ) {
        // The permission UI would block the profiled journey, so pre-grant the media
        // permissions via shell (no-ops on API < 33, where READ_MEDIA_* are unknown).
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
