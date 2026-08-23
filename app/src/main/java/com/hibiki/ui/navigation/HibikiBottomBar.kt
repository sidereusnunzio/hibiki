package com.hibiki.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.ui.components.NavKanjiHaloSize
import com.hibiki.ui.components.SpectrumGlowFrame
import com.hibiki.ui.components.SpectrumGlowGlyph
import com.hibiki.ui.components.TabGlowSaturation
import com.hibiki.ui.components.rememberTabSpectrumGlow
import com.hibiki.ui.theme.Cyberpunk

data class BottomDest(
    val graphRoute: String,
    val startRoute: String,
    val label: String,
    val iconKanji: String,
)

private val IndicatorShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
private const val IndicatorSlideMs = 420

private data class TabVisualState(
    val isActive: Boolean,
    val glowIntensity: Float,
)

private fun tabVisualState(
    index: Int,
    selectedIndex: Int,
    glowTargetIndex: Int,
    isPending: Boolean,
    glowIntensity: Float,
): TabVisualState {
    val isGlowing = isPending && index == glowTargetIndex
    val isActive = when {
        isGlowing -> true
        isPending && index == selectedIndex -> false
        index == selectedIndex -> true
        else -> false
    }
    return TabVisualState(
        isActive = isActive,
        glowIntensity = if (isGlowing) glowIntensity else 0f,
    )
}

@Composable
fun HibikiBottomBar(
    destinations: List<BottomDest>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabGlow = rememberTabSpectrumGlow()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Cyberpunk.PanelTranslucent)
            .drawBehind {
                drawLine(
                    color = Cyberpunk.GridLine,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
            },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            val tabWidth = maxWidth / destinations.size.coerceAtLeast(1)

            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = tween(durationMillis = IndicatorSlideMs, easing = FastOutSlowInEasing),
                label = "navIndicator",
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(3.dp)
                    .clip(IndicatorShape)
                    .background(Brush.horizontalGradient(colors = Cyberpunk.MagentaLime)),
            )

            Row(modifier = Modifier.fillMaxSize()) {
                destinations.forEachIndexed { index, dest ->
                    val visual = tabVisualState(
                        index = index,
                        selectedIndex = selectedIndex,
                        glowTargetIndex = tabGlow.targetIndex,
                        isPending = tabGlow.isPending,
                        glowIntensity = tabGlow.intensity,
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                role = Role.Tab
                                contentDescription = dest.label
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (index != selectedIndex && index != tabGlow.targetIndex) {
                                        tabGlow.trigger(index)
                                    }
                                    onSelect(index)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        SpectrumGlowGlyph(
                            text = dest.iconKanji,
                            frame = SpectrumGlowFrame(
                                transitionT = tabGlow.transitionT,
                                intensity = visual.glowIntensity,
                            ),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                            showWhiteWhenIdle = visual.isActive,
                            inactiveStyle = TextStyle(color = Cyberpunk.TextMuted),
                            haloSize = NavKanjiHaloSize,
                            saturation = TabGlowSaturation,
                        )
                    }
                }
            }
        }
    }
}
