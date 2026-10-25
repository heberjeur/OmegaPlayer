package com.mohamedaminelouati.oplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.imageLoader
import coil.memory.MemoryCache
import com.mohamedaminelouati.oplayer.util.SmartVideoThumbFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OmegaPlayerApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        warmApplicationInfoCache()
        warmImageLoader()
    }

    private fun warmApplicationInfoCache() {
        Thread({
            applicationInfo
            packageManager.getApplicationInfo(packageName, 0)
            android.provider.Settings.Global.getInt(
                contentResolver, "animator_duration_scale", 1
            )
        }, "app-info-warmup").apply { isDaemon = true }.start()
    }

    private fun warmImageLoader() {
        Thread({
            imageLoader
        }, "coil-warmup").apply { isDaemon = true }.start()
    }

    override fun newImageLoader(): ImageLoader {
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
