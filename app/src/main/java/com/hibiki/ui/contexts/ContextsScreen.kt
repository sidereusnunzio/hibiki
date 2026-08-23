package com.hibiki.ui.contexts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageAction
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.components.HibikiCard
import com.hibiki.ui.components.SquareThumb
import com.hibiki.ui.theme.Cyberpunk

@Composable
fun ContextsScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    viewModel: ContextsViewModel = viewModel(),
) {
    val contexts by viewModel.contexts.collectAsStateWithLifecycle()
    var error by remember { mutableStateOf<String?>(null) }
    AppPage(
        title = "Contesti",
        actions = {
            AppPageBackAction(onBack)
            ContextsActionsMenu(onCreate = onCreate)
        },
    ) {
        error?.let {
            Text(it, color = Cyberpunk.NeonMagenta)
            Spacer(modifier = Modifier.height(8.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(contexts, key = { it.id }) { context ->
                HibikiCard(onClick = { onEdit(context.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SquareThumb(
                            path = context.imagePath,
                            contentDescription = context.name,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = context.name,
                            color = Cyberpunk.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (!context.isBuiltIn) {
                            IconButton(onClick = { viewModel.delete(context.id) { error = it } }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Elimina",
                                    tint = Cyberpunk.NeonMagenta,
                                )
                            }
                        }
                        if (context.hasSubjects) {
                            Text(
                                text = "者",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 15.sp,
                                color = Cyberpunk.TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextsActionsMenu(onCreate: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AppPageAction(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Opzioni",
                tint = Cyberpunk.TextPrimary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Cyberpunk.PanelElevated,
        ) {
            DropdownMenuItem(
                text = { Text("Nuovo contesto", color = Cyberpunk.TextPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Cyberpunk.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onCreate()
                },
            )
        }
    }
}

