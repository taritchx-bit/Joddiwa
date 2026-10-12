package com.tarit.slipledger.ui

import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.tarit.slipledger.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

val Pacifico = FontFamily(Font(R.font.pacifico))

/** Once per app launch; tapping the title replays it any time. */
private var signedThisLaunch = false

/** The word as one outline per pen stroke, laid out from the top-left of the canvas. */
private class Glyphs(val fill: Path, val strokes: List<Pair<android.graphics.Path, Float>>, val width: Float, val height: Float) {
    val total = strokes.sumOf { it.second.toDouble() }.toFloat()
}

/**
 * The app name signed by a cartoon pencil: its tip runs along each letter of the script word, ink floods
 * in behind it, a lime highlighter swipes under the word, the pencil spins away, [suffix] bounces in and a
 * few sparkles twinkle. Tap to sign again.
 */
@Composable
fun SignatureTitle(text: String, suffix: String?) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val ink = MaterialTheme.colorScheme.onSurface
    val pad = with(density) { 6.dp.toPx() }
    val glyphs = remember(text, density) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, R.font.pacifico)
            textSize = with(density) { 34.sp.toPx() }
        }
        val path = android.graphics.Path()
        paint.getTextPath(text, 0, text.length, 0f, 0f, path)
        val bounds = RectF().also { path.computeBounds(it, true) }
        path.offset(pad - bounds.left, pad - bounds.top)
        val strokes = mutableListOf<Pair<android.graphics.Path, Float>>()
        val measure = PathMeasure(path, false)
        do {
            val len = measure.length
            if (len > 0f) strokes += android.graphics.Path().also { measure.getSegment(0f, len, it, true) } to len
        } while (measure.nextContour())
        Glyphs(path.asComposePath(), strokes, bounds.width() + pad * 2, bounds.height() + pad * 2)
    }

    val trace = remember { Animatable(if (signedThisLaunch) 1f else 0f) }
    val flood = remember { Animatable(if (signedThisLaunch) 1f else 0f) }
    val marker = remember { Animatable(if (signedThisLaunch) 1f else 0f) }
    val away = remember { Animatable(if (signedThisLaunch) 1f else 0f) }
    val tag = remember { Animatable(if (signedThisLaunch) 1f else 0f) }
    val sparkle = remember { Animatable(if (signedThisLaunch) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val running = remember { arrayOfNulls<Job>(1) }

    suspend fun sign() = coroutineScope {
        listOf(trace, flood, marker, away, tag, sparkle).forEach { it.snapTo(0f) }
        trace.animateTo(1f, tween(1800, easing = CubicBezierEasing(0.3f, 0f, 0.6f, 1f)))
        signedThisLaunch = true
        launch { flood.animateTo(1f, tween(350)) }
        launch { away.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        marker.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        launch { tag.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f)) }
        delay(120)
        sparkle.animateTo(1f, tween(900, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        if (!signedThisLaunch) { delay(350); sign() }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        Canvas(
            Modifier.size(with(density) { glyphs.width.toDp() }, with(density) { glyphs.height.toDp() })
                .clickable(remember { MutableInteractionSource() }, indication = null) {
                    running[0]?.cancel()
                    running[0] = scope.launch { sign() }
                },
        ) {
            // Highlighter swipe behind the lower half of the word, slightly tilted like a hand-drawn mark.
            if (marker.value > 0f) {
                rotate(-2f, pivot = Offset(0f, size.height)) {
                    drawRoundRect(
                        Lime.copy(alpha = 0.55f),
                        topLeft = Offset(pad * 0.5f, size.height * 0.55f),
                        size = Size((size.width - pad) * marker.value, size.height * 0.28f),
                        cornerRadius = CornerRadius(size.height * 0.14f),
                    )
                }
            }
            // Pen strokes traced so far.
            var left = glyphs.total * trace.value
            var tip = Offset(pad, size.height * 0.7f)
            val nativeStroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1.6.dp.toPx()
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                color = android.graphics.Color.argb((ink.alpha * 255).toInt(), (ink.red * 255).toInt(), (ink.green * 255).toInt(), (ink.blue * 255).toInt())
            }
            val pos = FloatArray(2)
            for ((stroke, len) in glyphs.strokes) {
                if (left <= 0f) break
                val part = minOf(len, left)
                val measure = PathMeasure(stroke, false)
                val seg = android.graphics.Path()
                measure.getSegment(0f, part, seg, true)
                drawContext.canvas.nativeCanvas.drawPath(seg, nativeStroke)
                measure.getPosTan(part, pos, null)
                tip = Offset(pos[0], pos[1])
                left -= len
            }
            // Ink floods the letters once the outline is complete.
            if (flood.value > 0f) drawPath(glyphs.fill, ink.copy(alpha = flood.value))

            if (sparkle.value in 0.001f..0.999f) drawSparkles(sparkle.value, Offset(size.width - pad, pad * 1.5f))
            if (away.value < 1f) {
                val scribble = if (trace.value < 1f) sin(trace.value * PI.toFloat() * 40f) * 1.2.dp.toPx() else 0f
                val fly = away.value
                drawPencil(
                    tip = tip + Offset(fly * 40.dp.toPx(), -fly * 46.dp.toPx() + scribble),
                    angle = -50f - fly * 260f,
                    alpha = 1f - fly,
                )
            }
        }
        if (suffix != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                suffix,
                style = TextStyle(fontFamily = Pacifico, fontSize = 14.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp).graphicsLayer {
                    alpha = tag.value.coerceIn(0f, 1f)
                    scaleX = tag.value; scaleY = tag.value
                    rotationZ = (1f - tag.value) * -25f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                },
            )
        }
    }
}

