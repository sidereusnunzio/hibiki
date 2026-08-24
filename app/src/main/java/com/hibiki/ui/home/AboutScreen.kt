package com.hibiki.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.R
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.home.about.AboutBlinkingCursor
import com.hibiki.ui.home.about.AboutCounterCell
import com.hibiki.ui.home.about.AboutCrtOverlay
import com.hibiki.ui.home.about.AboutDeveloperTerminal
import com.hibiki.ui.home.about.AboutDeviceHeader
import com.hibiki.ui.home.about.AboutHexBackground
import com.hibiki.ui.home.about.AboutKeyValue
import com.hibiki.ui.home.about.AboutLogoGlitchLayer
import com.hibiki.ui.home.about.AboutPanel
import com.hibiki.ui.home.about.AboutProgressBar
import com.hibiki.ui.home.about.AboutPulseDot
import com.hibiki.ui.home.about.AboutScanlineOnce
import com.hibiki.ui.home.about.AboutSectionLabel
import com.hibiki.ui.home.about.AboutStatusRow
import com.hibiki.ui.home.about.AboutTimelineLog
import com.hibiki.ui.home.about.AboutUiState
import com.hibiki.ui.home.about.rememberAboutBootGlitch
import com.hibiki.ui.theme.Cyberpunk
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bootGlitching = rememberAboutBootGlitch()
    var showTerminal by rememberSaveable { mutableStateOf(false) }
    var logoTaps by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(logoTaps) {
        if (logoTaps <= 0 || showTerminal) return@LaunchedEffect
        delay(2_500)
        logoTaps = 0
    }

    AppPage(
        title = stringResource(R.string.about_title),
        actions = { AppPageBackAction(onBack) },
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            AboutHexBackground()
            if (bootGlitching) {
                AboutCrtOverlay()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AboutHeaderBlock(
                    state = uiState,
                    bootGlitching = bootGlitching,
                    onHeaderTap = {
                        if (!showTerminal) {
                            logoTaps += 1
                            if (logoTaps >= 7) {
                                showTerminal = true
                                logoTaps = 0
                            }
                        }
                    },
                )

                AboutSystemStatusPanel(state = uiState)
                AboutCountersPanel(state = uiState)
                AboutDevicePanel(state = uiState)
                AboutTimelinePanel(state = uiState)
                AboutAiCorePanel(state = uiState)
                AboutIndexingPanel(state = uiState)
                AboutCoordinatesPanel(state = uiState)
                AboutCreditsPanel()
                AboutBlinkingCursor(modifier = Modifier.padding(top = 4.dp))
            }

            if (showTerminal) {
                AboutDeveloperTerminal(onDismiss = { showTerminal = false })
            }
        }
    }
}

@Composable
private fun AboutHeaderBlock(
    state: AboutUiState,
    bootGlitching: Boolean,
    onHeaderTap: () -> Unit,
) {
    val logoStyle = TextStyle(
        color = Cyberpunk.TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 4.sp,
        lineHeight = 28.sp,
    )

    AboutPanel(onTap = onHeaderTap) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box {
                        AboutLogoGlitchLayer(
                            kanji = stringResource(R.string.home_kanji),
                            brand = stringResource(R.string.about_brand),
                            glitching = bootGlitching,
                            style = logoStyle,
                        )
                        if (bootGlitching) {
                            AboutScanlineOnce(
                                active = true,
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.about_subtitle),
                        style = MaterialTheme.typography.labelLarge,
                        color = Cyberpunk.MutedCyan,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    text = state.buildId,
                    style = MaterialTheme.typography.labelLarge,
                    color = Cyberpunk.MutedCyan,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.about_status_operational),
                style = MaterialTheme.typography.bodyMedium,
                color = Cyberpunk.NeonLime,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun AboutSystemStatusPanel(state: AboutUiState) {
    AboutPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutSectionLabel(text = stringResource(R.string.about_system_status))
            AboutProgressBar(progress = 1f)
            Text(
                text = "100%",
                style = MaterialTheme.typography.labelLarge,
                color = Cyberpunk.NeonLime,
                fontFamily = FontFamily.Monospace,
            )
            AboutStatusRow(
                label = stringResource(R.string.about_status_db_integrity),
                value = state.databaseIntegrityLabel,
                valueColor = if (state.databaseIntegrityOk) Cyberpunk.NeonLime else Cyberpunk.NeonMagenta,
            )
            AboutStatusRow(
                label = stringResource(R.string.about_status_fingerprint_index),
                value = if (state.fingerprintIndexOnline) {
                    stringResource(R.string.about_status_online)
                } else {
                    stringResource(R.string.about_status_standby)
                },
            )
            AboutStatusRow(
                label = stringResource(R.string.about_status_audio_engine),
                value = stringResource(R.string.about_status_ready),
                barProgress = state.audioCoveragePercent / 100f,
            )
            AboutStatusRow(
                label = stringResource(R.string.about_status_fingerprints),
                value = "${state.fingerprintCoveragePercent}%",
                barProgress = state.fingerprintCoveragePercent / 100f,
            )
        }
    }
}

