package com.mohamedaminelouati.oplayer.media

import com.mohamedaminelouati.oplayer.data.PlaybackSpeedScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSpeedResolverTest {

    @Test
    fun isHistorySourceResolvesExpectedOrigins() {
        assertTrue(PlaybackSpeedResolver.isHistorySource("history", PlaybackSource.Folder))
        assertTrue(PlaybackSpeedResolver.isHistorySource(null, PlaybackSource.History))
        assertFalse(PlaybackSpeedResolver.isHistorySource("folder", PlaybackSource.History))
        assertFalse(PlaybackSpeedResolver.isHistorySource("folder_batch", PlaybackSource.History))
        assertFalse(PlaybackSpeedResolver.isHistorySource("playlist_5", PlaybackSource.History))
        assertFalse(PlaybackSpeedResolver.isHistorySource("search", PlaybackSource.Folder))
    }

    @Test
    fun resolvePlaylistIdExtractsIdCorrectly() {
        assertEquals(42, PlaybackSpeedResolver.resolvePlaylistId("playlist_42", PlaybackSource.None))
        assertEquals(9, PlaybackSpeedResolver.resolvePlaylistId("playlist", PlaybackSource.Playlist(9)))
        assertEquals(3, PlaybackSpeedResolver.resolvePlaylistId(null, PlaybackSource.Playlist(3)))
        assertEquals(null, PlaybackSpeedResolver.resolvePlaylistId("folder", PlaybackSource.None))
    }

    @Test
    fun resolveEffectiveSpeedPrioritizesGlobalWhenInGlobalScope() {
        val speed = PlaybackSpeedResolver.resolveEffectiveSpeed(
            scope = PlaybackSpeedScope.GLOBAL,
            globalSpeed = 1.25f,
            isHistory = false,
            historySpeed = 2.0f,
            playlistId = null,
            playlistSpeed = null,
            folderSpeed = 1.75f
        )
        assertEquals(1.25f, speed, 0.001f)
    }

    @Test
    fun resolveEffectiveSpeedUsesFolderSpeedWhenAvailable() {
        val speed = PlaybackSpeedResolver.resolveEffectiveSpeed(
            scope = PlaybackSpeedScope.PER_FOLDER,
            globalSpeed = 1.0f,
            isHistory = false,
            historySpeed = null,
            playlistId = null,
            playlistSpeed = null,
            folderSpeed = 1.75f
        )
        assertEquals(1.75f, speed, 0.001f)
    }

    @Test
    fun resolveEffectiveSpeedFallsBackToGlobalWhenFolderSpeedUnset() {
        val speed = PlaybackSpeedResolver.resolveEffectiveSpeed(
            scope = PlaybackSpeedScope.PER_FOLDER,
            globalSpeed = 1.5f,
            isHistory = false,
            historySpeed = null,
            playlistId = null,
            playlistSpeed = null,
            folderSpeed = null
        )
        assertEquals(1.5f, speed, 0.001f)
    }

    @Test
    fun resolveEffectiveSpeedHandlesHistoryAndPlaylistOverrides() {
        val historySpeed = PlaybackSpeedResolver.resolveEffectiveSpeed(
            scope = PlaybackSpeedScope.PER_FOLDER,
            globalSpeed = 1.0f,
            isHistory = true,
            historySpeed = 2.25f,
            playlistId = null,
            playlistSpeed = null,
            folderSpeed = 1.5f
        )
        assertEquals(2.25f, historySpeed, 0.001f)

        val playlistSpeed = PlaybackSpeedResolver.resolveEffectiveSpeed(
            scope = PlaybackSpeedScope.PER_FOLDER,
            globalSpeed = 1.0f,
            isHistory = false,
            historySpeed = null,
            playlistId = 7,
            playlistSpeed = 0.75f,
            folderSpeed = 1.5f
        )
        assertEquals(0.75f, playlistSpeed, 0.001f)
    }
}
