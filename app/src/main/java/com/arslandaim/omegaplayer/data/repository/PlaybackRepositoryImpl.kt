package com.arslandaim.omegaplayer.data.repository

import com.arslandaim.omegaplayer.data.AppDao
import com.arslandaim.omegaplayer.data.Playlist
import com.arslandaim.omegaplayer.data.PlaylistItem
import com.arslandaim.omegaplayer.data.RecentPlayback
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepositoryImpl @Inject constructor(
    private val appDao: AppDao
) : PlaybackRepository {

    override fun getPlaylists(): Flow<List<Playlist>> = appDao.getAllPlaylistsFlow()

    override suspend fun createPlaylist(name: String) {
        appDao.insertPlaylist(Playlist(name = name))
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        appDao.deletePlaylist(playlist)
    }

    override fun getPlaylistItems(playlistId: Int): Flow<List<PlaylistItem>> {
        return appDao.getPlaylistItemsFlow(playlistId)
    }

    override suspend fun addToPlaylist(playlistId: Int, uri: String, type: String) {
        appDao.insertPlaylistItem(PlaylistItem(playlistId = playlistId, mediaUri = uri, mediaType = type))
    }

    override suspend fun removeFromPlaylist(playlistId: Int, uri: String) {
        appDao.removePlaylistItem(playlistId, uri)
    }

    override fun getRecentPlayback(): Flow<List<RecentPlayback>> = appDao.getRecentPlaybackFlow()

    override fun getAllRecentPlayback(): Flow<List<RecentPlayback>> = appDao.getAllRecentPlaybackFlow()

    override suspend fun getRecentPlayback(uri: String): RecentPlayback? = appDao.getRecentPlayback(uri)

    override suspend fun saveRecentPlayback(recent: RecentPlayback) {
        appDao.insertRecentPlayback(recent)
    }

    override suspend fun deleteRecentPlayback(uri: String) {
        appDao.deleteRecentPlayback(uri)
    }

    override suspend fun clearAllRecentPlayback() {
        appDao.clearAllRecentPlayback()
    }
}