@Composable
private fun AboutCountersPanel(state: AboutUiState) {
    AboutPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AboutSectionLabel(text = stringResource(R.string.about_counters_title))
            Row(modifier = Modifier.fillMaxWidth()) {
                AboutCounterCell(
                    label = stringResource(R.string.about_counter_phrases),
                    value = state.phraseCount,
                    modifier = Modifier.weight(1f),
                )
                AboutCounterCell(
                    label = stringResource(R.string.about_counter_audio),
                    value = state.audioSampleCount,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                AboutCounterCell(
                    label = stringResource(R.string.about_counter_contexts),
                    value = state.contextCount,
                    modifier = Modifier.weight(1f),
                )
                AboutCounterCell(
                    label = stringResource(R.string.about_counter_subjects),
                    value = state.subjectCount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AboutDevicePanel(state: AboutUiState) {
    AboutPanel {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AboutDeviceHeader()
            AboutKeyValue(
                key = stringResource(R.string.about_device_os),
                value = state.androidVersion,
            )
            AboutKeyValue(
                key = stringResource(R.string.about_device_model),
                value = state.deviceModel,
            )
            AboutKeyValue(
                key = stringResource(R.string.about_device_ram),
                value = stringResource(
                    R.string.about_device_gb_pair,
                    formatGb(state.ramUsedGb),
                    formatGb(state.ramTotalGb),
                ),
            )
            AboutKeyValue(
                key = stringResource(R.string.about_device_storage),
                value = stringResource(
                    R.string.about_device_gb_pair,
                    formatGb(state.storageUsedGb),
                    formatGb(state.storageTotalGb),
                ),
            )
            AboutKeyValue(
                key = stringResource(R.string.about_device_locale),
                value = state.localeTag,
            )
        }
    }
}

@Composable
private fun AboutTimelinePanel(state: AboutUiState) {
    AboutPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutSectionLabel(text = stringResource(R.string.about_timeline_title))
            AboutTimelineLog(
                bootDate = state.bootDate,
                firstPhraseDate = state.firstPhraseDate,
                currentBuildDate = state.currentBuildDate,
            )
        }
    }
}

@Composable
private fun AboutAiCorePanel(state: AboutUiState) {
    AboutPanel {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AboutSectionLabel(text = stringResource(R.string.about_ai_core_title))
                AboutPulseDot(active = state.aiAvailable)
            }
            AboutKeyValue(
                key = stringResource(R.string.about_ai_transcription),
                value = state.transcriptionModelLabel,
            )
            AboutKeyValue(
                key = stringResource(R.string.about_ai_analysis),
                value = state.analysisModelLabel,
            )
            AboutKeyValue(
                key = stringResource(R.string.about_ai_link),
                value = if (state.aiAvailable) {
                    stringResource(R.string.about_status_online)
                } else {
                    stringResource(R.string.about_status_standby)
                },
            )
            AboutKeyValue(
                key = stringResource(R.string.about_ai_prompt_engine),
                value = state.analysisPromptVersion.toString(),
            )
        }
    }
}

@Composable
private fun AboutIndexingPanel(state: AboutUiState) {
    AboutPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutSectionLabel(text = stringResource(R.string.about_indexing_title))
            IndexingRow(
                label = stringResource(R.string.about_indexing_audio),
                percent = state.audioCoveragePercent.coerceIn(0, 100),
                oscillating = true,
            )
            IndexingRow(
                label = stringResource(R.string.about_indexing_fingerprints),
                percent = state.fingerprintCoveragePercent.coerceIn(0, 100),
                oscillating = false,
            )
            IndexingRow(
                label = stringResource(R.string.about_indexing_contexts),
                percent = 100,
                oscillating = false,
            )
        }
    }
}

@Composable
private fun IndexingRow(
    label: String,
    percent: Int,
    oscillating: Boolean,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.bodyMedium,
                color = Cyberpunk.TextMuted,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodyMedium,
                color = Cyberpunk.NeonLime,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AboutProgressBar(
            progress = percent / 100f,
            oscillating = oscillating,
        )
    }
}

@Composable
private fun AboutCoordinatesPanel(state: AboutUiState) {
    AboutPanel {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AboutSectionLabel(text = stringResource(R.string.about_node_title))
            AboutKeyValue(
                key = stringResource(R.string.about_node_id),
                value = stringResource(R.string.about_node_name),
            )
            AboutKeyValue(
                key = stringResource(R.string.about_node_location),
                value = stringResource(R.string.about_node_coords),
            )
            AboutKeyValue(
                key = stringResource(R.string.about_node_link),
                value = stringResource(R.string.about_status_online),
            )
            Spacer(modifier = Modifier.height(4.dp))
            AboutKeyValue(
                key = stringResource(R.string.about_session_id),
                value = state.sessionId,
            )
            AboutKeyValue(
                key = stringResource(R.string.about_entropy),
                value = stringResource(R.string.about_entropy_value, state.entropyPercent),
            )
        }
    }
}

@Composable
private fun AboutCreditsPanel() {
    AboutPanel {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AboutSectionLabel(text = stringResource(R.string.about_credits_title))
            AboutKeyValue(
                key = stringResource(R.string.about_credits_designed),
                value = stringResource(R.string.about_credits_designed_by),
            )
            AboutKeyValue(
                key = stringResource(R.string.about_credits_powered),
                value = stringResource(R.string.about_credits_powered_by),
            )
            AboutKeyValue(
                key = stringResource(R.string.about_credits_tested),
                value = stringResource(R.string.about_credits_tested_by),
            )
            AboutKeyValue(
                key = stringResource(R.string.about_credits_bugs),
                value = stringResource(R.string.about_credits_bugs_value),
            )
        }
    }
}

private fun formatGb(value: Float): String =
    String.format(Locale.US, "%.1f", value)
