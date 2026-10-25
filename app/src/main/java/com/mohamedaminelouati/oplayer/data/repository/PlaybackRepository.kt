package com.mohamedaminelouati.oplayer.data.repository

import com.mohamedaminelouati.oplayer.data.Playlist
import com.mohamedaminelouati.oplayer.data.PlaylistItem
import com.mohamedaminelouati.oplayer.data.RecentPlayback
import kotlinx.coroutines.flow.Flow

interface PlaybackRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun createPlaylist(name: String)
    suspend fun deletePlaylist(playlist: Playlist)
    fun getPlaylistItems(playlistId: Int): Flow<List<PlaylistItem>>
    suspend fun addToPlaylist(playlistId: Int, uri: String, type: String)
    suspend fun removeFromPlaylist(playlistId: Int, uri: String)
    
    fun getRecentPlayback(): Flow<List<RecentPlayback>>
    fun getAllRecentPlayback(): Flow<List<RecentPlayback>>
    suspend fun getRecentPlayback(uri: String): RecentPlayback?
    suspend fun saveRecentPlayback(recent: RecentPlayback)
    suspend fun deleteRecentPlayback(uri: String)
    suspend fun clearAllRecentPlayback()
    suspend fun resetAllPlaybackPositions()
}
