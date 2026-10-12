package com.tarit.slipledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val Emerald = Color(0xFF17A673)
private val Teal = Color(0xFF1FA3B5)

/**
 * Dark glass panel: slowly drifting coloured light blobs blurred behind a faint grid, and a rim that catches
 * light on the top-left edge. Content placed on it
 * should use [glass] so it reads as frosted panes over the glow.
 */
@Composable
fun InkPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    val motion = rememberInfiniteTransition(label = "ink")
    // Seconds on a 120 s loop; the blob paths below use periods that divide it, so the loop is seamless.
    val t by motion.animateFloat(0f, 120f, infiniteRepeatable(tween(120_000, easing = LinearEasing)), label = "t")
    val backdrop = rememberBackdrop()
    Box(
        modifier.fillMaxWidth().clip(shape).background(HeroGradient)
            .border(
                1.dp,
                Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.28f), 0.35f to Color.White.copy(alpha = 0.04f),
                    1f to Lime.copy(alpha = 0.18f),
                ),
                shape,
            ),
    ) {
        // Everything behind the content is recorded as the backdrop its glass panes refract.
        Box(Modifier.matchParentSize().backdropSource(backdrop)) {
        // Light blobs, blurred into soft glows (real blur on Android 12+, soft gradients alone below that).
        Canvas(Modifier.matchParentSize().blur(48.dp)) {
            val w = size.width
            val h = size.height
            fun blob(color: Color, cx: Float, cy: Float, r: Float) {
                val c = Offset(cx, cy)
                drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), c, r), r, c)
            }
            val a = 2f * PI.toFloat() * t
            blob(Lime.copy(alpha = 0.45f), w * (0.85f + 0.08f * cos(a / 20f)), h * (0.08f + 0.06f * sin(a / 15f)), w * 0.55f)
            blob(Emerald.copy(alpha = 0.40f), w * (0.12f + 0.10f * sin(a / 24f)), h * (0.85f + 0.08f * cos(a / 30f)), w * 0.6f)
            blob(Teal.copy(alpha = 0.22f), w * (0.5f + 0.25f * cos(a / 40f)), h * (0.45f + 0.15f * sin(a / 20f)), w * 0.4f)
        }
        Canvas(Modifier.matchParentSize()) {
            val step = 28.dp.toPx()
            val line = Color.White.copy(alpha = 0.03f)
            var x = step
            while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
            var y = step
            while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y), 1f); y += step }
        }
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) { content() }
    }
}

/**
 * Smooth glowing line chart for ink panels. [values] holds one point per slot up to today; [slots] is the
 * full width of the period so the line grows across the month. Drag or tap to read any day.
 */
