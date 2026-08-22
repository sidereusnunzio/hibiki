package com.hibiki.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

@Composable
fun ProtectedApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isLocked: Boolean,
    onRequestUnlock: () -> Unit,
    showPlainText: Boolean,
    onToggleVisibility: () -> Unit,
    showVisibilityToggleContentDescription: String,
    hideVisibilityToggleContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = if (isLocked) ({ _ -> }) else onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            readOnly = isLocked,
            interactionSource = interactionSource,
            colors = cyberpunkOutlinedTextFieldColors(),
            visualTransformation = if (showPlainText) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(
                    onClick = onToggleVisibility,
                    enabled = !isLocked,
                ) {
                    Icon(
                        imageVector = if (showPlainText) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPlainText) {
                            hideVisibilityToggleContentDescription
                        } else {
                            showVisibilityToggleContentDescription
                        },
                    )
                }
            },
        )
        if (isLocked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onRequestUnlock),
            )
        }
    }
}
