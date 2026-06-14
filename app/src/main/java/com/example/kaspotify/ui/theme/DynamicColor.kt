package com.example.kaspotify.ui.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a vibrant accent color from the artwork at [artworkUri], or null if the artwork can't
 * be loaded or has no usable swatch. Recomputed whenever [artworkUri] changes.
 */
@Composable
fun rememberArtworkAccentColor(artworkUri: Any?): State<Color?> {
    val context = LocalContext.current
    return produceState<Color?>(initialValue = null, key1 = artworkUri) {
        value = if (artworkUri == null) null else extractAccentColor(context, artworkUri)
    }
}

/** A curated palette the idle accent drifts through when nothing is playing. */
private val IdlePalette = listOf(
    Color(0xFF1DB954), // Spotify green
    Color(0xFF1ED7C4), // teal
    Color(0xFF5B6CFF), // indigo
    Color(0xFFB05BFF), // violet
    Color(0xFFFF6F61), // coral
    Color(0xFFFFB02E), // amber
)

/**
 * A slowly drifting accent color used as the app's "resting" theme when no song is playing, so the
 * tinted sections gently shift instead of sitting on a static green.
 */
@Composable
fun rememberIdleAccentColor(): Color {
    val transition = rememberInfiniteTransition(label = "idleAccent")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = IdlePalette.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = IdlePalette.size * 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idleAccentT"
    )
    val index = t.toInt() % IdlePalette.size
    val next = (index + 1) % IdlePalette.size
    return lerp(IdlePalette[index], IdlePalette[next], t - t.toInt())
}

private suspend fun extractAccentColor(context: Context, artworkUri: Any): Color? =
    withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(artworkUri)
            .allowHardware(false)
            .build()
        val result = ImageLoader(context).execute(request)
        val bitmap: Bitmap? = when (result) {
            is SuccessResult -> result.drawable.toBitmapOrNull()
            is ErrorResult -> null
            else -> null
        }
        bitmap?.let { bmp ->
            val palette = Palette.from(bmp).generate()
            val swatch = palette.vibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch
            swatch?.let { Color(it.rgb) }
        }
    }
