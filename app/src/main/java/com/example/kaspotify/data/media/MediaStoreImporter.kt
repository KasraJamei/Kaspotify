package com.example.kaspotify.data.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.kaspotify.data.model.Album
import com.example.kaspotify.data.model.Artist
import com.example.kaspotify.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reads music already present on the device through MediaStore. No network, no streaming. */
@Singleton
class MediaStoreImporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Scan device audio for actual music tracks (skips ringtones / very short clips). */
    suspend fun scan(): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED
        )
        // Music only, and at least 5 seconds long.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = ArrayList<Song>()
        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    songs += Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown title",
                        artist = cursor.getString(artistCol)?.takeUnless { it == "<unknown>" }
                            ?: "Unknown artist",
                        album = cursor.getString(albumCol) ?: "Unknown album",
                        albumId = cursor.getLong(albumIdCol),
                        durationMs = cursor.getLong(durationCol),
                        uri = uri,
                        track = cursor.getInt(trackCol),
                        year = cursor.getInt(yearCol),
                        dateAddedSec = cursor.getLong(dateCol)
                    )
                }
            }
        songs
    }

    fun deriveAlbums(songs: List<Song>): List<Album> =
        songs.groupBy { it.albumId }
            .map { (albumId, albumSongs) ->
                val first = albumSongs.first()
                Album(
                    id = albumId,
                    title = first.album,
                    artist = albumSongs.map { it.artist }.distinct().singleOrNull()
                        ?: "Various artists",
                    songCount = albumSongs.size
                )
            }
            .sortedBy { it.title.lowercase() }

    fun deriveArtists(songs: List<Song>): List<Artist> =
        songs.groupBy { it.artist }
            .map { (name, artistSongs) ->
                Artist(
                    name = name,
                    songCount = artistSongs.size,
                    albumCount = artistSongs.map { it.albumId }.distinct().size
                )
            }
            .sortedBy { it.name.lowercase() }

    companion object {
        private const val MIN_DURATION_MS = 5_000L
    }
}
