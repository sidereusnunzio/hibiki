package com.hibiki.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.domain.model.ArashiSyncState
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.components.DetailSection
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.components.HibikiConfirmDialog
import com.hibiki.ui.components.HibikiSelect
import com.hibiki.ui.components.HibikiSelectOption
import com.hibiki.ui.components.SegmentedTabs
import com.hibiki.ui.theme.Cyberpunk

private val ArashiSyncOptions = listOf(
    HibikiSelectOption(ArashiSyncState.DO_NOT_SYNC.name, "Non sincronizzare"),
    HibikiSelectOption(ArashiSyncState.PENDING.name, "Da sincronizzare"),
    HibikiSelectOption(ArashiSyncState.SYNCED.name, "Sincronizzata"),
)

@Composable
fun PhraseAdvancedEditScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: PhraseAdvancedEditViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val contexts by viewModel.contexts.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val current = state

    LaunchedEffect(contexts, current?.contextId) {
        if (contexts.isEmpty() || current == null) return@LaunchedEffect
        if (contexts.none { it.id == current.contextId }) {
            viewModel.setContext(contexts.first().id)
        }
    }

    LaunchedEffect(subjects, current?.subjectId) {
        val subjectId = current?.subjectId ?: return@LaunchedEffect
        if (subjects.none { it.id == subjectId }) {
            viewModel.setSubject(null)
        }
    }

    AppPage(
        title = "Modifica avanzata",
        actions = { AppPageBackAction(onBack) },
    ) {
        if (current == null) {
            Text("Caricamento…", color = Cyberpunk.TextMuted)
            return@AppPage
        }

        val selectedContext = contexts.find { it.id == current.contextId }
        val selectedContextIndex = contexts.indexOfFirst { it.id == current.contextId }.coerceAtLeast(0)
        val selectedSubject = subjects.find { it.id == current.subjectId }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailSection(title = "Contesto") {
                if (contexts.isNotEmpty()) {
                    SegmentedTabs(
                        labels = contexts.map { it.name },
                        selectedIndex = selectedContextIndex,
                        onSelect = { index -> viewModel.setContext(contexts[index].id) },
                    )
                }
                if (selectedContext?.hasSubjects == true && subjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    PhraseSubjectPickerField(
                        selected = selectedSubject,
                        subjects = subjects,
                        onSelect = viewModel::setSubject,
                        emptyLabel = "Nessuno",
                        clearLabel = "Nessuno",
                    )
                }
            }

            DetailSection(title = "Frase") {
                PhraseEditField("日本語", current.japaneseEdit, viewModel::updateJapanese)
            }

            DetailSection(title = "Pronuncia") {
                PhraseEditField("かな", current.kana, viewModel::updateKana)
                PhraseEditField("Rōmaji", current.romaji, viewModel::updateRomaji)
            }

            DetailSection(title = "Traduzioni") {
                PhraseEditField(
                    label = "Traduzione letterale",
                    value = current.literal,
                    onChange = viewModel::updateLiteral,
                    singleLine = false,
                )
                PhraseEditField(
                    label = "Traduzione naturale",
                    value = current.natural,
                    onChange = viewModel::updateNatural,
                    singleLine = false,
                )
            }

            DetailSection(title = "Sincronizzazione Arashi") {
                HibikiSelect(
                    label = "Stato",
                    selected = current.arashiSyncState.name,
                    options = ArashiSyncOptions,
                    onSelected = { value ->
                        viewModel.setArashiSyncState(ArashiSyncState.valueOf(value))
                    },
                )
            }

            current.error?.let { message ->
                Text(text = message, color = Cyberpunk.NeonMagenta)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                HibikiButton(
                    text = "SALVA",
                    onClick = viewModel::save,
                    style = HibikiButtonStyles.Primary,
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = false,
                    loading = current.saving,
                )
                HibikiButton(
                    text = "RIGENERA",
                    onClick = viewModel::regenerate,
                    style = HibikiButtonStyles.Violet,
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = false,
                    loading = current.regenerating,
                )
            }

            DetailSection(title = "Azioni irreversibili") {
                HibikiButton(
                    text = "Elimina frase",
                    onClick = { confirmDelete = true },
                    style = HibikiButtonStyles.Destructive,
                )
            }
        }
    }

    if (confirmDelete) {
        val preview = current?.japaneseEdit.orEmpty()
        HibikiConfirmDialog(
            title = "Eliminare la frase?",
            message = if (preview.isBlank()) {
                "L'operazione non può essere annullata."
            } else {
                "Vuoi eliminare «$preview»? L'operazione non può essere annullata."
            },
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDeleted)
            },
            onDismiss = { confirmDelete = false },
            confirmLabel = "ELIMINA",
            confirmStyle = HibikiButtonStyles.Destructive,
        )
    }
}
