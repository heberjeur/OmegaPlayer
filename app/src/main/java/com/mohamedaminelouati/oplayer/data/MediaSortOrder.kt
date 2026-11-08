package com.mohamedaminelouati.oplayer.data

import androidx.annotation.StringRes
import com.mohamedaminelouati.oplayer.R
import com.mohamedaminelouati.oplayer.data.model.FolderNode

enum class MediaSortOrder(val label: String, @StringRes val labelRes: Int) {
    DATE_DESC("Newest First", R.string.sort_date_desc),
    DATE_ASC("Oldest First", R.string.sort_date_asc),
    NAME_ASC("Name (A-Z)", R.string.sort_name_asc),
    NAME_DESC("Name (Z-A)", R.string.sort_name_desc),
    SIZE_ASC("Size (Smallest)", R.string.sort_size_asc),
    SIZE_DESC("Size (Largest)", R.string.sort_size_desc),
    DURATION_ASC("Duration (Shortest)", R.string.sort_duration_asc),
    DURATION_DESC("Duration (Longest)", R.string.sort_duration_desc)
}

fun String?.toMediaSortOrder(): MediaSortOrder {
    return MediaSortOrder.entries.firstOrNull { it.name == this } ?: MediaSortOrder.DATE_DESC
}

@JvmName("sortVideos")
fun List<VideoModel>.sortedWithOrder(order: MediaSortOrder): List<VideoModel> {
    return when (order) {
        MediaSortOrder.DATE_DESC -> sortedByDescending { it.id }
        MediaSortOrder.DATE_ASC -> sortedBy { it.id }
        MediaSortOrder.NAME_ASC -> sortedBy { it.name.lowercase() }
        MediaSortOrder.NAME_DESC -> sortedByDescending { it.name.lowercase() }
        MediaSortOrder.SIZE_ASC -> sortedBy { it.size }
        MediaSortOrder.SIZE_DESC -> sortedByDescending { it.size }
        MediaSortOrder.DURATION_ASC -> sortedBy { it.duration }
        MediaSortOrder.DURATION_DESC -> sortedByDescending { it.duration }
    }
}

@JvmName("sortAudios")
fun List<AudioModel>.sortedWithOrder(order: MediaSortOrder): List<AudioModel> {
    return when (order) {
        MediaSortOrder.DATE_DESC -> sortedByDescending { it.id }
        MediaSortOrder.DATE_ASC -> sortedBy { it.id }
        MediaSortOrder.NAME_ASC -> sortedBy { it.name.lowercase() }
        MediaSortOrder.NAME_DESC -> sortedByDescending { it.name.lowercase() }
        MediaSortOrder.SIZE_ASC -> sortedBy { it.size }
        MediaSortOrder.SIZE_DESC -> sortedByDescending { it.size }
        MediaSortOrder.DURATION_ASC -> sortedBy { it.duration }
        MediaSortOrder.DURATION_DESC -> sortedByDescending { it.duration }
    }
}

@JvmName("sortFolders")
fun List<FolderNode>.sortedWithOrder(order: MediaSortOrder): List<FolderNode> {
    return when (order) {
        MediaSortOrder.NAME_ASC -> sortedBy { it.name.lowercase() }
        MediaSortOrder.NAME_DESC -> sortedByDescending { it.name.lowercase() }
        MediaSortOrder.SIZE_DESC -> sortedByDescending { it.videoCount }
        MediaSortOrder.SIZE_ASC -> sortedBy { it.videoCount }
        MediaSortOrder.DURATION_DESC -> sortedByDescending { it.videoCount }
        MediaSortOrder.DURATION_ASC -> sortedBy { it.videoCount }
        MediaSortOrder.DATE_DESC -> this
        MediaSortOrder.DATE_ASC -> reversed()
    }
}
