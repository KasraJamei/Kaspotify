package com.example.kaspotify.ui.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
 * Extracts an ambient color from the artwork at [artworkUri] for gradient backdrops, or null if the
 * artwork can't be loaded. Recomputed only when [artworkUri] changes (no per-frame work).
 */
@Composable
fun rememberArtworkAccentColor(artworkUri: Any?): State<Color?> {
    val context = LocalContext.current
    return produceState<Color?>(initialValue = null, key1 = artworkUri) {
        value = if (artworkUri == null) null else extractAccentColor(context, artworkUri)
    }
}

/** Mixes [this] toward black so it works as a soft, deep ambient backdrop rather than a flat fill. */
fun Color.asAmbient(strength: Float = 0.55f): Color = lerp(Background, this, strength)

private suspend fun extractAccentColor(context: Context, artworkUri: Any): Color? =
    withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(artworkUri)
            .allowHardware(false)
            .size(128) // small bitmap — palette doesn't need full resolution
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
