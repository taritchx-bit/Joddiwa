package com.tarit.slipledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.random.Random

/**
 * Pill switch that turns a daytime sky (sun, clouds) into a starry night with a crescent moon.
 * Tap to toggle or drag the knob; the sky cross-fades with the knob position.
 */
@Composable
fun DayNightSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
) {
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)
    LaunchedEffect(checked) { progress.animateTo(if (checked) 1f else 0f, Motion) }

    // Seconds, wrapping at 360 so every period below divides it and loops seamlessly.
    val clock = rememberInfiniteTransition(label = "sky").animateFloat(
        0f, 360f, infiniteRepeatable(tween(360_000, easing = LinearEasing)), label = "clock",
    )

    Canvas(
        modifier
            .size(width = height * (69f / 22f), height = height)
            .shadow(height * 0.18f, RoundedCornerShape(percent = 47), clip = false)
            .semantics { stateDescription = if (checked) "มืด" else "สว่าง" }
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
            ) { on ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(on)
            }
            .pointerInput(Unit) {
                fun settle() {
                    val on = progress.value > 0.5f
                    scope.launch { progress.animateTo(if (on) 1f else 0f, Motion) }
                    if (on != currentChecked) currentOnChange(on)
                }
                detectHorizontalDragGestures(onDragEnd = ::settle, onDragCancel = ::settle) { change, dx ->
                    change.consume()
                    val travel = (size.width - size.height).toFloat()
                    scope.launch { progress.snapTo((progress.value + dx / travel).coerceIn(0f, 1f)) }
                }
            },
    ) {
        val p = progress.value
        val t = clock.value
        val h = size.height
        val pill = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, h, CornerRadius(h * 0.47f))) }
        clipPath(pill) {
            drawSky(p, t)
            drawSun(p)
            drawMoon(p)
            drawClouds(t)
        }
        drawKnob(p)
    }
}

private val Motion = tween<Float>(650, easing = CubicBezierEasing(0.45f, 0f, 0.2f, 1f))

private val DayBrush = Brush.horizontalGradient(
    0f to Color(0xFFCFE6E6), 0.22f to Color(0xFFA3CDEA), 0.55f to Color(0xFF78B1E3), 1f to Color(0xFF5F9ED8),
)
private val NightBrush = Brush.verticalGradient(
    0f to Color(0xFF2A2D68), 0.5f to Color(0xFF1A1D4C), 1f to Color(0xFF101338),
)

private class Star(val x: Float, val y: Float, val period: Float, val delay: Float)

/** Stars in the upper part of the pill, as fractions of its size. */
private val Stars = Random(9).let { r ->
    val periods = listOf(2f, 2.4f, 3f, 3.6f, 4f, 4.5f, 5f)
    List(9) { Star(r.nextFloat(), r.nextFloat() * 0.55f, periods[r.nextInt(periods.size)], r.nextFloat() * 4f) }
}

/** Thin flat clouds near the top: left, top, width as fractions. */
private val Wisps = listOf(
    Triple(.22f, .10f, .07f), Triple(.34f, .12f, .05f), Triple(.48f, .24f, .06f),
    Triple(.64f, .17f, .08f), Triple(.44f, .06f, .04f), Triple(.75f, .31f, .05f),
)

private fun DrawScope.drawSky(p: Float, t: Float) {
    val w = size.width
    val h = size.height
    drawRect(DayBrush)
    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), endY = h * 0.6f))
    drawRect(NightBrush, alpha = p)

    if (p > 0f) {
        val r = max(h * 0.025f, 1f)
        for (s in Stars) {
            val wave = 0.5f - 0.5f * cos(2f * PI.toFloat() * (t + s.delay) / s.period)
            drawCircle(Color.White, r, Offset(s.x * w, s.y * h), alpha = (0.1f + 0.85f * wave) * p)
        }
    }

    val wispH = max(h * 0.02f, 1f)
    for ((left, top, width) in Wisps) {
        drawRoundRect(
            Color.White, Offset(left * w, top * h), Size(width * w, wispH), CornerRadius(wispH),
            alpha = 0.75f * (1f - 0.65f * p),
        )
    }
}

private fun DrawScope.drawSun(p: Float) {
    if (p >= 1f) return
    val h = size.height
    val d = h * 0.42f
    val left = size.width - h * 0.12f - d
    val top = h * 0.14f + 0.18f * d * p
    val center = Offset(left + d / 2, top + d / 2)
    val alpha = 1f - p
    withTransform({ scale(1f - 0.4f * p, 1f - 0.4f * p, pivot = center) }) {
        val glow = d / 2 + h * 0.2f
        drawCircle(
            Brush.radialGradient(
                0.4f to Color(0x99FFE478), 1f to Color.Transparent, center = center, radius = glow,
            ),
            glow, center, alpha = alpha,
        )
        drawCircle(
            Brush.radialGradient(
                0f to Color(0xFFFFEB9A), 0.7f to Color(0xFFFFD966),
                center = Offset(left + 0.4f * d, top + 0.35f * d), radius = 0.885f * d,
            ),
            d / 2, center, alpha = alpha,
        )
    }
}

