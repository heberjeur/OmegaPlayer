package com.arslandaim.omegaplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse
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
            val targetWidth = options.size.width.pxOrElse { MAX_DECODE_WIDTH }
                .coerceIn(64, MAX_DECODE_WIDTH)

            val fastPathStarted = SystemClock.uptimeMillis()
            systemThumbnail(context, model.uri, targetWidth)?.let { bitmap ->
                StartupTrace.count("thumbnails.fastPath (MediaStore)", SystemClock.uptimeMillis() - fastPathStarted)
                return@withContext DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            }

            val slowPathStarted = SystemClock.uptimeMillis()
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, model.uri)
                val durationMs = model.durationMs

                val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                
                val timestampsToTry = mutableListOf(1000_000L) 
                if (durationMs > 10000) {
                    timestampsToTry.add((durationMs * 0.1 * 1000).toLong())
                    timestampsToTry.add((durationMs * 0.2 * 1000).toLong())
                    timestampsToTry.add((durationMs * 0.3 * 1000).toLong())
                }
                
                var bestBitmap: Bitmap? = null
                for (timeUs in timestampsToTry) {
                    val bitmap = extractFrame(retriever, timeUs, targetWidth, videoWidth, videoHeight) ?: continue
                    if (bestBitmap != null && bestBitmap !== bitmap) bestBitmap.recycle()
                    bestBitmap = bitmap
                    if (!isMostlySolidColor(bitmap)) {
                        break
                    }
                }
                
                if (bestBitmap != null) {
                    DrawableResult(
                        drawable = BitmapDrawable(context.resources, bestBitmap),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            } finally {
                StartupTrace.count("thumbnails.frameExtraction (MediaMetadataRetriever)", SystemClock.uptimeMillis() - slowPathStarted)
                try { retriever.release() } catch (e: Exception) {}
            }
        }
    }

    private fun systemThumbnail(context: Context, uri: Uri, targetWidth: Int): Bitmap? {
        val original = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, android.util.Size(targetWidth, targetWidth), null)
            } else {
                mediaStoreThumbnailLegacy(context, uri)
            }
        } catch (e: Throwable) {
            null
        } ?: return null

        val maxSide = maxOf(original.width, original.height)
        if (maxSide <= targetWidth) return original
        return try {
            val scale = targetWidth.toFloat() / maxSide
            val scaled = Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled !== original) original.recycle()
            scaled
        } catch (e: Exception) {
            original
        }
    }

    @Suppress("DEPRECATION")
    private fun mediaStoreThumbnailLegacy(context: Context, uri: Uri): Bitmap? {
        if (uri.scheme != "content" || uri.authority != "media") return null
        val id = uri.lastPathSegment?.toLongOrNull() ?: return null
        return MediaStore.Video.Thumbnails.getThumbnail(
            context.contentResolver,
            id,
            MediaStore.Video.Thumbnails.MINI_KIND,
            null
        )
    }

    private fun extractFrame(
        retriever: MediaMetadataRetriever,
        timeUs: Long,
        targetWidth: Int,
        videoWidth: Int,
        videoHeight: Int
    ): Bitmap? {
        val targetHeight = if (videoWidth > 0 && videoHeight > 0) {
            (targetWidth.toLong() * videoHeight / videoWidth).toInt().coerceAtLeast(1)
        } else {
            targetWidth
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                targetWidth,
                targetHeight
            )?.let { return it }
        }
        val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
        return try {
            val scaled = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
            if (scaled !== frame) frame.recycle()
            scaled
        } catch (e: Exception) {
            frame
        }
    }

    private fun isMostlySolidColor(bitmap: Bitmap): Boolean {
        try {
            val small = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            val pixels = IntArray(32 * 32)
            small.getPixels(pixels, 0, 32, 0, 0, 32, 32)
            if (small !== bitmap) small.recycle()
            
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
            
            return stdDev < 15.0
        } catch (e: Exception) {
            return false
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<SmartVideoThumb> {
        override fun create(data: SmartVideoThumb, options: Options, imageLoader: ImageLoader): Fetcher {
            return SmartVideoThumbFetcher(data, options, context)
        }
    }

    companion object {
        private const val MAX_DECODE_WIDTH = 512
    }
}
