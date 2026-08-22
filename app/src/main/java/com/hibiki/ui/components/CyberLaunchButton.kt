package com.hibiki.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.hibiki.ui.theme.Cyberpunk
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CyberLaunchButtonStyle(
    val ringSize: Dp,
    val primaryColor: Color,
    val spectrum: List<Color>,
    val arcColor: Color,
    val labelFontSize: TextUnit,
    val labelColor: Color,
    val dismissOnLaunch: Boolean,
)

private const val LaunchScaleDurationMs = 300
private const val LaunchFadeDelayMs = 180
private const val LaunchFadeDurationMs = 420
private const val LaunchNavEnterDurationMs = 280
private val LaunchNavigateDelayMs: Long =
    (LaunchFadeDelayMs + LaunchFadeDurationMs - LaunchNavEnterDurationMs).toLong()

@Composable
fun CyberLaunchButton(
    label: String,
    style: CyberLaunchButtonStyle,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var launching by remember { mutableStateOf(false) }
    val launchScale = remember { Animatable(1f) }
    val launchGlow = remember { Animatable(0f) }
    val launchAlpha = remember { Animatable(1f) }
    val maxLaunchScale = 1.2f

    fun resetVisuals() {
        scope.launch {
            launchScale.snapTo(1f)
            launchGlow.snapTo(0f)
            launchAlpha.snapTo(1f)
            launching = false
        }
    }

    fun triggerLaunch() {
        if (launching) return
        launching = true
        scope.launch {
            launch {
                launchScale.animateTo(
                    targetValue = maxLaunchScale,
                    animationSpec = tween(LaunchScaleDurationMs, easing = FastOutSlowInEasing),
                )
            }
            launch {
                launchGlow.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(LaunchScaleDurationMs, easing = FastOutSlowInEasing),
                )
            }
            if (style.dismissOnLaunch) {
                launch {
                    launchAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = LaunchFadeDurationMs,
                            delayMillis = LaunchFadeDelayMs,
                            easing = LinearEasing,
                        ),
                    )
                }
                delay(LaunchNavigateDelayMs)
                onLaunch()
            } else {
                delay(LaunchScaleDurationMs.toLong())
                onLaunch()
                resetVisuals()
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "launchRing")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringRotation",
    )
    val outerPulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "outerPulse",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val scaleFactor = style.ringSize.value / 280f
    val innerSize = style.ringSize * (200f / 280f)
    val labelColor = lerp(style.labelColor, Cyberpunk.TextPrimary, launchGlow.value * 0.55f)
    val ringBoost = glowAlpha + launchGlow.value * 0.45f

    var glitching by remember { mutableStateOf(false) }
    var glitchJitterX by remember { mutableFloatStateOf(0f) }
    var glitchJitterY by remember { mutableFloatStateOf(0f) }
    var glitchSplit by remember { mutableFloatStateOf(0f) }
    var glitchSliceY by remember { mutableFloatStateOf(0.45f) }
    var glitchSliceShift by remember { mutableFloatStateOf(0f) }
    val glitchSecondary = if (style.primaryColor == Cyberpunk.NeonCyan) {
        Cyberpunk.NeonMagenta
    } else {
        Cyberpunk.NeonCyan
    }
    val glitchJitterRange = (7f * scaleFactor).coerceIn(3f, 7f)
    val glitchSplitRange = (8f * scaleFactor).coerceIn(3f, 8f)

    fun applyLaunchGlitchTick(random: Random, intensity: Float) {
        val jitter = glitchJitterRange * intensity
        val split = glitchSplitRange * intensity
        glitchJitterX = random.nextFloat() * jitter * 2f - jitter
        glitchJitterY = random.nextFloat() * jitter - jitter * 0.5f
        glitchSplit = random.nextFloat() * split + split * 0.35f
        glitchSliceY = random.nextFloat()
        glitchSliceShift = random.nextFloat() * jitter * 2f - jitter
        glitching = true
    }

    fun clearLaunchGlitch() {
        glitching = false
        glitchJitterX = 0f
        glitchJitterY = 0f
        glitchSplit = 0f
        glitchSliceShift = 0f
    }

    LaunchedEffect(launching) {
        val random = Random.Default
        if (launching) {
            val untilMs = LaunchScaleDurationMs + LaunchFadeDelayMs
            var elapsed = 0L
            while (isActive && elapsed < untilMs) {
                applyLaunchGlitchTick(random, 1.15f)
                val step = random.nextLong(16L, 28L)
                delay(step)
                elapsed += step
            }
            clearLaunchGlitch()
            return@LaunchedEffect
        }
        delay(random.nextLong(400L, 2_000L))
        while (isActive) {
            delay(random.nextLong(2_200L, 6_800L))
            val burstSteps = random.nextInt(3, 7)
            repeat(burstSteps) {
                applyLaunchGlitchTick(random, 0.9f)
                delay(random.nextLong(18L, 34L))
            }
            clearLaunchGlitch()
            if (random.nextFloat() < 0.32f) {
                delay(random.nextLong(70L, 160L))
                repeat(random.nextInt(2, 4)) {
                    applyLaunchGlitchTick(random, 1f)
                    delay(random.nextLong(16L, 26L))
                }
                clearLaunchGlitch()
            }
        }
    }

    val maxOuterPulse = 1.08f
    val layerSize = style.ringSize * (maxOuterPulse + 0.08f)
    val frameSize = layerSize * maxLaunchScale
    val outerStrokeBase = (10f * scaleFactor).dp
    val outerStrokeBoost = (8f * scaleFactor).dp
    val innerStrokeBase = (5f * scaleFactor).dp
    val innerStrokeBoost = (3f * scaleFactor).dp
    val outerRadiusInset = (8f * scaleFactor).dp
    val innerRadiusInset = (18f * scaleFactor).dp
    val glowBlurBase = 36f * scaleFactor
    val glowBlurBoost = 40f * scaleFactor
    val density = LocalDensity.current.density

    Box(
        modifier = modifier.size(frameSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(layerSize)
                .graphicsLayer {
                    scaleX = launchScale.value
                    scaleY = launchScale.value
                    alpha = launchAlpha.value
                    translationX = glitchJitterX * density
                    translationY = glitchJitterY * density
                    clip = false
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(style.ringSize),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = outerPulse
                            scaleY = outerPulse
                            clip = false
                        },
                ) {
                    drawCircle(
                        color = Cyberpunk.withAlpha(style.primaryColor, ringBoost * 0.4f),
                        radius = size.minDimension / 2f - outerRadiusInset.toPx(),
                        style = Stroke(width = (outerStrokeBase + outerStrokeBoost * launchGlow.value).toPx()),
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotation + glitchJitterX * 0.35f },
                ) {
                    val stroke = (innerStrokeBase + innerStrokeBoost * launchGlow.value).toPx()
                    val radius = size.minDimension / 2f - innerRadiusInset.toPx()
                    drawCircle(
                        brush = Brush.sweepGradient(colors = style.spectrum),
                        radius = radius,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = Cyberpunk.withAlpha(style.arcColor, 0.9f),
                        startAngle = -20f,
                        sweepAngle = 50f,
                        useCenter = false,
                        style = Stroke(width = stroke * 1.4f, cap = StrokeCap.Round),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2f,
                            (size.height - radius * 2) / 2f,
                        ),
                        size = Size(radius * 2, radius * 2),
                    )
                }
                if (glitching) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    ) {
                        val bandH = (size.height * 0.055f).coerceIn(2.4f, 10f * scaleFactor)
                        fun drawTear(y: Float, shift: Float) {
                            drawRect(
                                color = Cyberpunk.withAlpha(style.primaryColor, 0.42f),
                                topLeft = Offset(shift, y),
                                size = Size(size.width, bandH * 0.5f),
                            )
                            drawRect(
                                color = Cyberpunk.withAlpha(glitchSecondary, 0.36f),
                                topLeft = Offset(-shift * 0.8f, y + bandH * 0.45f),
                                size = Size(size.width, bandH * 0.4f),
                            )
                        }
                        drawTear(size.height * glitchSliceY, glitchSliceShift)
                        drawTear(
                            size.height * ((glitchSliceY + 0.41f) % 1f),
                            -glitchSliceShift * 0.7f,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape)
                        .background(
                            Cyberpunk.withAlpha(
                                Cyberpunk.Void,
                                0.35f - launchGlow.value * 0.1f,
                            ),
                            CircleShape,
                        )
                        .clickable(
                            enabled = !launching,
                            role = Role.Button,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = ::triggerLaunch,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (glitching) {
                        Text(
                            text = label,
                            fontSize = style.labelFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Cyberpunk.withAlpha(Cyberpunk.NeonMagenta, 0.55f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(x = (-glitchSplit).dp),
                        )
                        Text(
                            text = label,
                            fontSize = style.labelFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.55f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(x = glitchSplit.dp),
                        )
                    }
                    Text(
                        text = label,
                        fontSize = style.labelFontSize,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = Cyberpunk.withAlpha(
                                    style.labelColor,
                                    0.55f + launchGlow.value * 0.4f,
                                ),
                                offset = Offset.Zero,
                                blurRadius = glowBlurBase + launchGlow.value * glowBlurBoost,
                            ),
                        ),
                    )
                }
            }
        }
    }
}
