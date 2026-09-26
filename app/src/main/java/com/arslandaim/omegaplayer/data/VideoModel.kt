package com.arslandaim.omegaplayer.data

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "videos")
data class VideoModel(
    @PrimaryKey val id: Long,
    val uri: Uri,
    val name: String,
    val duration: Long,
    val size: Long,
    val path: String
)
