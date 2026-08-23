package com.hibiki.ui.contexts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageAction
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.components.HibikiCard
import com.hibiki.ui.components.SquareThumb
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

@Composable
fun SubjectsScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    viewModel: SubjectsViewModel = viewModel(),
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val query by viewModel.search.collectAsStateWithLifecycle()
    var error by remember { mutableStateOf<String?>(null) }
    AppPage(
        title = "Personaggi",
        kanji = "名",
        actions = {
            AppPageAction(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = "Nuovo", tint = Cyberpunk.NeonCyan)
            }
            AppPageBackAction(onBack)
        },
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ricerca") },
            colors = cyberpunkOutlinedTextFieldColors(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(10.dp))
        error?.let {
            Text(it, color = Cyberpunk.NeonMagenta)
            Spacer(modifier = Modifier.height(8.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(subjects, key = { it.id }) { subject ->
                HibikiCard(onClick = { onEdit(subject.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SquareThumb(
                            path = subject.imagePath,
                            contentDescription = subject.displayName,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(subject.displayName, color = Cyberpunk.TextPrimary, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(subject.japaneseName, color = Cyberpunk.MutedCyan, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { viewModel.delete(subject.id) { error = it } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Elimina", tint = Cyberpunk.NeonMagenta)
                        }
                    }
                }
            }
        }
    }
}
