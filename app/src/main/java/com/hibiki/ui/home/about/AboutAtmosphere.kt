package com.hibiki.ui.home.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.ui.theme.Cyberpunk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val HexLines = listOf(
    "4A 61 70 61 6E 65 73 65",
    "E9 9F BF 00 FF",
    "48 49 42 49 4B 49",
    "41 55 44 49 4F",
    "46 49 4E 47 45 52",
    "43 41 50 54 55 52 45",
)

/** Sfondo esadecimale lento — solo About. */
@Composable
fun AboutHexBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aboutHex")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aboutHexOffset",
    )
    val measurer = rememberTextMeasurer()
    val style = remember {
        TextStyle(
            color = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.07f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val lineHeight = 18.sp.toPx()
        val scroll = offset * lineHeight * HexLines.size
        var y = -lineHeight + (scroll % (lineHeight * HexLines.size))
        var index = 0
        while (y < size.height + lineHeight) {
            val line = HexLines[index % HexLines.size]
            val layout = measurer.measure(text = line, style = style)
            drawText(textLayoutResult = layout, topLeft = Offset(12f, y))
            y += lineHeight
            index++
        }
    }
}

/** CRT leggero: scanline statiche + vignette. */
@Composable
fun AboutCrtOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 3.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Cyberpunk.withAlpha(Cyberpunk.Void, 0.18f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += step
        }
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Cyberpunk.Transparent,
                    Cyberpunk.withAlpha(Cyberpunk.Void, 0.35f),
                ),
                center = center,
                radius = size.maxDimension * 0.72f,
            ),
        )
    }
}

@Composable
fun AboutScanlineOnce(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        finished = false
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 520, easing = LinearEasing))
        finished = true
    }
    if (finished) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val y = size.height * progress.value
        drawLine(
            color = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.55f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = Cyberpunk.withAlpha(Cyberpunk.NeonMagenta, 0.25f),
            start = Offset(0f, y + 2.dp.toPx()),
            end = Offset(size.width, y + 2.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

@Composable
fun rememberAboutBootGlitch(durationMs: Long = 500L): Boolean {
    var glitching by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(durationMs)
        glitching = false
    }
    return glitching
}

@Composable
fun AboutBlinkingCursor(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aboutCursor")
    val visible by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutCursorBlink",
    )
    Text(
        text = "█",
        modifier = modifier,
        color = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, visible),
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
    )
}

/**
 * Flash + glitch locali a un pannello About. [flashKey] > 0 fa partire l'effetto.
 * Se [holdEndFlash] è true, il flash resta acceso sul finale (utile se la UI viene
 * sostituita a metà animazione e non deve tornare “pulita”).
 */
@Composable
fun AboutPanelTapFx(
    flashKey: Int,
    modifier: Modifier = Modifier,
    holdEndFlash: Boolean = false,
) {
    val flashAlpha = remember { Animatable(0f) }
    val scanProgress = remember { Animatable(0f) }
    var jitterX by remember { mutableStateOf(0f) }
    var useMagenta by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(false) }

    LaunchedEffect(flashKey) {
        if (flashKey == 0) return@LaunchedEffect
        useMagenta = flashKey % 2 == 0
        active = true
        flashAlpha.snapTo(0.62f)
        scanProgress.snapTo(0f)
        val random = Random.Default
        val glitchJob = launch {
            repeat(8) {
                jitterX = random.nextInt(-10, 11).toFloat()
                delay(22)
            }
            jitterX = 0f
        }
        launch {
            scanProgress.animateTo(1f, animationSpec = tween(durationMillis = 160, easing = LinearEasing))
        }
        val endAlpha = if (holdEndFlash) 0.32f else 0f
        flashAlpha.animateTo(
            endAlpha,
            animationSpec = tween(durationMillis = 180, easing = LinearEasing),
        )
        glitchJob.join()
        if (!holdEndFlash) {
            active = false
        }
    }

    if (!active && flashAlpha.value <= 0.001f) return

    val accent = if (useMagenta) Cyberpunk.NeonMagenta else Cyberpunk.NeonCyan
    val secondary = if (useMagenta) Cyberpunk.NeonCyan else Cyberpunk.NeonMagenta

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationX = jitterX },
    ) {
        drawRect(color = Cyberpunk.withAlpha(accent, flashAlpha.value))
        drawRect(
            color = Cyberpunk.withAlpha(secondary, flashAlpha.value * 0.35f),
            topLeft = Offset(jitterX * 1.4f, 0f),
            size = size,
        )
        val y = size.height * scanProgress.value
        drawRect(
            color = Cyberpunk.withAlpha(Cyberpunk.TextPrimary, flashAlpha.value * 0.55f),
            topLeft = Offset(0f, y - 2.dp.toPx()),
            size = Size(size.width, 3.dp.toPx()),
        )
        drawLine(
            color = Cyberpunk.withAlpha(accent, flashAlpha.value.coerceAtLeast(0.2f)),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

@Composable
fun AboutLogoGlitchLayer(
    kanji: String,
    brand: String,
    glitching: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    var jitter by remember { mutableStateOf(0f) }
    LaunchedEffect(glitching) {
        if (!glitching) {
            jitter = 0f
            return@LaunchedEffect
        }
        val random = Random.Default
        while (true) {
            jitter = random.nextInt(-6, 7).toFloat()
            delay(40)
        }
    }
    val labelStyle = remember(style) {
        style.copy(
            letterSpacing = 0.sp,
            lineHeight = style.fontSize,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        )
    }
    fun brandText(color: androidx.compose.ui.graphics.Color) = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = color,
                fontSize = style.fontSize,
                fontWeight = style.fontWeight,
                letterSpacing = 0.sp,
            ),
        ) {
            append(kanji)
        }
        append("  ")
        withStyle(
            SpanStyle(
                color = color,
                fontSize = style.fontSize,
                fontWeight = style.fontWeight,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
            ),
        ) {
            append(brand)
        }
    }
    Box(modifier = modifier) {
        if (glitching) {
            Text(
                text = brandText(Cyberpunk.withAlpha(Cyberpunk.NeonMagenta, 0.55f)),
                style = labelStyle,
                modifier = Modifier.offset(x = (-jitter).dp),
            )
            Text(
                text = brandText(Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.55f)),
                style = labelStyle,
                modifier = Modifier.offset(x = jitter.dp),
            )
        }
        Text(
            text = brandText(style.color),
            style = labelStyle,
        )
    }
}
