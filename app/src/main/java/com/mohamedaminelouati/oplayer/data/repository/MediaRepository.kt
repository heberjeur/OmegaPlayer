package com.mohamedaminelouati.oplayer.data.repository

import com.mohamedaminelouati.oplayer.data.AudioModel
import com.mohamedaminelouati.oplayer.data.VideoModel
import com.mohamedaminelouati.oplayer.util.Resource
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
