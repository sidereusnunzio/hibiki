package com.hibiki.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.domain.model.CaptureResult
import com.hibiki.domain.model.PhraseSource
import com.hibiki.ui.theme.Cyberpunk

@Composable
fun OverlayPhrasePanel(
    result: CaptureResult,
    onDrag: (dx: Int, dy: Int) -> Unit,
    onClose: () -> Unit,
    onPlay: () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    val phrase = result.phrase
    val japanese = phrase.japaneseDisplay
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cyberpunk.withAlpha(Cyberpunk.Void, 0.96f), shape)
            .border(1.dp, Cyberpunk.GridLine, shape)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Sposta",
                tint = Cyberpunk.MutedCyan,
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                        }
                    },
            )
            Spacer(modifier = Modifier.weight(1f))
            CaptureOriginDots(
                origin = result.origin,
                modifier = Modifier.padding(end = if (phrase.audioPath != null) 2.dp else 0.dp),
            )
            if (phrase.audioPath != null) {
                IconButton(onClick = onPlay, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Riproduci",
                        tint = Cyberpunk.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Chiudi frase",
                    tint = Cyberpunk.TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = japanese.ifBlank { "—" },
                style = phraseHeadlineStyle(japanese),
                color = Cyberpunk.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (phrase.kana.isNotBlank()) {
                Text(
                    text = phrase.kana,
                    style = MaterialTheme.typography.titleMedium,
                    color = Cyberpunk.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (phrase.romaji.isNotBlank()) {
                Text(
                    text = phrase.romaji,
                    style = MaterialTheme.typography.bodySmall,
                    color = Cyberpunk.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (phrase.literalTranslation.isNotBlank()) {
                Text(
                    text = phrase.literalTranslation,
                    color = Cyberpunk.MutedLime,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
            if (phrase.naturalTranslation.isNotBlank()) {
                Text(
                    text = phrase.naturalTranslation,
                    color = Cyberpunk.NeonLime,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun phraseHeadlineStyle(japanese: String): TextStyle {
    val base = when (japanese.length) {
        in 0..12 -> MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, letterSpacing = 0.sp)
        in 13..24 -> MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, letterSpacing = 0.sp)
        in 25..40 -> MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, letterSpacing = 0.sp)
        else -> MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, letterSpacing = 0.sp)
    }
    return base.copy(lineHeight = base.fontSize * 1.15f)
}

/** Due pallini: 1° = audio/API, 2° = archivio. */
@Composable
private fun CaptureOriginDots(origin: PhraseSource, modifier: Modifier = Modifier) {
    val (first, second) = when (origin) {
        PhraseSource.LOCAL_MATCH -> Cyberpunk.NeonCyan to Cyberpunk.NeonCyan
        PhraseSource.TEXT_MATCH_AFTER_TRANSCRIPTION -> Cyberpunk.NeonViolet to Cyberpunk.NeonCyan
        PhraseSource.API -> Cyberpunk.NeonViolet to Cyberpunk.NeonViolet
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OriginDot(first)
        OriginDot(second)
    }
}

@Composable
private fun OriginDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}
