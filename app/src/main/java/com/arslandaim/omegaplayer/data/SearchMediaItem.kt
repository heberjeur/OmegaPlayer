package com.arslandaim.omegaplayer.data

import android.net.Uri

sealed class SearchMediaItem {
    abstract val id: Long
    abstract val uri: Uri
    abstract val name: String
    abstract val duration: Long
    abstract val size: Long
    abstract val path: String
    abstract val isVideo: Boolean
    val searchKey: String get() = if (isVideo) "v_$id" else "a_$id"

    data class Video(val model: VideoModel) : SearchMediaItem() {
        override val id: Long = model.id
        override val uri: Uri = model.uri
        override val name: String = model.name
        override val duration: Long = model.duration
        override val size: Long = model.size
        override val path: String = model.path
        override val isVideo: Boolean = true
    }

    data class Audio(val model: AudioModel) : SearchMediaItem() {
        override val id: Long = model.id
        override val uri: Uri = model.uri
        override val name: String = model.name
        override val duration: Long = model.duration
        override val size: Long = model.size
        override val path: String = model.path
        override val isVideo: Boolean = false
    }
}
