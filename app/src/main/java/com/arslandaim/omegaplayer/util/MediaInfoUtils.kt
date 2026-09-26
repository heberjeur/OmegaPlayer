package com.arslandaim.omegaplayer.util

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetailedMediaInfo(
    val fileName: String,
    val path: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val format: String,
    val videoTrack: VideoTrackInfo?,
    val audioTracks: List<AudioTrackInfo>
)

data class VideoTrackInfo(
    val codec: String,
    val resolution: String,
    val frameRate: Float,
    val bitrate: Int
)

data class AudioTrackInfo(
    val index: Int,
    val codec: String,
    val sampleRate: Int,
    val bitrate: Int,
    val channels: Int,
    val language: String
)

suspend fun extractDetailedMediaInfo(context: Context, uri: Uri, fileName: String, path: String, sizeBytes: Long, durationMs: Long): DetailedMediaInfo = withContext(Dispatchers.IO) {
    var formatName = "Unknown"
    var videoTrack: VideoTrackInfo? = null
    val audioTracks = mutableListOf<AudioTrackInfo>()

    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, uri, null)
        var audioIndex = 1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            
            if (mime.startsWith("video/")) {
                val codec = mime.substringAfter("video/").uppercase()
                val width = if (format.containsKey(MediaFormat.KEY_WIDTH)) format.getInteger(MediaFormat.KEY_WIDTH) else 0
                val height = if (format.containsKey(MediaFormat.KEY_HEIGHT)) format.getInteger(MediaFormat.KEY_HEIGHT) else 0
                val frameRate = if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    try { format.getFloat(MediaFormat.KEY_FRAME_RATE) } 
                    catch (e: Exception) { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
                } else 0f
                val bitrate = if (format.containsKey(MediaFormat.KEY_BIT_RATE)) format.getInteger(MediaFormat.KEY_BIT_RATE) else 0
                
                videoTrack = VideoTrackInfo(
                    codec = codec,
                    resolution = "${width}x${height}",
                    frameRate = frameRate,
                    bitrate = bitrate
                )
            } else if (mime.startsWith("audio/")) {
                val codec = mime.substringAfter("audio/").uppercase()
                val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 0
                val bitrate = if (format.containsKey(MediaFormat.KEY_BIT_RATE)) format.getInteger(MediaFormat.KEY_BIT_RATE) else 0
                val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 0
                val language = if (format.containsKey(MediaFormat.KEY_LANGUAGE)) format.getString(MediaFormat.KEY_LANGUAGE) ?: "Unknown" else "Unknown"
                
                audioTracks.add(
                    AudioTrackInfo(
                        index = audioIndex++,
                        codec = codec,
                        sampleRate = sampleRate,
                        bitrate = bitrate,
                        channels = channels,
                        language = language
                    )
                )
            }
        }
        
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val mimetype = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (mimetype != null) {
                formatName = mimetype
            }
        } catch (e: Exception) {
        } finally {
            retriever.release()
        }
        
    } catch (e: Exception) {
    } finally {
        extractor.release()
    }

    DetailedMediaInfo(
        fileName = fileName,
        path = path,
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        format = formatName,
        videoTrack = videoTrack,
        audioTracks = audioTracks
    )
}
