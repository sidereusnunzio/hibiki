package com.hibiki.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

private object HibikiSwitchColors {
    @Composable
    fun defaults() = SwitchDefaults.colors(
        checkedThumbColor = Cyberpunk.Void,
        checkedTrackColor = Cyberpunk.TextPrimary,
        checkedBorderColor = Cyberpunk.TextPrimary,
        uncheckedThumbColor = Cyberpunk.TextMuted,
        uncheckedTrackColor = Cyberpunk.withAlpha(Cyberpunk.TextMuted, 0.28f),
        uncheckedBorderColor = Cyberpunk.TextMuted,
        disabledCheckedThumbColor = Cyberpunk.Void,
        disabledCheckedTrackColor = Cyberpunk.withAlpha(Cyberpunk.TextPrimary, 0.35f),
        disabledUncheckedThumbColor = Cyberpunk.withAlpha(Cyberpunk.TextMuted, 0.45f),
        disabledUncheckedTrackColor = Cyberpunk.withAlpha(Cyberpunk.TextMuted, 0.16f),
    )
}

@Composable
fun HibikiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
        colors = HibikiSwitchColors.defaults(),
    )
}

data class HibikiSelectOption(
    val value: String,
    val label: String = value,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HibikiSelect(
    label: String,
    selected: String,
    options: List<HibikiSelectOption>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    val current = options.find { it.value == selected }
        ?: options.firstOrNull()?.takeIf { selected.isBlank() }
    val display = current?.label ?: selected

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = cyberpunkOutlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Cyberpunk.PanelElevated,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, color = Cyberpunk.TextPrimary) },
                    onClick = {
                        onSelected(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun HibikiToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                !enabled -> Cyberpunk.withAlpha(Cyberpunk.TextMuted, 0.5f)
                checked -> Cyberpunk.TextPrimary
                else -> Cyberpunk.TextMuted
            },
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = HibikiSwitchColors.defaults(),
        )
    }
}
