package com.tarit.slipledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Liquid glass over the surrounding [LocalBackdrop] (an ink panel, the screen behind the tab bar);
 * frosted glass where there is no backdrop to look through.
 */
fun Modifier.glass(shape: Shape, strength: Float = 1f): Modifier = composed {
    // No lens bend here: over the panel's soft glow, bending only shows up as a ring of slightly different
    // colour inside the edge, which reads as a box within the pane. The tab bar keeps it, where there is
    // real detail behind to bend.
    liquidGlass(shape, LocalBackdrop.current, highlight = strength, refraction = 0.dp)
}

/**
 * Frosted-glass surface for use on ink panels: a faint milky fill that is brighter at the top, a rim that
 * catches light on the top-left edge, and a soft sheen across the upper half.
 */
fun Modifier.frostedGlass(shape: Shape, strength: Float = 1f): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f * strength), Color.White.copy(alpha = 0.05f * strength)),
        ),
    )
    .drawWithContent {
        drawContent()
        // Sheen: a pale diagonal wash fading out before the middle, like light on a pane.
        drawRect(
            Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.10f * strength),
                0.45f to Color.Transparent,
                start = Offset.Zero,
                end = Offset(size.width * 0.6f, size.height),
            ),
        )
    }
    .border(
        1.dp,
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.45f * strength),
            0.4f to Color.White.copy(alpha = 0.08f * strength),
            1f to Color.White.copy(alpha = 0.22f * strength),
        ),
        shape,
    )
