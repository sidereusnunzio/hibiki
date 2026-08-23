package com.hibiki.ui.contexts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.components.HibikiAccordion
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.components.HibikiSelect
import com.hibiki.ui.components.HibikiSelectOption
import com.hibiki.ui.components.HibikiToggleRow
import com.hibiki.ui.components.SquareImagePicker
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

private val LanguageOptions = listOf(HibikiSelectOption("ja", "Giapponese"))

@Composable
fun ContextEditScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ContextEditViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppPage(
        title = if (state.id == null) "Nuovo contesto" else "Modifica contesto",
        actions = { AppPageBackAction(onBack) },
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome") },
                colors = cyberpunkOutlinedTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            SquareImagePicker(
                imagePath = state.image.previewPath,
                onCropped = viewModel::setContextImage,
                label = "Immagine contesto",
            )
            Spacer(modifier = Modifier.height(10.dp))
            HibikiSelect(
                label = "Lingua prevista",
                selected = state.expectedLanguage,
                options = LanguageOptions,
                onSelected = viewModel::setLanguage,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::setPrompt,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Prompt aggiuntivo") },
                minLines = 5,
                colors = cyberpunkOutlinedTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            HibikiToggleRow(
                label = "Ha personaggi",
                checked = state.hasSubjects,
                onCheckedChange = viewModel::setHasSubjects,
                enabled = !state.isBuiltIn,
            )
            if (state.hasSubjects || state.subjects.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HibikiAccordion(
                    title = if (state.subjects.isEmpty()) {
                        "Personaggi"
                    } else {
                        "Personaggi (${state.subjects.size})"
                    },
                    initiallyExpanded = false,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.subjects.forEach { draft ->
                            key(draft.id) {
                                SubjectDraftCard(
                                    draft = draft,
                                    onDisplayNameChange = { viewModel.setSubjectDisplayName(draft.id, it) },
                                    onJapaneseNameChange = { viewModel.setSubjectJapaneseName(draft.id, it) },
                                    onPromptChange = { viewModel.setSubjectPrompt(draft.id, it) },
                                    onImageCropped = { viewModel.setSubjectImage(draft.id, it) },
                                    onRemove = { viewModel.removeSubject(draft.id) },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HibikiButton(
                        "AGGIUNGI PERSONAGGIO",
                        viewModel::addSubject,
                        HibikiButtonStyles.Secondary,
                    )
                }
            }
            state.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Cyberpunk.NeonMagenta)
            }
            Spacer(modifier = Modifier.height(16.dp))
            HibikiButton("SALVA", { viewModel.save(onDone) }, HibikiButtonStyles.Primary)
        }
    }
}

@Composable
private fun SubjectDraftCard(
    draft: SubjectDraft,
    onDisplayNameChange: (String) -> Unit,
    onJapaneseNameChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onImageCropped: (android.graphics.Bitmap) -> Unit,
    onRemove: () -> Unit,
) {
    HibikiAccordion(
        title = draft.displayName.ifBlank { "Nuovo personaggio" },
        actions = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Elimina", tint = Cyberpunk.NeonMagenta)
            }
        },
    ) {
        SquareImagePicker(
            imagePath = draft.image.previewPath,
            onCropped = onImageCropped,
            label = "Immagine personaggio",
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = draft.displayName,
            onValueChange = onDisplayNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nome rōmaji") },
            colors = cyberpunkOutlinedTextFieldColors(containerColor = Cyberpunk.PanelTranslucent),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = draft.japaneseName,
            onValueChange = onJapaneseNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nome giapponese") },
            colors = cyberpunkOutlinedTextFieldColors(containerColor = Cyberpunk.PanelTranslucent),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = draft.prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Prompt aggiuntivo") },
            minLines = 3,
            colors = cyberpunkOutlinedTextFieldColors(containerColor = Cyberpunk.PanelTranslucent),
        )
    }
}
