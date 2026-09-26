package com.arslandaim.omegaplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.room.withTransaction
import com.arslandaim.omegaplayer.data.AppDatabase
import com.arslandaim.omegaplayer.data.AudioModel
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: com.arslandaim.omegaplayer.data.AppDao,
    private val database: AppDatabase
) : MediaRepository {

    override fun getAudios(): Flow<Resource<List<AudioModel>>> {
        return appDao.getAllAudios().map { Resource.Success(it) }
    }

    override fun getVideos(): Flow<Resource<List<VideoModel>>> {
        return appDao.getAllVideos().map { Resource.Success(it) }
    }

    override fun getCachedTree(id: String): Flow<String?> {
        return appDao.getCachedTree(id)
    }

    override suspend fun saveCachedTree(id: String, treeJson: String) {
        appDao.insertCachedTree(com.arslandaim.omegaplayer.data.CachedTree(id, treeJson))
    }

    override suspend fun deleteCachedTree(id: String) {
        appDao.deleteCachedTree(id)
    }

    private fun hasPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private val syncMutex = Mutex()

    override suspend fun syncMediaWithSystem() {
        if (!hasPermission()) return

        syncMutex.withLock {
            withContext(Dispatchers.IO) {
                val audioResource = fetchAudios()
                val videoResource = fetchVideos()

                database.withTransaction {
                    if (audioResource is Resource.Success) {
                        audioResource.data?.let { applyAudiosDelta(it) }
                    }
                    if (videoResource is Resource.Success) {
                        videoResource.data?.let { applyVideosDelta(it) }
                    }
                    appDao.deleteCachedTree("audio")
                    appDao.deleteCachedTree("video")
                }
            }
        }
    }

    override suspend fun removeAudiosFromCache(uris: List<String>) {
        if (uris.isEmpty()) return
        withContext(Dispatchers.IO) {
            appDao.deleteAudiosByUri(uris)
        }
    }

    override suspend fun removeVideosFromCache(uris: List<String>) {
        if (uris.isEmpty()) return
        withContext(Dispatchers.IO) {
            appDao.deleteVideosByUri(uris)
        }
    }

    private suspend fun applyAudiosDelta(newList: List<AudioModel>) {
        val currentById = appDao.getAudiosOnce().associateBy { it.id }
        val newById = newList.associateBy { it.id }
        if (currentById == newById) return
        val removed = currentById.values.filter { newById[it.id] == null }
        if (removed.isNotEmpty()) appDao.deleteAudios(removed)
        val added = newById.values.filter { currentById[it.id] == null }
        if (added.isNotEmpty()) appDao.insertAudios(added)
        val updated = newById.values.filter { model ->
            val old = currentById[model.id]
            old != null && old != model
        }
        if (updated.isNotEmpty()) appDao.updateAudios(updated)
    }

    private suspend fun applyVideosDelta(newList: List<VideoModel>) {
        val currentById = appDao.getVideosOnce().associateBy { it.id }
        val newById = newList.associateBy { it.id }
        if (currentById == newById) return
        val removed = currentById.values.filter { newById[it.id] == null }
        if (removed.isNotEmpty()) appDao.deleteVideos(removed)
        val added = newById.values.filter { currentById[it.id] == null }
        if (added.isNotEmpty()) appDao.insertVideos(added)
        val updated = newById.values.filter { model ->
            val old = currentById[model.id]
            old != null && old != model
        }
        if (updated.isNotEmpty()) appDao.updateVideos(updated)
    }

    private fun fetchAudios(): Resource<List<AudioModel>> {
        val list = mutableListOf<AudioModel>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA
        )

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val albumId = it.getLong(albumIdColumn)
                val name = it.getString(nameColumn) ?: "Unknown"
                val artist = it.getString(artistColumn) ?: "Unknown Artist"
                val album = it.getString(albumColumn) ?: "Unknown Album"
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                val path = it.getString(dataColumn) ?: ""
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                list.add(AudioModel(id, albumId, contentUri, name, artist, album, duration, size, path))
            }
        }
        return Resource.Success(list)
    }

    private fun fetchVideos(): Resource<List<VideoModel>> {
        val list = mutableListOf<VideoModel>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA
        )

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "Unknown"
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                val path = it.getString(dataColumn) ?: ""
                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                list.add(VideoModel(id, contentUri, name, duration, size, path))
            }
        }
        return Resource.Success(list)
    }
}
