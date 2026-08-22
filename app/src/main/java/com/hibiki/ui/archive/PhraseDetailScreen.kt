package com.hibiki.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.components.FieldValue
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

@Composable
fun PhraseDetailScreen(
    onBack: () -> Unit,
    viewModel: PhraseDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state
    AppPage(
        title = "Dettaglio",
        kanji = "句",
        actions = { AppPageBackAction(onBack) },
    ) {
        if (current == null) {
            Text("Caricamento…", color = Cyberpunk.TextMuted)
            return@AppPage
        }
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            if (current.phrase.audioPath != null) {
                HibikiButton("▶ RIPRODUCI", viewModel::play, HibikiButtonStyles.Secondary)
                Spacer(modifier = Modifier.height(12.dp))
            }
            EditField("日本語", current.japaneseEdit, viewModel::updateJapanese)
            EditField("かな", current.kana, viewModel::updateKana)
            EditField("Rōmaji", current.romaji, viewModel::updateRomaji)
            EditField("Traduzione letterale", current.literal, viewModel::updateLiteral)
            EditField("Traduzione naturale", current.natural, viewModel::updateNatural)
            FieldValue("Raw", current.phrase.japaneseRaw)
            FieldValue("Contesto", current.contextName)
            FieldValue("Personaggio", current.subject?.let { "${it.displayName} · ${it.japaneseName}" } ?: "—")
            FieldValue("Data", current.createdLabel)
            FieldValue("Source", current.phrase.source.name)
            FieldValue("Trascrizione", "${current.phrase.transcriptionModel} · v${current.phrase.transcriptionPromptVersion}")
            FieldValue("Analisi", "${current.phrase.analysisModel} · v${current.phrase.analysisPromptVersion}")
            current.phrase.confidence?.let { FieldValue("Confidence", "${(it * 100).toInt()}%") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = current.phrase.verified,
                    onCheckedChange = viewModel::setVerified,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Cyberpunk.NeonLime,
                        uncheckedColor = Cyberpunk.TextMuted,
                    ),
                )
                Text("Verified", color = Cyberpunk.TextPrimary)
            }
            current.error?.let {
                Text(it, color = Cyberpunk.NeonMagenta, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        colors = cyberpunkOutlinedTextFieldColors(),
    )
    Spacer(modifier = Modifier.height(10.dp))
}
