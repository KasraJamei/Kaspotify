package com.example.kaspotify.data.model

import android.content.ContentUris
import android.net.Uri

/** A single playable track imported from the device. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val track: Int,
    val year: Int,
    val dateAddedSec: Long,
    val isFavorite: Boolean = false
) {
    /** Album-art content URI derived from the album id. May not resolve for every album. */
    val artworkUri: Uri
        get() = ContentUris.withAppendedId(ALBUM_ART_BASE, albumId)

    companion object {
        private val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int
) {
    val artworkUri: Uri
        get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)
}

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int
)
