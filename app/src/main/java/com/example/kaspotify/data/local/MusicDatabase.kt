package com.example.kaspotify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongStateEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
}
