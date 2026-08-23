package com.hibiki.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hibiki.domain.model.Subject
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.components.HibikiDialog
import com.hibiki.ui.components.SquareThumb
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

@Composable
fun PhraseSubjectPickerField(
    selected: Subject?,
    subjects: List<Subject>,
    onSelect: (String?) -> Unit,
    emptyLabel: String = "Tutti i personaggi",
    clearLabel: String = "Tutti",
) {
    var showPicker by remember { mutableStateOf(false) }
    val fieldShape = RoundedCornerShape(2.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cyberpunk.Panel, fieldShape)
            .border(
                1.dp,
                if (showPicker) Cyberpunk.NeonCyan else Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.35f),
                fieldShape,
            )
            .clickable { showPicker = true }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (selected != null) {
            SquareThumb(
                path = selected.imagePath,
                contentDescription = selected.displayName,
                showPlaceholder = false,
                modifier = Modifier.size(36.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Cyberpunk.PanelElevated)
                    .border(1.dp, Cyberpunk.GridLine, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = Cyberpunk.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "PERSONAGGIO",
                style = MaterialTheme.typography.labelLarge,
                color = Cyberpunk.MutedCyan,
            )
            Text(
                text = selected?.displayName ?: emptyLabel,
                color = Cyberpunk.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Cyberpunk.MutedCyan,
        )
    }
    if (showPicker) {
        PhraseSubjectPickerDialog(
            selectedId = selected?.id,
            subjects = subjects,
            clearLabel = clearLabel,
            onSelect = { id ->
                onSelect(id)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun PhraseSubjectPickerDialog(
    selectedId: String?,
    subjects: List<Subject>,
    clearLabel: String,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(subjects, query) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            subjects
        } else {
            subjects.filter { subject ->
                subject.displayName.contains(needle, ignoreCase = true) ||
                    subject.japaneseName.contains(needle, ignoreCase = true)
            }
        }
    }

    HibikiDialog(onDismissRequest = onDismiss) {
        Text(
            text = "Personaggio",
            style = MaterialTheme.typography.titleMedium,
            color = Cyberpunk.TextPrimary,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Cerca") },
            colors = cyberpunkOutlinedTextFieldColors(),
            singleLine = true,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            item(key = "clear") {
                PhraseSubjectPickerTile(
                    label = clearLabel,
                    imagePath = null,
                    selected = selectedId == null,
                    placeholderIcon = true,
                    onClick = { onSelect(null) },
                )
            }
            gridItems(filtered, key = { it.id }) { subject ->
                PhraseSubjectPickerTile(
                    label = subject.displayName,
                    imagePath = subject.imagePath,
                    selected = subject.id == selectedId,
                    placeholderIcon = false,
                    onClick = { onSelect(subject.id) },
                )
            }
        }
        HibikiButton(
            text = "CHIUDI",
            onClick = onDismiss,
            style = HibikiButtonStyles.Cancel,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PhraseSubjectPickerTile(
    label: String,
    imagePath: String?,
    selected: Boolean,
    placeholderIcon: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .border(
                1.dp,
                if (selected) Cyberpunk.NeonCyan else Cyberpunk.GridLine,
                shape,
            )
            .background(
                if (selected) Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.14f) else Cyberpunk.PanelElevated,
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (placeholderIcon) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(shape)
                    .background(Cyberpunk.Panel),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = label,
                    tint = if (selected) Cyberpunk.NeonCyan else Cyberpunk.TextMuted,
                )
            }
        } else {
            SquareThumb(
                path = imagePath,
                contentDescription = label,
                showPlaceholder = false,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) Cyberpunk.NeonCyan else Cyberpunk.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
