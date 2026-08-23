package com.hibiki.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.domain.model.AudioBufferConfig
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.components.DetailSection
import com.hibiki.ui.components.HibikiToggleRow
import com.hibiki.ui.theme.Cyberpunk

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AppPage(
        title = "Impostazioni",
        actions = { AppPageBackAction(onBack) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsOpenAiSection(
                uiState = uiState,
                onApiKeyChange = viewModel::setApiKey,
                onAdminApiKeyChange = viewModel::setAdminApiKey,
                onTranscriptionModelChange = viewModel::setTranscriptionModel,
                onAnalysisModelChange = viewModel::setAnalysisModel,
                onUnlockApiKeyEditing = viewModel::unlockApiKeyEditing,
                onUnlockAdminApiKeyEditing = viewModel::unlockAdminApiKeyEditing,
                onTestConnection = viewModel::testConnection,
                onFetchCosts = viewModel::fetchCosts,
            )
            DetailSection(title = "Audio") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Durata massima: ${uiState.audio.maxDurationSeconds}s",
                        color = Cyberpunk.TextMuted,
                    )
                    Slider(
                        value = uiState.audio.maxDurationSeconds.toFloat(),
                        onValueChange = { viewModel.setMaxDuration(it.toInt()) },
                        valueRange = 2f..30f,
                        steps = 27,
                        colors = SliderDefaults.colors(
                            thumbColor = Cyberpunk.NeonCyan,
                            activeTrackColor = Cyberpunk.NeonCyan,
                            inactiveTrackColor = Cyberpunk.PanelElevated,
                        ),
                    )
                    Text(
                        "Durata buffer: ${uiState.audio.bufferDurationSeconds}s",
                        color = Cyberpunk.TextMuted,
                    )
                    Slider(
                        value = uiState.audio.bufferDurationSeconds.toFloat(),
                        onValueChange = { viewModel.setBufferDuration(it.toInt()) },
                        valueRange = AudioBufferConfig.MIN_SECONDS.toFloat()..AudioBufferConfig.MAX_SECONDS.toFloat(),
                        steps = AudioBufferConfig.MAX_SECONDS - AudioBufferConfig.MIN_SECONDS - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = Cyberpunk.NeonCyan,
                            activeTrackColor = Cyberpunk.NeonCyan,
                            inactiveTrackColor = Cyberpunk.PanelElevated,
                        ),
                    )
                    HibikiToggleRow("Salva audio", uiState.audio.saveAudio, viewModel::setSaveAudio)
                    HibikiToggleRow("Trim silenzio", uiState.audio.trimSilence, viewModel::setTrimSilence)
                    Text("Formato: AAC / M4A", color = Cyberpunk.TextMuted)
                    Text(
                        "Soglia fingerprint: ${(uiState.audio.fingerprintThreshold * 100).toInt()}%",
                        color = Cyberpunk.TextMuted,
                    )
                    Slider(
                        value = uiState.audio.fingerprintThreshold,
                        onValueChange = viewModel::setThreshold,
                        valueRange = 0.80f..0.98f,
                        colors = SliderDefaults.colors(
                            thumbColor = Cyberpunk.NeonCyan,
                            activeTrackColor = Cyberpunk.NeonCyan,
                            inactiveTrackColor = Cyberpunk.PanelElevated,
                        ),
                    )
                }
            }
            DetailSection(title = "Overlay") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HibikiToggleRow("Japanese", uiState.overlayDisplay.showJapanese, viewModel::setShowJapanese)
                    HibikiToggleRow("Kana", uiState.overlayDisplay.showKana, viewModel::setShowKana)
                    HibikiToggleRow("Rōmaji", uiState.overlayDisplay.showRomaji, viewModel::setShowRomaji)
                    HibikiToggleRow(
                        "Traduzione letterale",
                        uiState.overlayDisplay.showLiteral,
                        viewModel::setShowLiteral,
                    )
                    HibikiToggleRow(
                        "Traduzione naturale",
                        uiState.overlayDisplay.showNatural,
                        viewModel::setShowNatural,
                    )
                }
            }
            SettingsDataSection(viewModel = viewModel)
        }
    }
}
