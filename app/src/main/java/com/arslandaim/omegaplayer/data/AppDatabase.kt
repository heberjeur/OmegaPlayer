package com.arslandaim.omegaplayer.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Immutable
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val mediaUri: String,
    val mediaType: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "recent_playback", indices = [Index("lastPlayed")])
data class RecentPlayback(
    @PrimaryKey val uri: String,
    val position: Long,
    val duration: Long,
    val lastPlayed: Long = System.currentTimeMillis(),
    val mediaType: String,
    val name: String,
    val artist: String? = null,
    val size: Long = 0L
)

@Dao
interface AppDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylistsFlow(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getPlaylistItemsFlow(playlistId: Int): Flow<List<PlaylistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItem)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaUri = :mediaUri")
    suspend fun removePlaylistItem(playlistId: Int, mediaUri: String)

    @Query("SELECT * FROM recent_playback ORDER BY lastPlayed DESC LIMIT 20")
    fun getRecentPlaybackFlow(): Flow<List<RecentPlayback>>

    @Query("SELECT * FROM recent_playback ORDER BY lastPlayed DESC")
    fun getAllRecentPlaybackFlow(): Flow<List<RecentPlayback>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPlayback(recent: RecentPlayback)

    @Query("SELECT * FROM recent_playback WHERE uri = :uri")
    suspend fun getRecentPlayback(uri: String): RecentPlayback?

    @Query("DELETE FROM recent_playback WHERE uri = :uri")
    suspend fun deleteRecentPlayback(uri: String)

    @Query("DELETE FROM recent_playback")
    suspend fun clearAllRecentPlayback()

    // Videos
    @Query("SELECT * FROM videos")
    fun getAllVideos(): Flow<List<VideoModel>>

    // One-shot read used by the delta sync to compute what actually changed.
    @Query("SELECT * FROM videos")
    suspend fun getVideosOnce(): List<VideoModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoModel>)

    @Update
    suspend fun updateVideos(videos: List<VideoModel>)

    @Delete
    suspend fun deleteVideos(videos: List<VideoModel>)

    // Targeted removal after a successful MediaStore delete. Updating the cached row directly
    // lets the UI reflect the deletion immediately instead of waiting for a full MediaStore
    // re-sync, which takes seconds on a large library.
    @Query("DELETE FROM videos WHERE uri IN (:uris)")
    suspend fun deleteVideosByUri(uris: List<String>)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    // Audios
    @Query("SELECT * FROM audios")
    fun getAllAudios(): Flow<List<AudioModel>>

    // One-shot read used by the delta sync to compute what actually changed.
    @Query("SELECT * FROM audios")
    suspend fun getAudiosOnce(): List<AudioModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudios(audios: List<AudioModel>)

    @Update
    suspend fun updateAudios(audios: List<AudioModel>)

    @Delete
    suspend fun deleteAudios(audios: List<AudioModel>)

    // Targeted removal after a successful MediaStore delete (see deleteVideosByUri).
    @Query("DELETE FROM audios WHERE uri IN (:uris)")
    suspend fun deleteAudiosByUri(uris: List<String>)

    @Query("DELETE FROM audios")
    suspend fun deleteAllAudios()

    @Query("SELECT treeJson FROM cached_trees WHERE id = :id")
    fun getCachedTree(id: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedTree(tree: CachedTree)

    @Query("DELETE FROM cached_trees WHERE id = :id")
    suspend fun deleteCachedTree(id: String)
}

@Immutable
@Entity(tableName = "cached_trees")
data class CachedTree(
    @PrimaryKey val id: String,
    val treeJson: String
)

@TypeConverters(Converters::class)
@Database(entities = [Playlist::class, PlaylistItem::class, RecentPlayback::class, VideoModel::class, AudioModel::class, CachedTree::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `videos` (`id` INTEGER NOT NULL, `uri` TEXT NOT NULL, `name` TEXT NOT NULL, `duration` INTEGER NOT NULL, `size` INTEGER NOT NULL, `path` TEXT NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `audios` (`id` INTEGER NOT NULL, `albumId` INTEGER NOT NULL, `uri` TEXT NOT NULL, `name` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `size` INTEGER NOT NULL, `path` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `cached_trees` (`id` TEXT NOT NULL, `treeJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
