package com.hibiki.ui.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.ui.components.AppPageOverlayActions
import com.hibiki.ui.components.CyberLaunchButton
import com.hibiki.ui.components.CyberLaunchButtonStyle
import com.hibiki.ui.theme.Cyberpunk

@Composable
fun HomeScreen(
    onStartOverlay: () -> Unit,
    onOpenContexts: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val maxWidthDp = with(density) { constraints.maxWidth.toDp() }
        val maxHeightDp = with(density) { constraints.maxHeight.toDp() }
        val mainRing = minOf(280.dp, maxWidthDp * 0.78f, maxHeightDp * 0.46f)
        val scale = mainRing.value / 280f
        val mainStyle = CyberLaunchButtonStyle(
            ringSize = mainRing,
            primaryColor = Cyberpunk.NeonMagenta,
            spectrum = listOf(
                Cyberpunk.NeonMagenta,
                Cyberpunk.NeonViolet,
                Cyberpunk.NeonCyan,
                Cyberpunk.NeonMagenta,
            ),
            arcColor = Cyberpunk.NeonMagenta,
            labelFontSize = (120f * scale).sp,
            labelColor = Cyberpunk.NeonMagenta,
            dismissOnLaunch = false,
        )
        val drop = maxHeightDp * 0.04f

        CyberLaunchButton(
            label = "響",
            style = mainStyle,
            onLaunch = onStartOverlay,
            modifier = Modifier.offset(y = drop),
        )

        AppPageOverlayActions(
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            HomeActionsMenu(
                onOpenContexts = onOpenContexts,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}
