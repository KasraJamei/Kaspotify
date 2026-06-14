package com.example.kaspotify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.QualityTier
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.SongRow

/** Browses the library separated into audio-quality tiers (Lossless, 320 kbps, …). */
@Composable
fun QualityScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val byQuality by viewModel.songsByQuality.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentId = currentSong?.id

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            Text("By Quality", style = MaterialTheme.typography.titleLarge)
        }

        if (byQuality.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            byQuality.forEach { (tier, tierSongs) ->
                if (tierSongs.isEmpty()) return@forEach
                item(key = "header_${tier.name}") {
                    QualityHeader(tier, tierSongs.size)
                }
                items(tierSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentId,
                        onClick = { viewModel.playSong(song, tierSongs) },
                        onToggleFavorite = { viewModel.toggleFavorite(song) },
                        onMore = { onMore(song) },
                        onPlayNext = { viewModel.playNext(song) },
                        onAddToQueue = { viewModel.addToQueue(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityHeader(tier: QualityTier, count: Int) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                tier.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "$count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
