package com.arslandaim.omegaplayer.media

import androidx.annotation.DrawableRes
import com.arslandaim.omegaplayer.R

enum class PlayerRepeatMode(
    @DrawableRes val iconRes: Int,
    val targetRepeats: Int
) {
    OFF(R.drawable.ic_repeat, 0),
    ONE(R.drawable.ic_repeat_one, 1),
    TWO(R.drawable.ic_repeat_two, 2),
    THREE(R.drawable.ic_repeat_three, 3),
    ALL(R.drawable.ic_repeat, -1);

    val isActive: Boolean
        get() = this != OFF

    fun next(): PlayerRepeatMode = when (this) {
        OFF -> ONE
        ONE -> TWO
        TWO -> THREE
        THREE -> ALL
        ALL -> OFF
    }
}
