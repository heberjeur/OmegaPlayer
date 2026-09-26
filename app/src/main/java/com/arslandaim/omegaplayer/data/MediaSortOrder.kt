package com.arslandaim.omegaplayer.data

enum class MediaSortOrder(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_ASC("Size (Smallest)"),
    SIZE_DESC("Size (Largest)"),
    DURATION_ASC("Duration (Shortest)"),
    DURATION_DESC("Duration (Longest)")
}
