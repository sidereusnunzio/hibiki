package com.hibiki.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.domain.model.ArashiSyncState
import com.hibiki.domain.model.PhraseListItem
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.ArashiKanji
import com.hibiki.ui.components.HibikiCard
import com.hibiki.ui.components.HibikiCountChip
import com.hibiki.ui.components.SegmentedTabs
import com.hibiki.ui.components.SquareThumb
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

@Composable
fun ArchiveScreen(
    onOpenPhrase: (String) -> Unit,
    viewModel: ArchiveViewModel = viewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val contexts by viewModel.contexts.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val filters by viewModel.currentFilters.collectAsStateWithLifecycle()

    LaunchedEffect(contexts, filters.contextId) {
        if (contexts.isEmpty()) return@LaunchedEffect
        if (contexts.none { it.id == filters.contextId }) {
            viewModel.setContext(contexts.first().id)
        }
    }

    val selectedContextIndex = contexts.indexOfFirst { it.id == filters.contextId }.coerceAtLeast(0)
    val selectedSubject = subjects.find { it.id == filters.subjectId }

    AppPage(
        title = "Database",
    ) {
        if (contexts.isNotEmpty()) {
            SegmentedTabs(
                labels = contexts.map { it.name },
                selectedIndex = selectedContextIndex,
                onSelect = { index -> viewModel.setContext(contexts[index].id) },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        if (subjects.isNotEmpty()) {
            PhraseSubjectPickerField(
                selected = selectedSubject,
                subjects = subjects,
                onSelect = viewModel::setSubject,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.weight(1f),
                label = { Text("Cerca") },
                colors = cyberpunkOutlinedTextFieldColors(),
                singleLine = true,
            )
            HibikiCountChip(
                label = items.size.toString(),
                modifier = Modifier.semantics {
                    contentDescription = "${items.size} frasi"
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f, fill = true)) {
            items(items, key = { it.phrase.id }) { item ->
                PhraseRow(
                    item = item,
                    onOpen = { onOpenPhrase(item.phrase.id) },
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
    onPlay: () -> Unit,
) {
    HibikiCard(onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.phrase.japaneseDisplay,
                    color = Cyberpunk.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.phrase.naturalTranslation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.phrase.naturalTranslation,
                        color = Cyberpunk.NeonLime,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (item.phrase.arashiSyncState != ArashiSyncState.DO_NOT_SYNC) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ArashiKanji(syncState = item.phrase.arashiSyncState)
                }
            }
            item.listThumbPath?.let { path ->
                SquareThumb(
                    path = path,
                    contentDescription = item.subjectDisplayName ?: item.contextName,
                    showPlaceholder = false,
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            if (item.phrase.audioPath != null) {
                                Modifier.clickable(onClick = onPlay)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}
