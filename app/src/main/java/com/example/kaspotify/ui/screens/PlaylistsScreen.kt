package com.example.kaspotify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.QualityTier
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.MusicViewModel

@Composable
fun PlaylistsScreen(
    viewModel: MusicViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSmartPlaylist: (SmartPlaylistType) -> Unit,
    onOpenQuality: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showNewDialog by remember { mutableStateOf(false) }
    var showBuilder by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Playlists", style = MaterialTheme.typography.headlineMedium)
            Row {
                IconButton(onClick = { showBuilder = true }) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Smart playlist")
                }
                IconButton(onClick = { showNewDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "New playlist")
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item(key = "made_for_you") {
                Text(
                    "Made for you",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
                )
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(SmartPlaylistType.entries.toList(), key = { it.name }) { type ->
                        MadeForYouCard(type.title, type.icon) { onOpenSmartPlaylist(type) }
                    }
                    item {
                        MadeForYouCard("By Quality", Icons.Filled.GraphicEq, onClick = onOpenQuality)
                    }
                }
                Text(
                    "Your playlists",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
                )
            }

            if (playlists.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "No playlists yet. Tap + to create one, or ✦ to build one from your library.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlaylist(playlist.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${playlist.songCount} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        NewPlaylistDialog(
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showNewDialog = false
            },
            onDismiss = { showNewDialog = false }
        )
    }
    if (showBuilder) {
        SmartPlaylistBuilderDialog(
            viewModel = viewModel,
            onDismiss = { showBuilder = false }
        )
    }
}

@Composable
private fun MadeForYouCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .width(132.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun NewPlaylistDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private enum class BuilderSource(val label: String) {
    ALL("All songs"),
    FAVORITES("Favorites"),
    MOST_PLAYED("Most played"),
    RECENTLY_ADDED("Recently added")
}

/** Lets the user assemble a playlist from their library by source + quality, then saves it. */
@Composable
private fun SmartPlaylistBuilderDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    val allSongs by viewModel.songs.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(BuilderSource.ALL) }
    var tier by remember { mutableStateOf<QualityTier?>(null) }

    fun resolve(): List<Song> {
        val base = when (source) {
            BuilderSource.ALL -> allSongs
            BuilderSource.FAVORITES -> favorites
            BuilderSource.MOST_PLAYED -> mostPlayed
            BuilderSource.RECENTLY_ADDED -> recentlyAdded
        }
        val filtered = tier?.let { t -> base.filter { it.qualityTier == t } } ?: base
        return filtered.take(MAX_BUILDER_SONGS)
    }

    val resolvedCount = resolve().size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Build a playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(12.dp))
                Text("From", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BuilderSource.entries.toList(), key = { it.name }) { s ->
                        FilterChip(
                            selected = source == s,
                            onClick = { source = s },
                            label = { Text(s.label) }
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text("Quality", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = tier == null,
                            onClick = { tier = null },
                            label = { Text("Any") }
                        )
                    }
                    items(QualityTier.entries.toList(), key = { it.name }) { t ->
                        FilterChip(
                            selected = tier == t,
                            onClick = { tier = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    "$resolvedCount songs match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.createSmartPlaylist(
                        name.ifBlank { source.label },
                        resolve()
                    )
                    onDismiss()
                },
                enabled = resolvedCount > 0
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private const val MAX_BUILDER_SONGS = 100
