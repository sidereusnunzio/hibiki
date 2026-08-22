package com.hibiki.ui.boot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.R
import com.hibiki.ui.components.SpectrumGlowFrame
import com.hibiki.ui.components.SpectrumGlowText
import com.hibiki.ui.components.StoneBackground
import com.hibiki.ui.components.rememberSpectrumGlowLoop
import com.hibiki.ui.theme.Cyberpunk
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * Schermata di avvio: pietra + «接続中…» in basso a destra (come Arashi).
 * Il tempo già trascorso conta verso [holdMillis].
 */
@Composable
fun ConnectingScreen(
    onFinished: () -> Unit,
    holdMillis: Long = 1_400L,
) {
    val startedAtNs = remember { System.nanoTime() }

    LaunchedEffect(Unit) {
        val elapsedMs = (System.nanoTime() - startedAtNs) / 1_000_000L
        val remaining = max(0L, holdMillis - elapsedMs)
        if (remaining > 0L) delay(remaining)
        onFinished()
    }

    val glow = rememberSpectrumGlowLoop()

    StoneBackground(scrimAlpha = 0.5f) {
        Box(modifier = Modifier.fillMaxSize()) {
            SpectrumGlowText(
                text = stringResource(R.string.connecting_ja),
                frame = SpectrumGlowFrame(glow.transitionT, glow.intensity),
                fontSize = 14.sp,
                haloSize = 72.dp,
                textColor = Cyberpunk.TextMuted,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
            )
        }
    }
}
