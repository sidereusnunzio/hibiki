package com.hibiki.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hibiki.ui.theme.Cyberpunk

@Composable
fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Cyberpunk.PanelTranslucent,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Cyberpunk.NeonCyan,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun HibikiDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Cyberpunk.PanelTranslucent,
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun HibikiConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "CONFERMA",
    confirmStyle: HibikiButtonColors = HibikiButtonStyles.Violet,
    alternateConfirmLabel: String? = null,
    alternateConfirmStyle: HibikiButtonColors = HibikiButtonStyles.Violet,
    onAlternateConfirm: (() -> Unit)? = null,
) {
    HibikiDialog(onDismissRequest = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Cyberpunk.TextPrimary,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Cyberpunk.TextMuted,
        )
        if (alternateConfirmLabel != null && onAlternateConfirm != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HibikiButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = alternateConfirmLabel,
                    onClick = onAlternateConfirm,
                    style = alternateConfirmStyle,
                    fillMaxWidth = false,
                )
                HibikiButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = confirmLabel,
                    onClick = onConfirm,
                    style = confirmStyle,
                    fillMaxWidth = false,
                )
                HibikiButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "ANNULLA",
                    onClick = onDismiss,
                    style = HibikiButtonStyles.Cancel,
                    fillMaxWidth = false,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HibikiButton(
                    modifier = Modifier.weight(1f),
                    text = "ANNULLA",
                    onClick = onDismiss,
                    style = HibikiButtonStyles.Cancel,
                    fillMaxWidth = false,
                )
                HibikiButton(
                    modifier = Modifier.weight(1f),
                    text = confirmLabel,
                    onClick = onConfirm,
                    style = confirmStyle,
                    fillMaxWidth = false,
                )
            }
        }
    }
}