/** Cartoon pencil with its graphite point at [tip], body running off at [angle] degrees. */
private fun DrawScope.drawPencil(tip: Offset, angle: Float, alpha: Float) {
    val u = 1.dp.toPx()
    val half = 3.6f * u
    translate(tip.x, tip.y) {
        rotate(angle, pivot = Offset.Zero) {
            // Shadow, offset down so the pencil seems to hover over the page.
            drawRoundRect(Color.Black.copy(alpha = 0.12f * alpha), Offset(6 * u, -half + 3 * u), Size(32 * u, half * 2), CornerRadius(2 * u))
            val wood = Path().apply { moveTo(0f, 0f); lineTo(9 * u, -half); lineTo(9 * u, half); close() }
            drawPath(wood, Color(0xFFF3CD95).copy(alpha = alpha))
            val lead = Path().apply { moveTo(0f, 0f); lineTo(3.4f * u, -1.4f * u); lineTo(3.4f * u, 1.4f * u); close() }
            drawPath(lead, Color(0xFF3A3A44).copy(alpha = alpha))
            drawRect(Color(0xFFFFC233).copy(alpha = alpha), Offset(9 * u, -half), Size(20 * u, half * 2))
            drawRect(Color(0xFFE5A400).copy(alpha = alpha), Offset(9 * u, -half * 0.33f), Size(20 * u, half * 0.66f))
            drawRect(Color(0xFFB9BEC7).copy(alpha = alpha), Offset(29 * u, -half), Size(3.5f * u, half * 2))
            drawRoundRect(Color(0xFFF59AB5).copy(alpha = alpha), Offset(32.5f * u, -half), Size(5 * u, half * 2), CornerRadius(2 * u))
        }
    }
}

/** Three four-point stars that pop and fade around [at]. */
private fun DrawScope.drawSparkles(t: Float, at: Offset) {
    val u = 1.dp.toPx()
    listOf(Triple(Offset(0f, 0f), 7f, 0f), Triple(Offset(-16f, 10f), 4.5f, 0.15f), Triple(Offset(10f, 16f), 3.5f, 0.3f))
        .forEach { (o, r, delayT) ->
            val local = ((t - delayT) / (1f - delayT)).coerceIn(0f, 1f)
            val s = sin(local * PI.toFloat())
            if (s <= 0f) return@forEach
            val c = at + Offset(o.x * u, o.y * u)
            val star = Path().apply {
                val big = r * u * s
                val small = big * 0.28f
                moveTo(c.x, c.y - big); lineTo(c.x + small, c.y - small); lineTo(c.x + big, c.y); lineTo(c.x + small, c.y + small)
                lineTo(c.x, c.y + big); lineTo(c.x - small, c.y + small); lineTo(c.x - big, c.y); lineTo(c.x - small, c.y - small); close()
            }
            drawPath(star, Color(0xFFFFC233).copy(alpha = s))
        }
}