/** Crescent from a 100x100 viewBox. */
private val MoonPath = PathParser().parsePathString("M58 6 A44 44 0 1 0 94 68 A36 36 0 0 1 58 6 Z").toPath()
private val Craters = listOf(Triple(22f, 52f, 4.6f), Triple(32f, 74f, 3.6f), Triple(25f, 31f, 3.1f), Triple(44f, 88f, 2.6f))

private fun DrawScope.drawMoon(p: Float) {
    if (p <= 0f) return
    val h = size.height
    val d = h * 0.48f
    val q = 1f - p
    withTransform({
        translate(h * 0.1f, h * 0.1f - 0.2f * d * q)
        rotate(-10f * q, pivot = Offset(d / 2, d / 2))
        scale(0.6f + 0.4f * p, 0.6f + 0.4f * p, pivot = Offset(d / 2, d / 2))
        scale(d / 100f, d / 100f, pivot = Offset.Zero)
    }) {
        drawPath(MoonPath, Color(0xFFF6C545), alpha = p)
        drawPath(MoonPath, Color(0xFF12131C), alpha = p, style = Stroke(width = 5.5f, join = StrokeJoin.Round))
        for ((x, y, r) in Craters) drawCircle(Color(0xFFE3A62B), r, Offset(x, y), alpha = p)
    }
}

private class Circle(val x: Float, val y: Float, val r: Float)

private class CloudLayer(val circles: List<Circle>, val base: Float, val brush: Brush)

/** Overlapping circles with a flat base, in a 400x127 viewBox. */
private fun cloudLayer(seed: Int, base: Float, rMin: Float, rMax: Float, step: Float, brush: Brush): CloudLayer {
    val rnd = Random(seed)
    val circles = mutableListOf<Circle>()
    var x = -20f
    while (x < 430f) {
        val r = rMin + rnd.nextFloat() * (rMax - rMin)
        circles += Circle(x, base - r * 0.3f, r)
        x += step * (0.65f + rnd.nextFloat() * 0.7f)
    }
    return CloudLayer(circles, base, brush)
}

private val BackClouds = cloudLayer(
    7, 106f, 17f, 27f, 27f,
    Brush.verticalGradient(0f to Color(0xFFCFE3FA), 1f to Color(0xFF6EA6E6), startY = 66f, endY = 127f),
)
private val FrontClouds = cloudLayer(
    19, 123f, 19f, 31f, 31f,
    Brush.verticalGradient(
        0f to Color.White, 0.6f to Color(0xFFE6F1FD), 1f to Color(0xFF9CC3EE), startY = 78f, endY = 127f,
    ),
)

private fun DrawScope.drawClouds(t: Float) {
    withTransform({ scale(size.width / 400f, size.height / 127f, pivot = Offset.Zero) }) {
        // Slow sideways drift, the two layers out of step.
        drawCloudLayer(BackClouds, -6f * cos(PI.toFloat() * t / 12f))
        drawCloudLayer(FrontClouds, 6f * cos(PI.toFloat() * t / 9f))
    }
}

private fun DrawScope.drawCloudLayer(layer: CloudLayer, dx: Float) {
    withTransform({ translate(left = dx) }) {
        for (c in layer.circles) drawCircle(layer.brush, c.r, Offset(c.x, c.y))
        drawRect(layer.brush, Offset(-30f, layer.base), Size(460f, 60f))
    }
}

private fun DrawScope.drawKnob(p: Float) {
    val h = size.height
    val k = h * 0.85f
    val pad = (h - k) / 2
    val center = Offset(pad + k / 2 + p * (size.width - h), h / 2)

    val glow = 0.92f * k * (0.85f + 0.27f * p)
    drawCircle(
        Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.95f), 0.38f to Color.White.copy(alpha = 0.95f),
            0.52f to Color.White.copy(alpha = 0.35f), 0.7f to Color.Transparent,
            center = center, radius = glow,
        ),
        glow, center, alpha = 0.5f + 0.5f * p,
    )
    drawCircle(
        Brush.radialGradient(
            0f to Color.White, 0.55f to Color.White, 1f to Color(0xFFF4F6FA),
            center = Offset(center.x - k / 2 + 0.38f * k, center.y - k / 2 + 0.32f * k), radius = 0.92f * k,
        ),
        k / 2, center,
    )
}
