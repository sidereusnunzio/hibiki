package com.hibiki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hibiki.ui.theme.Cyberpunk

private val CardShape = RoundedCornerShape(4.dp)

@Composable
fun HibikiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Cyberpunk.PanelTranslucent, CardShape)
            .border(1.dp, Cyberpunk.GridLine, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
fun StatusChip(text: String, ok: Boolean) {
    Text(
        text = text.uppercase(),
        color = if (ok) Cyberpunk.NeonLime else Cyberpunk.NeonMagenta,
        modifier = Modifier
            .background(
                Cyberpunk.withAlpha(if (ok) Cyberpunk.NeonLime else Cyberpunk.NeonMagenta, 0.12f),
                RoundedCornerShape(2.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
