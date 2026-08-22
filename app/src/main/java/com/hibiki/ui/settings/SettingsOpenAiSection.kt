package com.hibiki.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hibiki.data.api.openai.OpenAiCostsClient
import com.hibiki.data.api.openai.OpenAiCostsReport
import com.hibiki.ui.components.DetailSection
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.components.HibikiDialog
import com.hibiki.ui.components.HibikiSelect
import com.hibiki.ui.components.HibikiSelectOption
import com.hibiki.ui.theme.Cyberpunk
import java.util.Locale

private enum class SettingsKeyUnlockRequest {
    API_KEY,
    ADMIN_API_KEY,
}

private val TranscriptionModelOptions = listOf(
    HibikiSelectOption("gpt-transcribe"),
)

private val AnalysisModelOptions = listOf(
    HibikiSelectOption("gpt-4o-mini"),
)

@Composable
fun SettingsOpenAiSection(
    uiState: SettingsUiState,
    onApiKeyChange: (String) -> Unit,
    onAdminApiKeyChange: (String) -> Unit,
    onTranscriptionModelChange: (String) -> Unit,
    onAnalysisModelChange: (String) -> Unit,
    onUnlockApiKeyEditing: () -> Unit,
    onUnlockAdminApiKeyEditing: () -> Unit,
    onTestConnection: () -> Unit,
    onFetchCosts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showApiKey by rememberSaveable { mutableStateOf(false) }
    var showAdminApiKey by rememberSaveable { mutableStateOf(false) }
    var unlockRequest by remember { mutableStateOf<SettingsKeyUnlockRequest?>(null) }

    val apiKeyLocked = uiState.persistedApiKey.isNotBlank() && !uiState.isApiKeyEditing
    val adminApiKeyLocked = uiState.persistedAdminApiKey.isNotBlank() && !uiState.isAdminApiKeyEditing

    unlockRequest?.let { request ->
        KeyUnlockConfirmDialog(
            title = "Modificare la chiave?",
            message = when (request) {
                SettingsKeyUnlockRequest.API_KEY ->
                    "La chiave API è già configurata. Conferma per abilitare la modifica."
                SettingsKeyUnlockRequest.ADMIN_API_KEY ->
                    "La chiave admin è già configurata. Conferma per abilitare la modifica."
            },
            onConfirm = {
                when (request) {
                    SettingsKeyUnlockRequest.API_KEY -> onUnlockApiKeyEditing()
                    SettingsKeyUnlockRequest.ADMIN_API_KEY -> onUnlockAdminApiKeyEditing()
                }
                unlockRequest = null
            },
            onDismiss = { unlockRequest = null },
        )
    }

    DetailSection(title = "OpenAI", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProtectedApiKeyField(
                value = uiState.apiKey,
                onValueChange = onApiKeyChange,
                label = "CHIAVE",
                isLocked = apiKeyLocked,
                onRequestUnlock = { unlockRequest = SettingsKeyUnlockRequest.API_KEY },
                showPlainText = showApiKey,
                onToggleVisibility = { showApiKey = !showApiKey },
                showVisibilityToggleContentDescription = "Mostra chiave",
                hideVisibilityToggleContentDescription = "Nascondi chiave",
            )
            ProtectedApiKeyField(
                value = uiState.adminApiKey,
                onValueChange = onAdminApiKeyChange,
                label = "CHIAVE ADMIN",
                isLocked = adminApiKeyLocked,
                onRequestUnlock = { unlockRequest = SettingsKeyUnlockRequest.ADMIN_API_KEY },
                showPlainText = showAdminApiKey,
                onToggleVisibility = { showAdminApiKey = !showAdminApiKey },
                showVisibilityToggleContentDescription = "Mostra chiave admin",
                hideVisibilityToggleContentDescription = "Nascondi chiave admin",
            )
            HibikiSelect(
                label = "Modello trascrizione",
                selected = uiState.transcriptionModel,
                options = TranscriptionModelOptions,
                onSelected = onTranscriptionModelChange,
            )
            HibikiSelect(
                label = "Modello analisi",
                selected = uiState.analysisModel,
                options = AnalysisModelOptions,
                onSelected = onAnalysisModelChange,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HibikiButton(
                    modifier = Modifier.weight(1f),
                    text = "TEST",
                    onClick = onTestConnection,
                    enabled = uiState.pingState !is PingState.Loading && uiState.apiKey.isNotBlank(),
                    loading = uiState.pingState is PingState.Loading,
                    style = HibikiButtonStyles.Violet,
                    fillMaxWidth = false,
                    disabledContainerColor = Cyberpunk.withAlpha(Cyberpunk.NeonViolet, 0.35f),
                    disabledContentColor = Cyberpunk.withAlpha(Cyberpunk.Void, 0.5f),
                )
                HibikiButton(
                    modifier = Modifier.weight(1f),
                    text = "COSTI",
                    onClick = onFetchCosts,
                    enabled = uiState.costsState !is CostsState.Loading && uiState.adminApiKey.isNotBlank(),
                    loading = uiState.costsState is CostsState.Loading,
                    style = HibikiButtonStyles.Lime,
                    fillMaxWidth = false,
                    disabledContainerColor = Cyberpunk.withAlpha(Cyberpunk.NeonLime, 0.35f),
                    disabledContentColor = Cyberpunk.withAlpha(Cyberpunk.Void, 0.5f),
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (uiState.persistState) {
                    PersistState.Error -> SettingsStatusLine(
                        text = "ERRORE DI SALVATAGGIO",
                        color = Cyberpunk.NeonMagenta,
                    )
                    else -> Unit
                }
                when (uiState.pingState) {
                    PingState.Success -> SettingsStatusLine(
                        text = "CONFIGURAZIONE VALIDA",
                        color = Cyberpunk.NeonViolet,
                    )
                    is PingState.Error -> SettingsStatusLine(
                        text = "ERRORE DI CONFIGURAZIONE",
                        color = Cyberpunk.NeonMagenta,
                    )
                    else -> Unit
                }
                OpenAiCostsSection(state = uiState.costsState)
            }
        }
    }
}

