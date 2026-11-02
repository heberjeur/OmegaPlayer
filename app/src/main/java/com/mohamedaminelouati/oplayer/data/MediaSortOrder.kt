package com.mohamedaminelouati.oplayer.data

import androidx.annotation.StringRes
import com.mohamedaminelouati.oplayer.R

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
