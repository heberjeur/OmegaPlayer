package com.arslandaim.omegaplayer.ui.common

import android.content.Context
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision

internal const val VIDEO_THUMB_FRAME_MS = 1000L

private const val VIDEO_THUMB_SIZE = 400

internal fun videoThumbImageRequest(context: Context, data: Any, cacheKey: String? = null): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(data)
        .videoFrameMillis(VIDEO_THUMB_FRAME_MS)
        .size(VIDEO_THUMB_SIZE)
        .precision(Precision.INEXACT)
    if (cacheKey != null) {
        builder.diskCacheKey(cacheKey)
        builder.memoryCacheKey(cacheKey)
    }
    return builder.build()
}
