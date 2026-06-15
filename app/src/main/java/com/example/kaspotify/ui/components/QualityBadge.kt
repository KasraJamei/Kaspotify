package com.example.kaspotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassStroke

private val LosslessGold = Color(0xFFE8C468)

/** Small glass pill showing the audio format/bitrate, e.g. "Lossless", "320 kbps". */
@Composable
fun QualityBadge(song: Song, modifier: Modifier = Modifier) {
    val label = song.qualityLabel
    if (label.isEmpty()) return

    // Stay monochrome except for a subtle warm cue on true lossless.
    val content = if (song.isLossless) LosslessGold else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(6.dp)

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
