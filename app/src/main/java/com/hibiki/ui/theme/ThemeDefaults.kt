package com.hibiki.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CyberpunkButtonDefaults {
    val disabledContainerColor: Color =
        Cyberpunk.withAlpha(Cyberpunk.PanelElevated, 0.6f)

    val disabledContentColor: Color =
        Cyberpunk.withAlpha(Cyberpunk.TextMuted, 0.5f)
}

@Composable
fun cyberpunkOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Cyberpunk.TextPrimary,
    unfocusedTextColor = Cyberpunk.TextPrimary,
    disabledTextColor = Cyberpunk.withAlpha(Cyberpunk.TextMuted, 0.5f),
    focusedBorderColor = Cyberpunk.NeonCyan,
    unfocusedBorderColor = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.35f),
    disabledBorderColor = Cyberpunk.GridLine,
    disabledLabelColor = Cyberpunk.withAlpha(Cyberpunk.TextMuted, 0.45f),
    disabledTrailingIconColor = Cyberpunk.withAlpha(Cyberpunk.TextMuted, 0.45f),
    focusedLabelColor = Cyberpunk.NeonCyan,
    unfocusedLabelColor = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.35f),
    cursorColor = Cyberpunk.NeonCyan,
    focusedTrailingIconColor = Cyberpunk.TextMuted,
    unfocusedTrailingIconColor = Cyberpunk.TextMuted,
    focusedContainerColor = Cyberpunk.Panel,
    unfocusedContainerColor = Cyberpunk.Panel,
    disabledContainerColor = Cyberpunk.Deep,
)
