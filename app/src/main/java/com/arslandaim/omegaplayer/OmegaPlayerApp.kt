/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.imageLoader
import coil.memory.MemoryCache
import com.arslandaim.omegaplayer.util.SmartVideoThumbFetcher
import com.arslandaim.omegaplayer.util.StartupTrace
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OmegaPlayerApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        // Startup profiling must be anchored before anything else on the main thread.
        StartupTrace.start(this)
        StartupTrace.installStrictMode()
        super.onCreate()
        StartupTrace.mark("Application.onCreate body done (Hilt component ready)")
        StartupTrace.startWatchdog()
        warmApplicationInfoCache()
        warmImageLoader()
    }

    /*
     * Startup report insight: the first View created during the launch (the ComposeView built by
     * MainActivity.setContent) calls PackageManager.getApplicationInfo() -> Binder on the main
     * thread, and PhoneWindow's construction reads Settings.Global the same way (the report
     * sampled the ~657ms launch stall right there). Touching the same per-process caches here,
     * from a background thread, moves those Binder round-trips off the activity-launch path.
     */
    private fun warmApplicationInfoCache() {
        Thread({
            try {
                applicationInfo // LoadedApk.getApplicationInfo() cache
                packageManager.getApplicationInfo(packageName, 0) // ApplicationPackageManager cache
                // One Settings read warms the process-wide Global NameValueCache for every later read.
                android.provider.Settings.Global.getInt(
                    contentResolver, "animator_duration_scale", 1
                )
            } catch (_: Exception) {
                // Best effort only.
            }
        }, "app-info-warmup").apply { isDaemon = true }.start()
    }

    /*
     * A startup report showed the shared Coil ImageLoader (decoder chain + memory/disk caches)
     * being constructed on the main thread at the first artwork request - a 199ms stall right
     * when the player screen opened. Building it here, on a background thread, moves that cost
     * off the UI thread while the app is still warming up.
     */
    private fun warmImageLoader() {
        Thread({
            try {
                imageLoader // Builds the singleton via newImageLoader() below.
            } catch (_: Exception) {
                // Best effort only.
            }
        }, "coil-warmup").apply { isDaemon = true }.start()
    }

    override fun newImageLoader(): ImageLoader {
        StartupTrace.mark("Coil ImageLoader created")
        return ImageLoader.Builder(this)
            .components {
                add(SmartVideoThumbFetcher.Factory(this@OmegaPlayerApp))
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB dedicated disk cache
                    .build()
            }
            .allowHardware(true)
            .crossfade(150)
            .build()
    }
}
