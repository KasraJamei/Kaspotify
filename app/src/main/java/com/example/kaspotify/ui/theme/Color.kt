package com.example.kaspotify.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Minimal-mono palette on deep black ----------------------------------
// Surfaces: near-black base with two subtly lifted greys.
val Background = Color(0xFF09090B)
val Surface = Color(0xFF141417)
val SurfaceVariant = Color(0xFF1E1E22)

// Translucent "glass" tokens — soft fills/strokes layered over gradients
// without any (expensive) real blur.
val GlassFill = Color(0x14FFFFFF)   // ~8% white
val GlassFillStrong = Color(0x24FFFFFF) // ~14% white
val GlassStroke = Color(0x1FFFFFFF) // ~12% white hairline

// Accent is platinum/white — controls and highlights stay monochrome and classy.
val Platinum = Color(0xFFF6F6F8)
val PlatinumDim = Color(0xFFC9C9D0)

val OnBackground = Color(0xFFF4F4F6)
val OnSurfaceVariant = Color(0xFF98989F)

// Neutral ambient used behind content when no artwork color is available.
val AmbientNeutral = Color(0xFF2A2A30)

// Kept for backwards compatibility with older references; no longer the accent.
val SpotifyGreen = Color(0xFF1DB954)
val SpotifyGreenDark = Color(0xFF179443)
