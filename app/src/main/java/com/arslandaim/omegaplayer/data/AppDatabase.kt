package com.arslandaim.omegaplayer.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.*
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
}

@Database(entities = [Playlist::class, PlaylistItem::class, RecentPlayback::class], version = 9)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
