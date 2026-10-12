package com.tarit.slipledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Springy spec shared by the press and entrance effects so the whole app moves alike. */
private val Bouncy = spring<Float>(dampingRatio = 0.45f, stiffness = 500f)

/** Shrinks a little while pressed and springs back on release, like a physical button. */
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.95f): Modifier = composed {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, Bouncy, label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** A tappable card that squishes when pressed. */
@Composable
fun PressCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.pressScale(interaction),
        shape = shape,
        color = color,
        interactionSource = interaction,
    ) { Column(content = content) }
}

/**
 * Fades and floats the element up into place the first time it appears, delayed by [index] so a list
 * of sections cascades in. Plays once per screen visit, not again when scrolled back into view.
 */
fun Modifier.appear(index: Int = 0): Modifier = composed {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        delay(40L + index * 55L)
        launch { progress.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 220f)) }
        played = true
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 28.dp.toPx()
        val s = 0.96f + 0.04f * p
        scaleX = s; scaleY = s
    }
}

/** Counts up (or down) to [target] instead of jumping, then settles on the exact value. */
@Composable
fun animatedSatang(target: Long, durationMs: Int = 900): Long {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), tween(durationMs, easing = FastOutSlowInEasing)) }
    return if (anim.isRunning) anim.value.toLong() else target
}

/** Segmented pill whose lime thumb slides (with a little overshoot) to the chosen option. */
@Composable
fun PillTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    track: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    thumb: Color = Lime,
    selectedText: Color = InkBottom,
) {
    val thumbColor by animateColorAsState(thumb, label = "thumbColor")
    BoxWithConstraints(
        modifier.fillMaxWidth().height(46.dp).clip(CircleShape)
            .background(track).padding(4.dp),
    ) {
        val slot = maxWidth / options.size
        val x by animateDpAsState(slot * selected, spring(dampingRatio = 0.65f, stiffness = 380f), label = "thumb")
        Box(Modifier.offset(x = x).width(slot).fillMaxHeight().clip(CircleShape).background(thumbColor))
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier.weight(1f).fillMaxHeight().pressScale(interaction, 0.92f).clip(CircleShape)
                        .clickable(interaction, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    val color by animateColorAsState(
                        if (i == selected) selectedText else MaterialTheme.colorScheme.onSurfaceVariant, label = "tab",
                    )
                    Text(label, color = color, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Gentle endless up-down drift for decorative icons. */
fun Modifier.floating(amplitude: Float = 6f, periodMs: Int = 2400): Modifier = composed {
    val y by rememberInfiniteTransition(label = "float").animateFloat(
        -amplitude, amplitude, infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y",
    )
    graphicsLayer { translationY = y.dp.toPx() }
}
