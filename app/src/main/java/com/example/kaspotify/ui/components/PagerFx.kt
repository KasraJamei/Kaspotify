package com.example.kaspotify.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue

/**
 * A Niagara-launcher-style page transition: as a page slides away from the settled center it gently
 * scales down and fades, while the incoming page grows back to full — a soft "depth crossfade"
 * rather than a flat slide.
 *
 * Read inside [graphicsLayer] so the values are recomputed on the draw pass as the pager scrolls,
 * without triggering recomposition of the page content.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.niagaraPage(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    val dist = offset.absoluteValue.coerceIn(0f, 1f)
    alpha = 0.45f + (1f - dist) * 0.55f
    val scale = 0.88f + (1f - dist) * 0.12f
    scaleX = scale
    scaleY = scale
}
