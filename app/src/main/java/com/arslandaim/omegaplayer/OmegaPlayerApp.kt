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
        StartupTrace.start(this)
        StartupTrace.installStrictMode()
        super.onCreate()
        StartupTrace.mark("Application.onCreate body done (Hilt component ready)")
        StartupTrace.startWatchdog()
        warmApplicationInfoCache()
        warmImageLoader()
    }

    private fun warmApplicationInfoCache() {
        Thread({
            try {
                applicationInfo
                packageManager.getApplicationInfo(packageName, 0)
                android.provider.Settings.Global.getInt(
                    contentResolver, "animator_duration_scale", 1
                )
            } catch (_: Exception) {
            }
        }, "app-info-warmup").apply { isDaemon = true }.start()
    }

    private fun warmImageLoader() {
        Thread({
            try {
                imageLoader
            } catch (_: Exception) {
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
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }
            .allowHardware(true)
            .crossfade(150)
            .build()
    }
}
