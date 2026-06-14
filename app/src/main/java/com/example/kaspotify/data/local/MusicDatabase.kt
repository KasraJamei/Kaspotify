package com.example.kaspotify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SongStateEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        /** v1 -> v2: add the search_history table without touching favorites/playlists. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `search_history` " +
                        "(`query` TEXT NOT NULL, `lastUsedAt` INTEGER NOT NULL, PRIMARY KEY(`query`))"
                )
            }
        }
    }
}
