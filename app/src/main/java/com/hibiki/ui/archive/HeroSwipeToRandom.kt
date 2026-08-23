package com.hibiki.ui.archive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.ui.theme.Cyberpunk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal val HeroSwipeToRandomMinDistance = 40.dp
internal val HeroSwipeToRandomMaxDistance = 72.dp
internal val HeroCardPagerPageSpacing = 16.dp

private val HeroSwipeSettleSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * Wraps a vertically scrollable page so a downward pull at the top
 * moves the whole content, reveals a RANDOM hint, and opens a random item.
 *
 * Uses nested scroll so horizontal paging gestures stay with [HorizontalPager].
 */
@Composable
fun HeroSwipeToRandomContainer(
    scrollState: ScrollState? = null,
    lazyListState: LazyListState? = null,
    onRandom: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val randomAction = onRandom
    if (randomAction == null || (scrollState == null && lazyListState == null)) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val rawPullPx = remember { mutableFloatStateOf(0f) }
    val animationScope = rememberCoroutineScope()
    val minDistancePx = with(LocalDensity.current) { HeroSwipeToRandomMinDistance.toPx() }
    val maxOffsetPx = with(LocalDensity.current) { HeroSwipeToRandomMaxDistance.toPx() }
    val latestOnRandom = rememberUpdatedState(randomAction)
    val latestScrollState = rememberUpdatedState(scrollState)
    val latestLazyListState = rememberUpdatedState(lazyListState)
    val displayOffsetPx = heroSwipeToRandomVisualOffset(rawPullPx.floatValue, maxOffsetPx)
    val progress = heroSwipeRandomLabelAlpha(displayOffsetPx, minDistancePx)

    val connection = remember(minDistancePx, maxOffsetPx, animationScope) {
        var settleJob: Job? = null
        fun cancelSettle() {
            settleJob?.cancel()
            settleJob = null
        }
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val consumedY = heroSwipeConsumePreScrollY(
                    currentRawPullPx = rawPullPx.floatValue,
                    availableY = available.y,
                )
                if (consumedY == 0f) return Offset.Zero
                cancelSettle()
                rawPullPx.floatValue += consumedY
                return Offset(0f, consumedY)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val atTop = isHeroScrollAtTop(
                    latestScrollState.value,
                    latestLazyListState.value,
                )
                val consumedY = heroSwipeConsumePostScrollY(
                    atTop = atTop,
                    availableY = available.y,
                )
                if (consumedY == 0f) return Offset.Zero
                cancelSettle()
                rawPullPx.floatValue += consumedY
                return Offset(0f, consumedY)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (rawPullPx.floatValue <= 0f) return Velocity.Zero
                val shouldTrigger = shouldTriggerHeroSwipeToRandom(
                    atTop = true,
                    downwardDistancePx = rawPullPx.floatValue,
                    minDistancePx = minDistancePx,
                )
                if (shouldTrigger) {
                    cancelSettle()
                    latestOnRandom.value()
                    rawPullPx.floatValue = 0f
                } else {
                    settleJob = animationScope.launchSettleHeroSwipe(
                        from = rawPullPx.floatValue,
                        onFrame = { rawPullPx.floatValue = it },
                    )
                }
                return Velocity(0f, available.y)
            }
        }
    }

    Box(modifier = modifier.nestedScroll(connection)) {
        Text(
            text = "RANDOM",
            color = Cyberpunk.TextMuted,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .offset {
                    IntOffset(0, (displayOffsetPx * 0.35f).roundToInt())
                }
                .graphicsLayer {
                    alpha = progress
                    val scale = 0.92f + (0.08f * progress)
                    scaleX = scale
                    scaleY = scale
                },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, displayOffsetPx.roundToInt()) },
        ) {
            content()
        }
    }
}

private fun CoroutineScope.launchSettleHeroSwipe(
    from: Float,
    onFrame: (Float) -> Unit,
): Job = launch {
    val anim = Animatable(from)
    anim.animateTo(0f, HeroSwipeSettleSpring) {
        onFrame(value)
    }
    onFrame(0f)
}

internal fun heroSwipeConsumePostScrollY(
    atTop: Boolean,
    availableY: Float,
): Float = if (atTop && availableY > 0f) availableY else 0f

internal fun heroSwipeConsumePreScrollY(
    currentRawPullPx: Float,
    availableY: Float,
): Float {
    if (availableY >= 0f || currentRawPullPx <= 0f) return 0f
    return (currentRawPullPx + availableY).coerceAtLeast(0f) - currentRawPullPx
}

internal fun heroSwipeToRandomVisualOffset(
    rawDisplacementPx: Float,
    maxOffsetPx: Float,
): Float {
    val raw = rawDisplacementPx.coerceAtLeast(0f)
    if (raw <= maxOffsetPx) return raw
    val extra = raw - maxOffsetPx
    return maxOffsetPx + extra * 0.25f
}

internal fun heroSwipeRandomLabelAlpha(
    offsetPx: Float,
    minDistancePx: Float,
): Float {
    if (minDistancePx <= 0f) return 0f
    return (offsetPx / minDistancePx).coerceIn(0f, 1f)
}

internal fun shouldTriggerHeroSwipeToRandom(
    atTop: Boolean,
    downwardDistancePx: Float,
    minDistancePx: Float,
): Boolean = atTop && downwardDistancePx >= minDistancePx
