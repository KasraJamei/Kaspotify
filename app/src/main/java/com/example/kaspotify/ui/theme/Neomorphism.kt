package com.example.kaspotify.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neomorphism ("soft UI") theming.
 *
 * The whole look is built from a single mid-tone base color and two twin shadows: a light highlight
 * cast from the top-left and a dark shadow cast to the bottom-right. When both sit on a surface that
 * is *the same color* as its background, the element appears gently extruded from — or pressed into —
 * the page. That's the entire trick, and it's why the neo palette is a soft charcoal (not the app's
 * usual deep black): pure black can't show a lighter highlight.
 *
 * These are plain [Modifier] draw extensions (no per-frame recomposition) so they're cheap to sprinkle
 * across shared surfaces. [neoRaised] extrudes; [neoInset] carves a well. Both take an explicit corner
 * radius so the caller's `.clip(RoundedCornerShape(r))` lines up exactly.
 */

/** Soft-charcoal palette tuned so twin shadows read clearly without looking muddy. */
object Neo {
    val Base = Color(0xFF23262E)          // surfaces AND background share this — the neo essence
    val BaseElevated = Color(0xFF272B34)  // a hair lighter for stacked cards
    val Light = Color(0xFF31353F)          // top-left highlight
    val Dark = Color(0xFF13151A)           // bottom-right shadow
    val OnSurface = Color(0xFFEDEEF2)
    val OnSurfaceVariant = Color(0xFF969AA6)
    val Accent = Color(0xFFB9C0FF)         // soft periwinkle — the one spot of color
    val Stroke = Color(0x14FFFFFF)
}

/** True only inside a neomorphism-themed subtree; lets shared composables switch their rendering. */
val LocalNeomorphism = staticCompositionLocalOf { false }

/**
 * Draws twin drop shadows behind the element so it looks raised off the surface. Place this BEFORE any
 * `.clip`/`.background` so the shadows bleed outside the bounds (the element needs a little surrounding
 * padding for them to show). The base-colored body is painted here too, so the element reads opaque.
 */
fun Modifier.neoRaised(
    cornerRadius: Dp,
    base: Color = Neo.Base,
    light: Color = Neo.Light,
    dark: Color = Neo.Dark,
    offset: Dp = 6.dp,
    blur: Dp = 16.dp
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
 * Carves an inset "well" — twin shadows cast *inward* from the rim, so the element looks pressed into
 * the surface. Used for selected/active states (e.g. the current nav pill). Draws over content, so put
 * the fill/content first; a matching `.clip(RoundedCornerShape(cornerRadius))` keeps edges crisp.
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
