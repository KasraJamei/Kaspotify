package com.example.kaspotify.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Per-song persisted state. songId matches MediaStore audio _ID. */
@Entity(tableName = "song_state")
data class SongStateEntity(
    @PrimaryKey val songId: Long,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

/** Projection used by playlistsWithCounts(). */
data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val songCount: Int
)
