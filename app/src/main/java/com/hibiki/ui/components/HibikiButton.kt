package com.hibiki.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.CyberpunkButtonDefaults

private val HibikiButtonShape = RoundedCornerShape(2.dp)

fun contrastingContentColor(containerColor: Color): Color =
    if (containerColor.luminance() > 0.5f) Cyberpunk.Void else Cyberpunk.TextPrimary

data class HibikiButtonColors(
    val containerColor: Color,
    val contentColor: Color,
)

object HibikiButtonStyles {
    val Primary = HibikiButtonColors(Cyberpunk.NeonCyan, contrastingContentColor(Cyberpunk.NeonCyan))
    val Secondary = HibikiButtonColors(Cyberpunk.PanelElevated, Cyberpunk.NeonCyan)
    val Destructive = HibikiButtonColors(Cyberpunk.NeonMagenta, Cyberpunk.Void)
    val Violet = HibikiButtonColors(Cyberpunk.NeonViolet, Cyberpunk.Void)
    val Lime = HibikiButtonColors(Cyberpunk.NeonLime, Cyberpunk.Void)
    val Magenta = HibikiButtonColors(Cyberpunk.PanelElevated, Cyberpunk.NeonMagenta)
    val Cancel = HibikiButtonColors(Cyberpunk.PanelElevated, Cyberpunk.TextMuted)
}

@Composable
fun HibikiButton(
    text: String,
    onClick: () -> Unit,
    style: HibikiButtonColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    disabledContainerColor: Color = CyberpunkButtonDefaults.disabledContainerColor,
    disabledContentColor: Color = CyberpunkButtonDefaults.disabledContentColor,
    fillMaxWidth: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier,
        shape = HibikiButtonShape,
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = style.containerColor,
            contentColor = style.contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = style.contentColor,
            )
        } else {
            Text(text)
        }
    }
}
