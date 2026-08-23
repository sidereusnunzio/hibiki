package com.hibiki.ui.archive

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

@Composable
fun PhraseEditField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        colors = cyberpunkOutlinedTextFieldColors(),
        singleLine = singleLine,
    )
    Spacer(modifier = Modifier.height(10.dp))
}
