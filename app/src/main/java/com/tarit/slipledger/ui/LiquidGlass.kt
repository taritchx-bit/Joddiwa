package com.tarit.slipledger.ui

import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * What a liquid-glass element sees through itself: the drawing of some area behind it, recorded every frame
 * by [backdropSource]. Glass elements re-draw a blurred, refracted copy of the part under them.
 */
class Backdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
}

/** The backdrop that [glass] elements inside an ink panel (or other host) refract. */
val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun rememberBackdrop(): Backdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { Backdrop(layer) }
}

/** Records this element's drawing into [backdrop] while still drawing it normally. */
fun Modifier.backdropSource(backdrop: Backdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/**
 * Liquid glass: the backdrop under the element, blurred, then bent by a lens at the rim (with a touch of
 * colour fringing), under a clear tint, a bright rim and a top highlight.
 *
 * Android 13+ gets refraction and blur, Android 12 blur only; older versions fall back to [frostedGlass].
 */
fun Modifier.liquidGlass(
    shape: Shape,
    backdrop: Backdrop?,
    blur: Dp = 8.dp,
    refraction: Dp = 14.dp,
    rimWidth: Dp = 18.dp,
    tint: Color = Color.White.copy(alpha = 0.06f),
    highlight: Float = 1f,
): Modifier {
    if (backdrop == null || Build.VERSION.SDK_INT < 31) return frostedGlass(shape, highlight)
    return composed {
        val glassLayer = rememberGraphicsLayer()
        var position by remember { mutableStateOf(Offset.Zero) }
        val effects = remember { GlassEffects() }
        onGloballyPositioned { position = it.positionInRoot() }.drawWithContent {
            val outline = shape.createOutline(size, layoutDirection, this)
            val clip = Path().apply { addOutline(outline) }
            val radius = when (outline) {
                is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
                is Outline.Rectangle -> 0f
                else -> minOf(size.width, size.height) / 2
            }
            val blurPx = blur.toPx()
            val bendPx = refraction.toPx()
            // Spare backdrop around the element so blur and refraction have real pixels to pull from at the rim.
            val margin = blurPx * 2 + bendPx + 4f
            val rel = position - backdrop.origin

            glassLayer.renderEffect = effects.get(size, margin, radius, blurPx, rimWidth.toPx(), bendPx)
            glassLayer.record(IntSize((size.width + margin * 2).toInt(), (size.height + margin * 2).toInt())) {
                translate(margin - rel.x, margin - rel.y) { drawLayer(backdrop.layer) }
            }
            clipPath(clip) {
                translate(-margin, -margin) { drawLayer(glassLayer) }
                drawRect(tint)
                // Highlight pooled along the top, as light enters a thick pane.
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.14f * highlight), 0.4f to Color.Transparent,
                    ),
                )
                // Rim, drawn inside the edge only: catches light at the top-left and bottom-right corners and
                // fades out along the sides, so it reads as one pane rather than an outlined box.
                drawPath(
                    clip,
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.75f * highlight),
                        0.3f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color.White.copy(alpha = 0.35f * highlight),
                        start = Offset.Zero, end = Offset(size.width, size.height),
                    ),
                    style = Stroke(2.dp.toPx()),
                )
            }
            drawContent()
        }
    }
}

/** Builds the blur + lens effect once per size, since a shader's uniforms are fixed when the effect is made. */
private class GlassEffects {
    private var key: List<Float>? = null
    private var effect: RenderEffect? = null

    fun get(size: Size, margin: Float, radius: Float, blur: Float, rim: Float, bend: Float): RenderEffect? {
        val k = listOf(size.width, size.height, margin, radius, blur, rim, bend)
        if (k != key) {
            key = k
            effect = build(size, margin, radius, blur, rim, bend)
        }
        return effect
    }

    private fun build(size: Size, margin: Float, radius: Float, blur: Float, rim: Float, bend: Float): RenderEffect? {
        if (Build.VERSION.SDK_INT < 31) return null
        val blurFx = if (blur > 0f) android.graphics.RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP) else null
        if (Build.VERSION.SDK_INT < 33) return blurFx?.asComposeRenderEffect()
        val lens = lensEffect(size, margin, radius, rim, bend)
        return (if (blurFx != null) android.graphics.RenderEffect.createChainEffect(lens, blurFx) else lens)
            .asComposeRenderEffect()
    }

    @RequiresApi(33)
    private fun lensEffect(size: Size, margin: Float, radius: Float, rim: Float, bend: Float): android.graphics.RenderEffect {
        val shader = RuntimeShader(LENS_SHADER).apply {
            setFloatUniform("size", size.width, size.height)
            setFloatUniform("margin", margin, margin)
            setFloatUniform("radius", radius)
            setFloatUniform("band", rim)
            setFloatUniform("strength", bend)
            setFloatUniform("dispersion", 0.05f)
        }
        return android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
    }
}

/**
 * Rounded-rectangle lens. Inside a band along the rim the image is pulled from further out along the edge
 * normal, following a circular bevel profile, so whatever lies just outside the pane appears to wrap round
 * its edge. Red and blue are bent slightly differently for the faint rainbow fringe of real glass.
 */
private const val LENS_SHADER = """
uniform shader content;
uniform float2 size;
uniform float2 margin;
uniform float radius;
uniform float band;
uniform float strength;
uniform float dispersion;

float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 p = coord - margin - halfSize;
    float r = min(radius, min(halfSize.x, halfSize.y));
    float d = sdRoundRect(p, halfSize, r);
    if (d > 0.0 || band <= 0.0) {
        return content.eval(coord);
    }
    float t = clamp(1.0 + d / band, 0.0, 1.0);
    // Eases in from zero so the start of the bend leaves no visible line inside the pane.
    float bevel = t * t * t;
    float2 g = float2(
        sdRoundRect(p + float2(1.0, 0.0), halfSize, r) - sdRoundRect(p - float2(1.0, 0.0), halfSize, r),
        sdRoundRect(p + float2(0.0, 1.0), halfSize, r) - sdRoundRect(p - float2(0.0, 1.0), halfSize, r));
    float len = length(g);
    float2 n = len > 0.0 ? g / len : float2(0.0);
    float2 off = n * bevel * strength;
    half4 c = content.eval(coord + off);
    half red = content.eval(coord + off * (1.0 + dispersion)).r;
    half blue = content.eval(coord + off * (1.0 - dispersion)).b;
    return half4(red, c.g, blue, c.a);
}
"""
