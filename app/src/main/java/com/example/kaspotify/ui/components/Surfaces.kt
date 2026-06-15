package com.example.kaspotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassStroke
import com.example.kaspotify.ui.theme.LocalAmbientColor

/**
 * A full-screen backdrop: a soft vertical gradient from the album-art–derived ambient color at the
 * top fading into the app background. The whole premium look hangs off this — and it's just a
 * single linear-gradient draw (no blur), so it stays cheap.
 */
@Composable
fun GradientBackdrop(
    modifier: Modifier = Modifier,
    ambient: Color = LocalAmbientColor.current,
    content: @Composable BoxScope.() -> Unit
) {
    val base = MaterialTheme.colorScheme.background
    val brush = Brush.verticalGradient(
        0f to ambient.copy(alpha = 0.42f).compositeOver(base),
        0.45f to ambient.copy(alpha = 0.12f).compositeOver(base),
        1f to base
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .background(brush),
        content = content
    )
}

/** Soft translucent card — the "glass without blur" surface used across the app. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    fill: Color = GlassFill,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, GlassStroke, shape),
        content = content
    )
}

/** Lightweight transparent top bar that floats over the gradient backdrop. */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            content = actions
        )
    }
}

/** A small circular icon button on a glass chip — used for floating controls. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 50))
            .background(GlassFill)
            .border(1.dp, GlassStroke, RoundedCornerShape(percent = 50)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, content = { content() })
    }
}

/** Vertical spacer shorthand. */
@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))