@Composable
fun SpendGraph(
    values: List<Long>,
    slots: Int,
    xLabel: (Int) -> String,
    detail: (Int) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
) {
    val count = slots.coerceAtLeast(2)
    val target = values.map { it.toFloat() }
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val morph = remember { Animatable(1f) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(target, count) {
        if (reveal.value == 1f && from.size == target.size && to.size == target.size) {
            // Same days, new numbers (e.g. daily ↔ running total): glide each point to its new height.
            from = to.mapIndexed { i, v -> lerp(from[i], v, morph.value) }
            to = target
            morph.snapTo(0f)
            morph.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 120f))
        } else {
            from = target; to = target
            morph.snapTo(1f)
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        }
    }

    val lastIndex = (values.size - 1).coerceAtLeast(0)
    var selected by remember(count) { mutableIntStateOf(lastIndex) }
    LaunchedEffect(lastIndex) { selected = lastIndex }
    val selX by animateFloatAsState(selected.toFloat(), spring(dampingRatio = 0.7f, stiffness = 400f), label = "sel")
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "p",
    )

    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val latestDetail by rememberUpdatedState(detail)
    val latestLabel by rememberUpdatedState(xLabel)
    val gutter = with(androidx.compose.ui.platform.LocalDensity.current) { 34.dp.toPx() }

    fun pick(x: Float, width: Float) {
        if (values.isEmpty()) return
        val plot = width - gutter
        val i = (((x - gutter) / plot) * (count - 1)).roundToInt().coerceIn(0, lastIndex)
        if (i != selected) {
            selected = i
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Canvas(
        modifier.fillMaxWidth().height(height)
            .pointerInput(count, lastIndex) { detectTapGestures { pick(it.x, size.width.toFloat()) } }
            .pointerInput(count, lastIndex) {
                detectHorizontalDragGestures { change, _ -> change.consume(); pick(change.position.x, size.width.toFloat()) }
            },
    ) {
        val labelStyle = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        val bottomPad = 22.dp.toPx()
        val topPad = 42.dp.toPx()
        val left = gutter
        val plotW = size.width - left
        val plotH = size.height - topPad - bottomPad
        val shown = to.mapIndexed { i, v -> lerp(from.getOrElse(i) { v }, v, morph.value) }
        val yMax = lerp(niceMax(from.maxOrNull() ?: 0f), niceMax(to.maxOrNull() ?: 0f), morph.value)
        fun px(i: Float) = left + plotW * i / (count - 1)
        fun py(v: Float) = topPad + plotH * (1f - v / yMax)

        // Grid with y labels.
        for (k in 0..4) {
            val v = yMax * k / 4
            val y = py(v)
            drawLine(Color.White.copy(alpha = 0.08f), Offset(left, y), Offset(size.width, y), 1f)
            val t = measurer.measure(if (k == 0) "0" else Fmt.bahtCompact(v.toLong()), labelStyle)
            drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), y - t.size.height / 2))
        }
        val every = ceil(count / 7f).toInt().coerceAtLeast(1)
        for (i in 0 until count step every) {
            val x = px(i.toFloat())
            drawLine(Color.White.copy(alpha = 0.05f), Offset(x, topPad), Offset(x, topPad + plotH), 1f)
            val active = i == selected
            val t = measurer.measure(
                latestLabel(i),
                labelStyle.copy(color = if (active) Lime else labelStyle.color, fontWeight = if (active) FontWeight.Bold else null),
            )
            drawText(t, topLeft = Offset((x - t.size.width / 2).coerceIn(left - 4f, size.width - t.size.width), size.height - t.size.height))
        }
        if (shown.isEmpty()) {
            // A period that has not started yet has no days to plot; say so instead of showing a bare grid.
            val msg = measurer.measure("ยังไม่ถึงช่วงนี้", TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp))
            drawText(msg, topLeft = Offset(left + (plotW - msg.size.width) / 2, topPad + (plotH - msg.size.height) / 2))
            return@Canvas
        }
        // `values` switches to a new period one frame before the animated copy does, so draw only from `shown`.
        val last = shown.lastIndex

        val line = smoothPath(shown.indices.map { Offset(px(it.toFloat()), py(shown[it])) })
        val revealRight = left + (px(last.toFloat()) - left) * reveal.value + 12.dp.toPx()
        clipRect(right = revealRight) {
            val fill = Path().apply {
                addPath(line)
                lineTo(px(last.toFloat()), topPad + plotH)
                lineTo(px(0f), topPad + plotH)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(Lime.copy(alpha = 0.32f), Color.Transparent), topPad, topPad + plotH))
            drawPath(line, Lime.copy(alpha = 0.10f), style = Stroke(12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(line, Lime.copy(alpha = 0.22f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(line, Lime, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        if (reveal.value < 1f) return@Canvas
        // Scrubber: vertical rule, pulsing dot and a value bubble, gliding between days.
        val sel = selX.coerceIn(0f, last.toFloat())
        val i0 = floor(sel).toInt().coerceIn(0, last)
        val i1 = (i0 + 1).coerceAtMost(last)
        val v = lerp(shown[i0], shown[i1], sel - i0)
        val dot = Offset(px(sel), py(v))
        drawLine(Color.White.copy(alpha = 0.35f), Offset(dot.x, topPad - 4.dp.toPx()), Offset(dot.x, topPad + plotH), 1.dp.toPx())
        drawCircle(Lime.copy(alpha = 0.45f * (1f - pulse)), 6.dp.toPx() + 12.dp.toPx() * pulse, dot)
        drawCircle(Color.White, 6.dp.toPx(), dot)
        drawCircle(Lime, 3.dp.toPx(), dot)
        drawBubble(measurer, "฿" + Fmt.bahtShort(values.getOrElse(selected) { 0 }), latestDetail(selected), dot.x)
    }
}

private fun DrawScope.drawBubble(measurer: androidx.compose.ui.text.TextMeasurer, value: String, detail: String, x: Float) {
    val v = measurer.measure(value, TextStyle(color = InkBottom, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    val d = measurer.measure(detail, TextStyle(color = InkBottom.copy(alpha = 0.6f), fontSize = 10.sp))
    val padH = 10.dp.toPx()
    val gap = 6.dp.toPx()
    val w = v.size.width + gap + d.size.width + padH * 2
    val h = maxOf(v.size.height, d.size.height) + 8.dp.toPx()
    val left = (x - w / 2).coerceIn(0f, size.width - w)
    drawRoundRect(Color.White, Offset(left, 0f), Size(w, h), CornerRadius(h / 2))
    drawText(v, topLeft = Offset(left + padH, (h - v.size.height) / 2))
    drawText(d, topLeft = Offset(left + padH + v.size.width + gap, (h - d.size.height) / 2 + 1.dp.toPx()))
}

/** Curve through the points with horizontal tangents, so it never overshoots below zero. */
private fun smoothPath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points[0].x, points[0].y)
    if (points.size == 1) { lineTo(points[0].x + 0.1f, points[0].y); return@apply }
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val mx = (a.x + b.x) / 2
        cubicTo(mx, a.y, mx, b.y, b.x, b.y)
    }
}

/** Rounds the axis top up to 1, 2 or 5 × 10ⁿ satang so the labels are tidy. */
private fun niceMax(v: Float): Float {
    if (v <= 0f) return 100_00f
    val exp = 10.0.pow(floor(log10(v.toDouble()))).toFloat()
    val f = v / exp
    val nice = when {
        f <= 1f -> 1f
        f <= 2f -> 2f
        f <= 5f -> 5f
        else -> 10f
    }
    return nice * exp
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
