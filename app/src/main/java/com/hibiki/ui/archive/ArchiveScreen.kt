package com.hibiki.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.domain.model.PhraseListItem
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.components.HibikiCard
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onOpenPhrase: (String) -> Unit,
    viewModel: ArchiveViewModel = viewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val contexts by viewModel.contexts.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val filters by viewModel.currentFilters.collectAsStateWithLifecycle()

    AppPage(
        title = "Archivio",
        kanji = "基",
        actions = { AppPageBackAction(onBack) },
    ) {
        OutlinedTextField(
            value = filters.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ricerca") },
            colors = cyberpunkOutlinedTextFieldColors(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filters.verifiedOnly,
                onClick = { viewModel.setVerifiedOnly(!filters.verifiedOnly) },
                label = { Text("Verified") },
                colors = chipColors(),
            )
            FilterChip(
                selected = !filters.newestFirst,
                onClick = { viewModel.setNewestFirst(!filters.newestFirst) },
                label = { Text(if (filters.newestFirst) "Nuove prima" else "Vecchie prima") },
                colors = chipColors(),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            contexts.forEach { context ->
                FilterChip(
                    selected = filters.contextId == context.id,
                    onClick = {
                        viewModel.setContext(if (filters.contextId == context.id) null else context.id)
                    },
                    label = { Text(context.name) },
                    colors = chipColors(),
                )
            }
        }
        if (subjects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                subjects.forEach { subject ->
                    FilterChip(
                        selected = filters.subjectId == subject.id,
                        onClick = {
                            viewModel.setSubject(if (filters.subjectId == subject.id) null else subject.id)
                        },
                        label = { Text(subject.displayName) },
                        colors = chipColors(),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f, fill = true)) {
            items(items, key = { it.phrase.id }) { item ->
                PhraseRow(
                    item = item,
                    onOpen = { onOpenPhrase(item.phrase.id) },
                    onDelete = { viewModel.delete(item.phrase.id) },
                    onPlay = { item.phrase.audioPath?.let(viewModel::play) },
                )
            }
        }
    }
}

@Composable
private fun PhraseRow(
    item: PhraseListItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
) {
    HibikiCard(onClick = onOpen) {
        Text(item.phrase.japaneseDisplay, color = Cyberpunk.TextPrimary, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(item.phrase.kana, color = Cyberpunk.MutedCyan, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(item.phrase.naturalTranslation, color = Cyberpunk.TextMuted, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(item.contextName)
                        item.subjectDisplayName?.let { append(" · ").append(it) }
                    },
                    color = Cyberpunk.NeonViolet,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (item.phrase.audioPath != null) {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Cyberpunk.NeonCyan)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Elimina", tint = Cyberpunk.NeonMagenta)
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Cyberpunk.Panel,
    labelColor = Cyberpunk.TextMuted,
    selectedContainerColor = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.18f),
    selectedLabelColor = Cyberpunk.NeonCyan,
)
