package com.hibiki.ui.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hibiki.ui.components.AppPageOverlayActions
import com.hibiki.ui.components.CyberLaunchButton
import com.hibiki.ui.components.CyberLaunchButtonStyle
import com.hibiki.ui.components.StoneBackground
import com.hibiki.ui.theme.Cyberpunk

@Composable
fun HomeScreen(
    onStartOverlay: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenContexts: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var archiveDismissed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                archiveDismissed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    StoneBackground(scrimAlpha = 0.5f) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val maxWidthDp = with(density) { constraints.maxWidth.toDp() }
            val maxHeightDp = with(density) { constraints.maxHeight.toDp() }
            val mainRing = minOf(280.dp, maxWidthDp * 0.78f, maxHeightDp * 0.46f)
            val scale = mainRing.value / 280f
            val archiveRing = (70f * scale).dp
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
            val archiveStyle = CyberLaunchButtonStyle(
                ringSize = archiveRing,
                primaryColor = Cyberpunk.NeonCyan,
                spectrum = Cyberpunk.Spectrum,
                arcColor = Cyberpunk.NeonCyan,
                labelFontSize = (30f * scale).sp,
                labelColor = Cyberpunk.NeonCyan,
                dismissOnLaunch = true,
            )
            val lift = maxHeightDp * 0.08f
            val buttonGap = maxOf(56.dp, maxHeightDp * 0.08f)
            val archiveOffset = mainRing / 2 + archiveRing / 2 + buttonGap - lift

            CyberLaunchButton(
                label = "響",
                style = mainStyle,
                onLaunch = onStartOverlay,
                modifier = Modifier.offset(y = -lift),
            )

            if (!archiveDismissed) {
                CyberLaunchButton(
                    label = "基",
                    style = archiveStyle,
                    onLaunch = {
                        archiveDismissed = true
                        onOpenArchive()
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = archiveOffset),
                )
            }

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
}
