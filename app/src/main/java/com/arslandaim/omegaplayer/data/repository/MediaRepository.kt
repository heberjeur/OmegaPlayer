package com.arslandaim.omegaplayer.data.repository

import com.arslandaim.omegaplayer.data.AudioModel
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.util.Resource
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAudios(): Flow<Resource<List<AudioModel>>>
    fun getVideos(): Flow<Resource<List<VideoModel>>>
    
    fun getCachedTree(id: String): Flow<String?>
    suspend fun saveCachedTree(id: String, treeJson: String)
    suspend fun deleteCachedTree(id: String)
    
    suspend fun syncMediaWithSystem()

    suspend fun removeAudiosFromCache(uris: List<String>)

    suspend fun removeVideosFromCache(uris: List<String>)
}
