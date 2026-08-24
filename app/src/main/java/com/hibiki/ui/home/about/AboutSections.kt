package com.hibiki.ui.home.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.R
import com.hibiki.ui.theme.Cyberpunk
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay

private val PanelShape = RoundedCornerShape(8.dp)

@Composable
fun AboutPanel(
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var flashKey by remember { mutableIntStateOf(0) }
    var contentJitter by remember { mutableStateOf(0f) }

    LaunchedEffect(flashKey) {
        if (flashKey == 0) return@LaunchedEffect
        val random = Random.Default
        repeat(8) {
            contentJitter = random.nextInt(-8, 9).toFloat()
            delay(22)
        }
        contentJitter = 0f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(onTap) {
                detectTapGestures(
                    onTap = {
                        flashKey += 1
                        onTap?.invoke()
                    },
                )
            },
        shape = PanelShape,
        color = Cyberpunk.PanelElevated,
    ) {
        Box(
            modifier = Modifier
                .clip(PanelShape)
                .border(
                    width = 1.dp,
                    color = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.35f),
                    shape = PanelShape,
                ),
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer { translationX = contentJitter }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                content()
            }
            AboutPanelTapFx(
                flashKey = flashKey,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
fun AboutSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.ROOT),
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = Cyberpunk.NeonCyan,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
fun AboutStatusRow(
    label: String,
    value: String,
    barProgress: Float? = null,
    valueColor: Color = Cyberpunk.NeonLime,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Cyberpunk.TextMuted,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
        }
        if (barProgress != null) {
            Spacer(modifier = Modifier.height(4.dp))
            AboutProgressBar(progress = barProgress)
        }
    }
}

@Composable
fun AboutProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    oscillating: Boolean = false,
) {
    if (oscillating) {
        AboutOscillatingProgressBar(progress = progress, modifier = modifier)
    } else {
        AboutStaticProgressBar(progress = progress, modifier = modifier)
    }
}

@Composable
private fun AboutStaticProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animated.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        )
    }
    AboutProgressTrack(progress = animated.value, modifier = modifier)
}

@Composable
private fun AboutOscillatingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animated = remember { Animatable(0f) }
    val infinite = rememberInfiniteTransition(label = "aboutBarOsc")
    val osc by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutBarOscValue",
    )
    LaunchedEffect(progress) {
        animated.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        )
    }
    val base = animated.value.coerceAtLeast(0.94f)
    val shown = 0.94f + ((osc - 0.94f) / 0.06f) * (base - 0.94f).coerceAtLeast(0.02f)
    AboutProgressTrack(progress = shown, modifier = modifier)
}

@Composable
private fun AboutProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.12f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .background(Cyberpunk.NeonCyan),
        )
    }
}

@Composable
fun AboutAnimatedCount(
    target: Int,
    modifier: Modifier = Modifier,
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animated.snapTo(0f)
        animated.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.ITALIAN) }
    Text(
        text = formatter.format(animated.value.toInt()),
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        color = Cyberpunk.TextPrimary,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun AboutCounterCell(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelLarge,
            color = Cyberpunk.TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
        AboutAnimatedCount(target = value)
    }
}

@Composable
fun AboutTimelineLog(
    bootDate: String,
    firstPhraseDate: String,
    currentBuildDate: String,
    modifier: Modifier = Modifier,
) {
    var visibleLines by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(3) {
            delay(150)
            visibleLines = it + 1
        }
    }
    val lines = listOf(
        stringResource(R.string.about_timeline_boot, bootDate),
        stringResource(R.string.about_timeline_first_phrase, firstPhraseDate),
        stringResource(R.string.about_timeline_current_build, currentBuildDate),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.take(visibleLines).forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = Cyberpunk.TextPrimary,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
fun AboutPulseDot(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "aboutPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutPulseAlpha",
    )
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
                if (active) {
                    Cyberpunk.withAlpha(Cyberpunk.NeonLime, pulse)
                } else {
                    Cyberpunk.withAlpha(Cyberpunk.NeonMagenta, 0.5f)
                },
            ),
    )
}

@Composable
fun AboutDeviceHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Terminal,
            contentDescription = null,
            tint = Cyberpunk.NeonCyan,
            modifier = Modifier.size(16.dp),
        )
        AboutSectionLabel(text = stringResource(R.string.about_device_title))
    }
}

@Composable
fun AboutKeyValue(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = Cyberpunk.TextMuted,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Cyberpunk.TextPrimary,
            fontFamily = FontFamily.Monospace,
        )
    }
}
