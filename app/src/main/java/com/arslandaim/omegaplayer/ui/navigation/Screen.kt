package com.arslandaim.omegaplayer.ui.navigation

import com.arslandaim.omegaplayer.util.MediaUtils

sealed class Screen(val route: String) {
    object Main : Screen("main?tab={tab}") {
        fun createRoute(tab: String?) = if (tab != null) "main?tab=$tab" else "main"
    }
    object Home : Screen("home")
    object Player : Screen("player/{videoUri}?from={from}&pos={pos}") {
        fun createRoute(videoUri: String, from: String? = null, pos: Long = -1L) = 
            "player/${MediaUtils.safeEncodeUri(videoUri)}" + (if (from != null) "?from=$from" else "?from=none") + "&pos=$pos"
    }
    object AudioPlayer : Screen("audio_player/{audioUri}?pos={pos}") {
        fun createRoute(audioUri: String, pos: Long = -1L) = "audio_player/${MediaUtils.safeEncodeUri(audioUri)}?pos=$pos"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
}
