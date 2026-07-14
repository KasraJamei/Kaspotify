package com.example.kaspotify.ui.theme

import androidx.compose.foundation.border
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neomorphism ("soft UI") theming.
 *
 * The look is a single mid-tone base color plus twin shadows — a light highlight cast from the
 * top-left and a dark shadow to the bottom-right — so an element the *same color* as its background
 * appears gently extruded from, or pressed into, the page.
 *
 * Two things make this read as "designed" rather than muddy, and both matter in dark mode where the
 * effect lives or dies on tonal contrast:
 *   1. A wide gap between [Neo.Light] and [Neo.Dark] (near-black shadow, clearly-lighter highlight).
 *   2. A crisp diagonal **bevel** edge ([neoBevel]) — a hairline that is bright at the top-left and
 *      dark at the bottom-right. It renders on every device regardless of whether soft blur is
 *      supported, so surfaces never collapse to a flat rectangle.
 *
 * These are plain [Modifier] draw extensions (no recomposition) so they're cheap to use everywhere.
 */

/** Soft-charcoal palette tuned for a high-contrast, tactile extrusion in dark mode. */
object Neo {
    val Base = Color(0xFF262A33)          // surfaces AND background share this — the neo essence
    val BaseElevated = Color(0xFF2C313C)  // a hair lighter for stacked/hero cards
    val Light = Color(0xFF3A404E)          // top-left highlight (clearly lighter than Base)
    val Dark = Color(0xFF0B0C10)           // bottom-right shadow (near-black for real depth)
    val BevelLight = Color(0x663A414F)     // bright top-left rim of the bevel edge
    val BevelDark = Color(0x800A0B0E)      // dark bottom-right rim of the bevel edge
    val OnSurface = Color(0xFFECEEF3)
    val OnSurfaceVariant = Color(0xFF8E93A1)
    val Accent = Color(0xFF8E9BFF)         // saturated periwinkle — reserved for interactive accents
    val OnAccent = Color(0xFF12141B)
    val AccentGlow = Color(0x668E9BFF)     // soft colored halo behind accent buttons
    val Stroke = Color(0x14FFFFFF)
}

/** True only inside a neomorphism-themed subtree; lets shared composables switch their rendering. */
val LocalNeomorphism = staticCompositionLocalOf { false }

/**
 * Draws twin drop shadows behind the element so it looks raised off the surface. Place BEFORE any
 * `.clip`/`.background` so the shadows bleed outside the bounds (the element needs a little
 * surrounding padding for them to show). Pairs with [neoBevel] applied after the background.
 */
fun Modifier.neoRaised(
    cornerRadius: Dp,
    base: Color = Neo.Base,
    light: Color = Neo.Light,
    dark: Color = Neo.Dark,
    offset: Dp = 7.dp,
    blur: Dp = 20.dp
): Modifier = this.drawBehind {
    val r = cornerRadius.toPx()
    val off = offset.toPx()
    val bl = blur.toPx()
    drawIntoCanvas { canvas ->
        val darkPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = base.toArgb()
            setShadowLayer(bl, off, off, dark.toArgb())
        }
        val lightPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = base.toArgb()
            setShadowLayer(bl, -off, -off, light.toArgb())
        }
        canvas.nativeCanvas.apply {
            drawRoundRect(0f, 0f, size.width, size.height, r, r, darkPaint)
            drawRoundRect(0f, 0f, size.width, size.height, r, r, lightPaint)
        }
    }
}

/**
 * A soft colored halo behind an accent (primary) surface — e.g. the Now Playing play button — so it
 * glows and clearly reads as the one interactive focal point. Place before `.clip`/`.background`.
 */
fun Modifier.neoAccentGlow(
    cornerRadius: Dp,
    accent: Color = Neo.Accent,
    glow: Color = Neo.AccentGlow,
    blur: Dp = 22.dp
): Modifier = this.drawBehind {
    val r = cornerRadius.toPx()
    val bl = blur.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = accent.toArgb()
            setShadowLayer(bl, 0f, 0f, glow.toArgb())
        }
        canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
    }
}

/**
 * A crisp diagonal bevel: a hairline that is bright at the top-left and dark at the bottom-right,
 * giving the element a defined, tactile edge. Renders on every device (no blur required), so it is
 * the reliable backbone of the extruded look. Apply AFTER the background, with the same [shape].
 */
fun Modifier.neoBevel(
    shape: Shape,
    light: Color = Neo.BevelLight,
    dark: Color = Neo.BevelDark,
    width: Dp = 1.5.dp
): Modifier = this.border(
    width = width,
    brush = Brush.linearGradient(listOf(light, Color.Transparent, dark)),
    shape = shape
)

/**
 * Carves an inset "well" — twin shadows cast *inward* from the rim, so the element looks pressed into
 * the surface. Used for selected/active states (e.g. the current nav pill). Draws over content, so
 * put the fill/content first; a matching `.clip` keeps edges crisp.
 */
fun Modifier.neoInset(
    cornerRadius: Dp,
    base: Color = Neo.Base,
    light: Color = Neo.Light,
    dark: Color = Neo.Dark,
    offset: Dp = 5.dp,
    blur: Dp = 12.dp
): Modifier = this.drawWithContent {
    drawContent()
    val r = cornerRadius.toPx()
    val off = offset.toPx()
    val bl = blur.toPx()
    val w = size.width
    val h = size.height
    val rounded = Path().apply { addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(r, r))) }
    // A frame that fills everything *outside* the rounded rect; its inner rim casts the inward shadow.
    val outer = Path().apply { addRect(Rect(-w, -h, w * 2f, h * 2f)) }
    val frame = Path().apply { op(outer, rounded, PathOperation.Difference) }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.apply {
            val save = save()
            canvas.clipPath(rounded)
            val darkPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = base.toArgb()
                setShadowLayer(bl, off, off, dark.toArgb())
            }
            val lightPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = base.toArgb()
                setShadowLayer(bl, -off, -off, light.toArgb())
            }
            val androidFrame = frame.asAndroidPath()
            drawPath(androidFrame, darkPaint)
            drawPath(androidFrame, lightPaint)
            restoreToCount(save)
        }
    }
}
