package com.arslandaim.omegaplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

data class SmartVideoThumb(
    val uri: Uri,
    val durationMs: Long,
    val id: String
)

class SmartVideoThumbFetcher(
    private val model: SmartVideoThumb,
    private val options: Options,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, model.uri)
                val durationMs = model.durationMs
                
                // We try 1s, then 10%, 20%, 30% of the video if it's long enough
                val timestampsToTry = mutableListOf(1000_000L) 
                if (durationMs > 10000) {
                    timestampsToTry.add((durationMs * 0.1 * 1000).toLong())
                    timestampsToTry.add((durationMs * 0.2 * 1000).toLong())
                    timestampsToTry.add((durationMs * 0.3 * 1000).toLong())
                }
                
                var bestBitmap: Bitmap? = null
                for (timeUs in timestampsToTry) {
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        bestBitmap = bitmap
                        if (!isMostlySolidColor(bitmap)) {
                            break // We found a good frame
                        }
                    }
                }
                
                if (bestBitmap != null) {
                    DrawableResult(
                        drawable = BitmapDrawable(context.resources, bestBitmap),
                        isSampled = false,
                        dataSource = DataSource.DISK
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            } finally {
                try { retriever.release() } catch (e: Exception) {}
            }
        }
    }

    private fun isMostlySolidColor(bitmap: Bitmap): Boolean {
        try {
            // Scale down drastically to compute standard deviation of colors
            val small = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            val pixels = IntArray(32 * 32)
            small.getPixels(pixels, 0, 32, 0, 0, 32, 32)
            
            for (color in pixels) {
                rSum += Color.red(color).toLong()
                gSum += Color.green(color).toLong()
                bSum += Color.blue(color).toLong()
            }
            val count = pixels.size
            val rAvg = rSum / count
            val gAvg = gSum / count
            val bAvg = bSum / count
            
            var variance = 0L
            for (color in pixels) {
                val rDiff = Color.red(color) - rAvg
                val gDiff = Color.green(color) - gAvg
                val bDiff = Color.blue(color) - bAvg
                variance += (rDiff * rDiff + gDiff * gDiff + bDiff * bDiff)
            }
            val stdDev = sqrt((variance / count).toDouble())
            
            // A pure solid color has stdDev = 0.
            // Almost solid black/white frames usually have stdDev < 15.
            // Real scenes usually have stdDev > 40.
            return stdDev < 15.0
        } catch (e: Exception) {
            return false // If something fails, assume it's not solid to prevent dropping a valid frame
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<SmartVideoThumb> {
        override fun create(data: SmartVideoThumb, options: Options, imageLoader: ImageLoader): Fetcher {
            return SmartVideoThumbFetcher(data, options, context)
        }
    }
}
