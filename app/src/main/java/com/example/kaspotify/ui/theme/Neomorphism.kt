package com.example.kaspotify.ui.theme

import androidx.compose.foundation.border
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
 * Neomorphism ("soft UI") theming — v3.
 *
 * What earlier iterations got wrong: they filled surfaces with a FLAT color and hoped the outer
 * shadows would carry the depth. They can't — on dark themes the shadows sit outside the element
 * where scroll containers may clip them, and a flat fill reads as a plain rectangle no matter what
 * happens at its edges. Real soft-UI reads convex because the surface itself is lit: a diagonal
 * gradient, brighter where the (imaginary top-left) light hits and darker where it falls away.
 *
 * So the v3 recipe for a raised element, in order:
 *   1. [neoRaised]  — twin outer shadows (kept tight so they survive partial clipping)
 *   2. clip(shape)
 *   3. background([Neo.surface]) — the diagonal *gradient* fill; this is what sells the depth
 *   4. [neoBevel]   — a soft 1dp lit rim, bright top-left → dark bottom-right
 *
 * Pressed / selected states invert the light: [Neo.pressed] (gradient reversed, slightly darker)
 * under a [neoInset] inner shadow, so wells look carved instead of painted.
 */

/** Blue-charcoal palette, tuned so the gradient-lit surfaces have real tonal range. */
object Neo {
    // Page ground. Slightly darker than the surface gradient's midpoint so raised elements float.
    val Base = Color(0xFF282C33)

    // Raised-surface gradient endpoints (top-left lit → bottom-right shaded).
    val SurfaceLit = Color(0xFF343A44)
    val SurfaceShade = Color(0xFF22252B)

    // Pressed/inset gradient endpoints (light inverted, a touch darker overall).
    val PressedLit = Color(0xFF1E2126)
    val PressedShade = Color(0xFF2C313A)

    // Twin outer shadows.
    val Light = Color(0xFF3F4754)
    val Dark = Color(0xFF0D0E12)

    // Bevel rim.
    val BevelLight = Color(0x4DFFFFFF)
    val BevelDark = Color(0x59000000)

    val OnSurface = Color(0xFFECEEF3)
    val OnSurfaceVariant = Color(0xFF959BA8)

    // Periwinkle accent, reserved for interactive focal points.
    val Accent = Color(0xFF8E9BFF)
    val OnAccent = Color(0xFF12141B)
    val AccentGlow = Color(0x408E9BFF)

    val Stroke = Color(0x14FFFFFF)

    /** Diagonal convex fill for raised surfaces — the core of the soft-UI look. */
    fun surface(): Brush = Brush.linearGradient(
        colors = listOf(SurfaceLit, SurfaceShade),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    /** Reversed (concave) fill for pressed/selected wells. */
    fun pressed(): Brush = Brush.linearGradient(
        colors = listOf(PressedLit, PressedShade),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    /** Convex fill in the accent color for the primary control. */
    fun accentSurface(): Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFA3AEFF), Color(0xFF7683E8)),
        start = Offset.Zero,
        end = Offset.Infinite
    )
}

/** True only inside a neomorphism-themed subtree; lets shared composables switch their rendering. */
val LocalNeomorphism = staticCompositionLocalOf { false }

/**
 * Twin outer drop shadows. Tight by design (they support the gradient fill rather than carry the
 * whole effect), so partial clipping by list containers doesn't collapse the look. Apply BEFORE
 * `.clip`/`.background`.
 */
fun Modifier.neoRaised(
    cornerRadius: Dp,
    base: Color = Neo.Base,
    light: Color = Neo.Light,
    dark: Color = Neo.Dark,
    offset: Dp = 5.dp,
    blur: Dp = 12.dp
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

/** A soft colored halo behind the accent (primary) control. Apply before `.clip`/`.background`. */
fun Modifier.neoAccentGlow(
    cornerRadius: Dp,
    accent: Color = Neo.Accent,
    glow: Color = Neo.AccentGlow,
    blur: Dp = 18.dp
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
 * A soft lit rim: 1dp hairline, bright at the top-left fading through transparent to dark at the
 * bottom-right. Renders identically on every device (plain border, no blur), giving each surface a
 * defined edge even where shadows are subtle. Apply AFTER the background, with the same [shape].
 */
fun Modifier.neoBevel(
    shape: Shape,
    light: Color = Neo.BevelLight,
    dark: Color = Neo.BevelDark,
    width: Dp = 1.dp
): Modifier = this.border(
    width = width,
    brush = Brush.linearGradient(listOf(light, Color.Transparent, dark)),
    shape = shape
)

/**
 * Inner twin shadows cast inward from the rim so the element reads pressed INTO the surface.
 * Combine with a [Neo.pressed] background for the full carved-well effect. Draws over content;
 * apply after `.clip`/`.background`.
 */
fun Modifier.neoInset(
    cornerRadius: Dp,
    base: Color = Neo.Base,
    light: Color = Neo.Light,
    dark: Color = Neo.Dark,
    offset: Dp = 4.dp,
    blur: Dp = 10.dp
): Modifier = this.drawWithContent {
    drawContent()
    val r = cornerRadius.toPx()
    val off = offset.toPx()
    val bl = blur.toPx()
    val w = size.width
    val h = size.height
    val rounded = Path().apply { addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(r, r))) }
    // A frame filling everything *outside* the rounded rect; its inner rim casts the inward shadow.
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
