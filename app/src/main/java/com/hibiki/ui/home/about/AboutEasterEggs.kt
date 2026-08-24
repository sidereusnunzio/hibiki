package com.hibiki.ui.home.about

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.R
import com.hibiki.ui.theme.Cyberpunk
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class TerminalLineKind { Output, Error, Dim }

private data class TerminalLine(
    val text: String,
    val kind: TerminalLineKind = TerminalLineKind.Output,
    val showCursor: Boolean = false,
)

private val PreludeCommands = listOf(
    "user --status" to "listening",
    "phrases --count" to "never enough",
    "chocolate" to "required",
    "overlay --status" to "unstable",
)

private fun runningStatus(tick: Int): String {
    val dots = ".".repeat((tick % 3) + 1).padEnd(3)
    return "running$dots  [eta unknown]"
}

private suspend fun MutableList<TerminalLine>.typeCommand(command: String) {
    add(TerminalLine(text = "", showCursor = true))
    val index = lastIndex
    for (i in command.indices) {
        this[index] = TerminalLine(text = command.take(i + 1), showCursor = true)
        delay(if (i < 2) 90L else 48L)
    }
}

private fun MutableList<TerminalLine>.hideTypingCursor() {
    val index = indices.lastOrNull { this[it].showCursor } ?: return
    this[index] = this[index].copy(showCursor = false)
}

@Composable
fun AboutDeveloperTerminal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledLabel = stringResource(R.string.about_dev_mode_enabled)
    val lines = remember(enabledLabel) {
        mutableStateListOf(TerminalLine(enabledLabel))
    }
    var abortFailed by remember { mutableStateOf(false) }
    var showCursor by remember { mutableStateOf(false) }
    var abortFlashKey by remember { mutableIntStateOf(0) }
    val panelShape = RoundedCornerShape(8.dp)
    val borderColor by animateColorAsState(
        targetValue = if (abortFailed) {
            Cyberpunk.withAlpha(Cyberpunk.NeonMagenta, 0.75f)
        } else {
            Cyberpunk.withAlpha(Cyberpunk.NeonLime, 0.55f)
        },
        animationSpec = tween(durationMillis = 220),
        label = "terminalBorder",
    )

    LaunchedEffect(Unit) {
        delay(600)
        for ((cmd, result) in PreludeCommands) {
            lines.typeCommand("> $cmd")
            delay(380)
            lines.hideTypingCursor()
            lines.add(TerminalLine(result))
            delay(520)
        }

        lines.typeCommand("> sudo capture audio")
        delay(380)
        lines.hideTypingCursor()
        lines.add(TerminalLine(runningStatus(0), TerminalLineKind.Dim))
        val runningIndex = lines.lastIndex
        launch {
            var tick = 1
            while (isActive) {
                delay(280)
                lines[runningIndex] = TerminalLine(runningStatus(tick), TerminalLineKind.Dim)
                tick++
            }
        }

        delay(2100)

        lines.typeCommand("> disconnect")
        delay(260)
        lines.hideTypingCursor()
        lines.add(TerminalLine("refused", TerminalLineKind.Error))
        abortFailed = true
        abortFlashKey += 1
        delay(180)
        lines.add(TerminalLine("EPERM: operation cannot be interrupted", TerminalLineKind.Error))
        delay(160)
        lines.add(TerminalLine("kernel: abort ignored — capture still listening", TerminalLineKind.Error))
        abortFlashKey += 1
        showCursor = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Cyberpunk.withAlpha(Cyberpunk.Void, 0.82f))
            .clickable(onClick = onDismiss)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(panelShape)
                .border(width = 1.dp, color = borderColor, shape = panelShape)
                .background(Cyberpunk.PanelElevated, panelShape)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_terminal_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (abortFailed) Cyberpunk.NeonMagenta else Cyberpunk.NeonLime,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(6.dp))
                lines.forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (line.kind) {
                                TerminalLineKind.Output -> Cyberpunk.TextPrimary
                                TerminalLineKind.Error -> Cyberpunk.NeonMagenta
                                TerminalLineKind.Dim -> Cyberpunk.MutedLime
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                        if (line.showCursor) {
                            AboutBlinkingCursor()
                        }
                    }
                }
                if (showCursor) {
                    AboutBlinkingCursor(modifier = Modifier.padding(top = 4.dp))
                }
            }
            AboutPanelTapFx(flashKey = abortFlashKey)
        }
    }
}
