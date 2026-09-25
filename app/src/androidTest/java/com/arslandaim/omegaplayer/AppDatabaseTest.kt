package com.arslandaim.omegaplayer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arslandaim.omegaplayer.data.AppDao
import com.arslandaim.omegaplayer.data.AppDatabase
import com.arslandaim.omegaplayer.data.Playlist
import com.arslandaim.omegaplayer.data.PlaylistItem
import com.arslandaim.omegaplayer.data.RecentPlayback
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AppDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.appDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun playlistItemsStayScopedToTheirPlaylist() = runBlocking {
        val firstId = dao.insertPlaylist(Playlist(name = "First")).toInt()
        val secondId = dao.insertPlaylist(Playlist(name = "Second")).toInt()
        dao.insertPlaylistItem(PlaylistItem(playlistId = firstId, mediaUri = "content://media/audio/1", mediaType = "audio"))
        dao.insertPlaylistItem(PlaylistItem(playlistId = secondId, mediaUri = "content://media/audio/2", mediaType = "audio"))
        val firstItems = dao.getPlaylistItemsFlow(firstId).first()
        assertEquals(1, firstItems.size)
        assertEquals("content://media/audio/1", firstItems.first().mediaUri)
    }

    @Test
    fun removePlaylistItemDeletesOnlyMatchingUri() = runBlocking {
        val playlistId = dao.insertPlaylist(Playlist(name = "Mix")).toInt()
        dao.insertPlaylistItem(PlaylistItem(playlistId = playlistId, mediaUri = "a", mediaType = "audio"))
        dao.insertPlaylistItem(PlaylistItem(playlistId = playlistId, mediaUri = "b", mediaType = "audio"))
        dao.removePlaylistItem(playlistId, "a")
        val items = dao.getPlaylistItemsFlow(playlistId).first()
        assertEquals(1, items.size)
        assertEquals("b", items.first().mediaUri)
    }

    @Test
    fun recentPlaybackRoundTripKeepsPosition() = runBlocking {
        val uri = "content://media/external/video/media/7"
        dao.insertRecentPlayback(RecentPlayback(uri = uri, position = 42_000L, duration = 90_000L, mediaType = "video", name = "clip.mp4"))
        val loaded = dao.getRecentPlayback(uri)
        assertNotNull(loaded)
        assertEquals(42_000L, loaded!!.position)
    }

    @Test
    fun recentPlaybackFlowIsLimitedToTwentyEntries() = runBlocking {
        repeat(25) { index ->
            dao.insertRecentPlayback(
                RecentPlayback(
                    uri = "content://media/video/$index",
                    position = 0L,
                    duration = 1_000L,
                    lastPlayed = index.toLong(),
                    mediaType = "video",
                    name = "item$index.mp4"
                )
            )
        }
        val recent = dao.getRecentPlaybackFlow().first()
        assertEquals(20, recent.size)
        assertEquals("content://media/video/24", recent.first().uri)
    }

    @Test
    fun clearAllRecentPlaybackEmptiesTable() = runBlocking {
        dao.insertRecentPlayback(RecentPlayback(uri = "content://media/video/1", position = 0L, duration = 1_000L, mediaType = "video", name = "a.mp4"))
        dao.clearAllRecentPlayback()
        assertEquals(0, dao.getAllRecentPlaybackFlow().first().size)
    }
}
