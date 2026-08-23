package com.hibiki.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.hibiki.ui.theme.Cyberpunk
import kotlin.math.sin
import kotlinx.coroutines.launch

private const val MinIntensity = 0.01f
private const val PulseCycles = 2f

private object GlowMetrics {
    const val InnerAlphaBase = 0.22f
    const val InnerAlphaPulse = 0.38f
    const val OuterAlphaBase = 0.08f
    const val OuterAlphaPulse = 0.12f
    const val RadiusBase = 0.6f
    const val RadiusIntensity = 0.25f
    const val RadiusPulse = 0.35f
    const val ShadowAlphaBase = 0.4f
    const val ShadowAlphaPulse = 0.5f
    const val BlurBase = 6f
    const val BlurIntensity = 8f
    const val BlurPulse = 20f
}

const val KanjiGlowCycleMs = 2_400
const val TabGlowCycleMs = 1_850
const val TabGlowSaturation = 1.65f
val NavKanjiHaloSize = 44.dp

@Immutable
data class SpectrumGlowFrame(
    val transitionT: Float,
    val intensity: Float,
)

@Immutable
data class SpectrumGlowLoop(
    val transitionT: Float,
    val intensity: Float,
)

@Immutable
data class TabSpectrumGlow(
    val transitionT: Float,
    val intensity: Float,
    val targetIndex: Int,
    val isPending: Boolean,
    val trigger: (Int) -> Unit,
)

private data class GlowRender(
    val color: Color,
    val innerAlpha: Float,
    val outerAlpha: Float,
    val radiusScale: Float,
    val shadowAlpha: Float,
    val blurRadius: Float,
)

fun spectrumColor(t: Float): Color {
    val spectrum = Cyberpunk.Spectrum
    val segments = spectrum.size - 1
    val pos = (t.coerceIn(0f, 1f) * segments).coerceIn(0f, segments.toFloat())
    val index = pos.toInt().coerceIn(0, segments - 1)
    val frac = pos - index
    return lerp(spectrum[index], spectrum[index + 1], frac)
}

private fun envelopePulse(t: Float): Float =
    0.42f + 0.58f * sin(t.coerceIn(0f, 1f) * Math.PI.toFloat() * PulseCycles)

private fun visualPulse(t: Float): Float =
    0.5f + 0.5f * sin(t.coerceIn(0f, 1f) * Math.PI.toFloat() * PulseCycles)

fun spectrumTabGlowEnvelope(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val rampIn = 1f - (1f - x) * (1f - x) * (1f - x) * 0.12f
    val fadeOut = 1f - smoothstep(0.58f, 1f, x)
    return rampIn * fadeOut * envelopePulse(x)
}

fun spectrumLoopGlowIntensity(t: Float): Float =
    0.58f + 0.42f * envelopePulse(t)

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun glowRender(
    transitionT: Float,
    intensity: Float,
    saturation: Float = 1f,
): GlowRender? {
    if (intensity <= MinIntensity) return null

    val pulse = visualPulse(transitionT)
    return GlowRender(
        color = spectrumColor(transitionT),
        innerAlpha = (intensity * saturation * (GlowMetrics.InnerAlphaBase + pulse * GlowMetrics.InnerAlphaPulse))
            .coerceIn(0f, 1f),
        outerAlpha = (intensity * saturation * (GlowMetrics.OuterAlphaBase + pulse * GlowMetrics.OuterAlphaPulse))
            .coerceIn(0f, 1f),
        radiusScale = GlowMetrics.RadiusBase + intensity * (GlowMetrics.RadiusIntensity + pulse * GlowMetrics.RadiusPulse),
        shadowAlpha = (intensity * saturation * (GlowMetrics.ShadowAlphaBase + pulse * GlowMetrics.ShadowAlphaPulse))
            .coerceIn(0f, 1f),
        blurRadius = GlowMetrics.BlurBase + intensity * (GlowMetrics.BlurIntensity + pulse * GlowMetrics.BlurPulse),
    )
}

private fun glowTextStyle(
    render: GlowRender,
    textColor: Color = Cyberpunk.TextPrimary,
): TextStyle =
    TextStyle(
        color = textColor,
        shadow = Shadow(
            color = Cyberpunk.withAlpha(render.color, render.shadowAlpha),
            offset = Offset.Zero,
            blurRadius = render.blurRadius,
        ),
    )

@Composable
fun rememberSpectrumGlowLoop(cycleMs: Int = KanjiGlowCycleMs): SpectrumGlowLoop {
    val transition = rememberInfiniteTransition(label = "spectrumGlowLoop")
    val transitionT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spectrumGlowT",
    )
    return SpectrumGlowLoop(
        transitionT = transitionT,
        intensity = spectrumLoopGlowIntensity(transitionT),
    )
}

@Composable
fun rememberTabSpectrumGlow(): TabSpectrumGlow {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var targetIndex by remember { mutableIntStateOf(-1) }

    val trigger: (Int) -> Unit = remember(scope, progress) {
        { index ->
            targetIndex = index
            scope.launch {
                progress.stop()
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = TabGlowCycleMs, easing = LinearEasing),
                )
                targetIndex = -1
                progress.snapTo(0f)
            }
        }
    }

    val transitionT = progress.value
    return TabSpectrumGlow(
        transitionT = transitionT,
        intensity = spectrumTabGlowEnvelope(transitionT),
        targetIndex = targetIndex,
        isPending = targetIndex >= 0,
        trigger = trigger,
    )
}

@Composable
private fun GlowHaloCanvas(
    render: GlowRender,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f * render.radiusScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Cyberpunk.withAlpha(render.color, render.innerAlpha),
                    Cyberpunk.withAlpha(render.color, render.outerAlpha),
                    Cyberpunk.Transparent,
                ),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

@Composable
fun SpectrumGlowGlyph(
    text: String,
    frame: SpectrumGlowFrame,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    inactiveStyle: TextStyle? = null,
    showWhiteWhenIdle: Boolean = false,
    textColor: Color = Cyberpunk.TextPrimary,
    idleTextColor: Color = textColor,
    haloSize: Dp = NavKanjiHaloSize,
    saturation: Float = 1f,
) {
    val render = glowRender(frame.transitionT, frame.intensity, saturation)
    val style = when {
        render != null -> glowTextStyle(render, textColor)
        showWhiteWhenIdle -> TextStyle(color = idleTextColor)
        inactiveStyle != null -> inactiveStyle
        else -> TextStyle(color = idleTextColor)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (render != null) {
            GlowHaloCanvas(
                render = render,
                modifier = Modifier.size(haloSize),
            )
        }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
            style = style,
        )
    }
}

@Composable
fun SpectrumGlowText(
    text: String,
    frame: SpectrumGlowFrame,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    haloSize: Dp = NavKanjiHaloSize,
    textColor: Color = Cyberpunk.TextPrimary,
) {
    SpectrumGlowGlyph(
        text = text,
        frame = frame,
        modifier = modifier,
        fontSize = fontSize,
        haloSize = haloSize,
        textColor = textColor,
        idleTextColor = textColor,
    )
}
