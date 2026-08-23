package com.hibiki.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

object HibikiMotion {
    val Easing = FastOutSlowInEasing

    const val NavEnterMs = 300
    const val NavExitMs = 220
    const val ContentCrossfadeMs = 220
    const val TabCrossfadeMs = 150
    const val SegmentedTabMs = 260

    fun contentCrossfadeTween() = tween<Float>(durationMillis = ContentCrossfadeMs, easing = Easing)
}