@Composable
private fun KeyUnlockConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    HibikiDialog(onDismissRequest = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Cyberpunk.TextPrimary,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Cyberpunk.TextMuted,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HibikiButton(
                modifier = Modifier.weight(1f),
                text = "ANNULLA",
                onClick = onDismiss,
                style = HibikiButtonStyles.Cancel,
                fillMaxWidth = false,
            )
            HibikiButton(
                modifier = Modifier.weight(1f),
                text = "CONFERMA",
                onClick = onConfirm,
                style = HibikiButtonStyles.Violet,
                fillMaxWidth = false,
            )
        }
    }
}

@Composable
private fun OpenAiCostsSection(
    state: CostsState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CostsState.Idle, CostsState.Loading -> Unit
        is CostsState.Error -> SettingsStatusLine(
            text = state.message,
            color = Cyberpunk.NeonMagenta,
            modifier = modifier,
        )
        is CostsState.Success -> OpenAiCostsReportView(
            report = state.report,
            modifier = modifier,
        )
    }
}

@Composable
private fun OpenAiCostsReportView(
    report: OpenAiCostsReport,
    modifier: Modifier = Modifier,
) {
    val lineItemsWithCost = report.lineItemTotals.filter {
        OpenAiCostsClient.hasBillableAmount(it.amount)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsStatusLine(
            text = "TOTALE ULTIMI 28 GIORNI: ${OpenAiCostsClient.formatAmount(report.totalValue, report.currency)}",
            color = Cyberpunk.NeonLime,
        )
        lineItemsWithCost.forEach { item ->
            SettingsStatusLine(
                text = "${item.lineItem}: ${OpenAiCostsClient.formatAmount(item.amount, item.currency)}",
                color = Cyberpunk.MutedLime,
            )
        }
        if (report.dailyCosts.isEmpty() && report.totalValue <= 0.0) {
            SettingsStatusLine(
                text = "NESSUN COSTO REGISTRATO NEGLI ULTIMI 28 GIORNI.",
                color = Cyberpunk.NeonLime,
            )
        } else {
            report.dailyCosts.chunked(2).forEach { rowDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowDays.forEach { day ->
                        SettingsStatusLine(
                            text = "${day.dateLabel}: ${OpenAiCostsClient.formatAmount(day.amount, day.currency)}",
                            color = if (OpenAiCostsClient.hasBillableAmount(day.amount)) {
                                Cyberpunk.NeonLime
                            } else {
                                Cyberpunk.TextMuted
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(2 - rowDays.size) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsStatusLine(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(Locale.ITALY),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = color,
    )
}
