package com.arslandaim.omegaplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.room.withTransaction
import com.arslandaim.omegaplayer.data.AppDatabase
import com.arslandaim.omegaplayer.data.AudioModel
import com.arslandaim.omegaplayer.data.VideoModel
import com.arslandaim.omegaplayer.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(FlowPreview::class)
@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: com.arslandaim.omegaplayer.data.AppDao,
    private val database: AppDatabase
) : MediaRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mediaChangeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val mediaContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            mediaChangeEvents.tryEmit(Unit)
        }
    }

    init {
        repositoryScope.launch {
            mediaChangeEvents
                .debounce(1500L)
                .collectLatest {
                    syncMediaWithSystem()
                }
        }

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaContentObserver
            )
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaContentObserver
            )
        } catch (e: SecurityException) {
            Log.w("MediaRepository", "Failed to register content observer", e)
        }
    }

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
                    val audioChanged = if (audioResource is Resource.Success) {
                        applyAudiosDelta(audioResource.data)
                    } else false
                    val videoChanged = if (videoResource is Resource.Success) {
                        applyVideosDelta(videoResource.data)
                    } else false
                    if (audioChanged) appDao.deleteCachedTree("audio")
                    if (videoChanged) appDao.deleteCachedTree("video")
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

    private suspend fun applyAudiosDelta(newList: List<AudioModel>): Boolean {
        val currentById = appDao.getAudiosOnce().associateBy { it.id }
        val newById = newList.associateBy { it.id }
        if (currentById == newById) return false
        val removed = currentById.values.filter { newById[it.id] == null }
        if (removed.isNotEmpty()) appDao.deleteAudios(removed)
        val added = newById.values.filter { currentById[it.id] == null }
        if (added.isNotEmpty()) appDao.insertAudios(added)
        val updated = newById.values.filter { model ->
            val old = currentById[model.id]
            old != null && old != model
        }
        if (updated.isNotEmpty()) appDao.updateAudios(updated)
        return true
    }

    private suspend fun applyVideosDelta(newList: List<VideoModel>): Boolean {
        val currentById = appDao.getVideosOnce().associateBy { it.id }
        val newById = newList.associateBy { it.id }
        if (currentById == newById) return false
        val removed = currentById.values.filter { newById[it.id] == null }
        if (removed.isNotEmpty()) appDao.deleteVideos(removed)
        val added = newById.values.filter { currentById[it.id] == null }
        if (added.isNotEmpty()) appDao.insertVideos(added)
        val updated = newById.values.filter { model ->
            val old = currentById[model.id]
            old != null && old != model
        }
        if (updated.isNotEmpty()) appDao.updateVideos(updated)
        return true
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
