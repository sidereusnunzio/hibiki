package com.hibiki.ui.archive

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.data.arashi.ArashiExportContract
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.components.DetailSection
import com.hibiki.ui.components.FieldValue
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.components.HibikiDialog
import com.hibiki.ui.theme.Cyberpunk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private const val PhraseBrowsePagerEnableDelayMs = 320L

@Composable
fun PhraseDetailScreen(
    phraseId: String,
    onBack: () -> Unit,
    onAdvancedEdit: (String) -> Unit,
    onRandomPhrase: (String) -> Unit,
    viewModel: PhraseDetailViewModel = viewModel(),
) {
    LaunchedEffect(phraseId) {
        if (viewModel.currentPhraseId.value != phraseId) {
            viewModel.showBrowsePhrase(phraseId)
        }
    }

    val currentPhraseId by viewModel.currentPhraseId.collectAsStateWithLifecycle()
    val browsePhraseIds by viewModel.browsePhraseIds.collectAsStateWithLifecycle()
    val detailCache by viewModel.detailCache.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showInspect by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val arashiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onArashiActivityResult(
            context = context,
            resultCode = result.resultCode,
            resultJson = result.data?.getStringExtra(ArashiExportContract.EXTRA_IMPORT_RESULT),
            errorMessage = result.data?.getStringExtra(ArashiExportContract.EXTRA_IMPORT_ERROR),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.arashiLaunch.collect { intent ->
            runCatching { arashiLauncher.launch(intent) }
                .onFailure { viewModel.onArashiLaunchFailed(context, it) }
        }
    }

    LaunchedEffect(state?.error) {
        val message = state?.error ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    var browsePagerEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(phraseId) {
        browsePagerEnabled = false
        delay(PhraseBrowsePagerEnableDelayMs)
        browsePagerEnabled = true
    }

    val pagerPhraseIds = remember(browsePhraseIds, currentPhraseId, browsePagerEnabled) {
        if (browsePagerEnabled) {
            val index = browsePhraseIds.indexOf(currentPhraseId)
            if (index >= 0) browsePhraseIds else listOf(currentPhraseId)
        } else {
            listOf(currentPhraseId)
        }
    }
    val pagerModeKey = if (browsePagerEnabled && browsePhraseIds.contains(currentPhraseId)) {
        "browse"
    } else {
        "single"
    }
    val latestShowBrowsePhrase by rememberUpdatedState(viewModel::showBrowsePhrase)

    AppPage(
        title = "DETTAGLIO FRASE",
        actions = { AppPageBackAction(onBack) },
    ) {
        if (state == null && detailCache.isEmpty()) {
            Text("Caricamento…", color = Cyberpunk.TextMuted)
            return@AppPage
        }

        key(pagerModeKey) {
            val initialPage = pagerPhraseIds.indexOf(currentPhraseId).coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { pagerPhraseIds.size },
            )

            LaunchedEffect(pagerState, pagerPhraseIds) {
                snapshotFlow { pagerState.settledPage }
                    .distinctUntilChanged()
                    .collect { page ->
                        val id = pagerPhraseIds.getOrNull(page) ?: return@collect
                        if (id != viewModel.currentPhraseId.value) {
                            latestShowBrowsePhrase(id)
                        }
                    }
            }

            LaunchedEffect(currentPhraseId, pagerPhraseIds) {
                val target = pagerPhraseIds.indexOf(currentPhraseId)
                if (target >= 0 &&
                    target != pagerState.currentPage &&
                    !pagerState.isScrollInProgress
                ) {
                    pagerState.scrollToPage(target)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = if (browsePagerEnabled) 1 else 0,
                pageSpacing = HeroCardPagerPageSpacing,
                userScrollEnabled = pagerPhraseIds.size > 1,
                key = { page -> pagerPhraseIds[page] },
            ) { page ->
                val pagePhraseId = pagerPhraseIds[page]
                val pageState = detailCache[pagePhraseId]
                val isSettledCurrent =
                    pagePhraseId == currentPhraseId && page == pagerState.settledPage
                val composeContent =
                    page == pagerState.currentPage || page == pagerState.settledPage

                PhraseDetailPagerPage(
                    state = pageState,
                    isActivePage = isSettledCurrent,
                    composeContent = composeContent,
                    onPlay = viewModel::play,
                    onInspect = { showInspect = true },
                    onAdvancedEdit = { onAdvancedEdit(pagePhraseId) },
                    onSyncWithArashi = { viewModel.syncWithArashi(context) },
                    onRandom = { viewModel.openRandomPhrase(onRandomPhrase) },
                )
            }
        }

        if (showInspect && state != null) {
            PhraseInspectDialog(
                state = state!!,
                viewModel = viewModel,
                onDismiss = { showInspect = false },
            )
        }
    }
}

@Composable
private fun PhraseDetailPagerPage(
    state: PhraseDetailUi?,
    isActivePage: Boolean,
    composeContent: Boolean,
    onPlay: () -> Unit,
    onInspect: () -> Unit,
    onAdvancedEdit: () -> Unit,
    onSyncWithArashi: () -> Unit,
    onRandom: () -> Unit,
) {
    if (state == null || !composeContent) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val scrollState = rememberScrollState()
    HeroSwipeToRandomContainer(
        scrollState = scrollState,
        onRandom = if (isActivePage) onRandom else null,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            PhraseHeroCard(
                state = state,
                onPlay = onPlay,
                onInspect = if (isActivePage) onInspect else ({}),
                onAdvancedEdit = if (isActivePage) onAdvancedEdit else ({}),
                onSyncWithArashi = if (isActivePage) onSyncWithArashi else ({}),
            )
        }
    }
}

@Composable
private fun PhraseInspectDialog(
    state: PhraseDetailUi,
    viewModel: PhraseDetailViewModel,
    onDismiss: () -> Unit,
) {
    HibikiDialog(onDismissRequest = onDismiss) {
        Text(
            text = "Ispeziona scheda",
            style = MaterialTheme.typography.titleMedium,
            color = Cyberpunk.TextPrimary,
        )
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailSection(title = "Dettagli") {
                FieldValue("Raw", state.phrase.japaneseRaw)
                FieldValue("Data", state.createdLabel)
                FieldValue("Source", state.phrase.source.name)
                FieldValue(
                    "Trascrizione",
                    "${state.phrase.transcriptionModel} · v${state.phrase.transcriptionPromptVersion}",
                )
                FieldValue(
                    "Analisi",
                    "${state.phrase.analysisModel} · v${state.phrase.analysisPromptVersion}",
                )
                state.phrase.confidence?.let { FieldValue("Confidence", "${(it * 100).toInt()}%") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.phrase.verified,
                        onCheckedChange = viewModel::setVerified,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Cyberpunk.NeonLime,
                            uncheckedColor = Cyberpunk.TextMuted,
                        ),
                    )
                    Text("Verified", color = Cyberpunk.TextPrimary)
                }
            }
            DetailSection(title = "Testo") {
                PhraseEditField("日本語", state.japaneseEdit, viewModel::updateJapanese)
                PhraseEditField("かな", state.kana, viewModel::updateKana)
                PhraseEditField("Rōmaji", state.romaji, viewModel::updateRomaji)
                PhraseEditField("Traduzione letterale", state.literal, viewModel::updateLiteral, singleLine = false)
                PhraseEditField("Traduzione naturale", state.natural, viewModel::updateNatural, singleLine = false)
            }
            state.error?.let {
                Text(it, color = Cyberpunk.NeonMagenta, style = MaterialTheme.typography.bodyLarge)
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
                    loading = state.saving,
                )
                HibikiButton(
                    text = "RIGENERA",
                    onClick = viewModel::regenerate,
                    style = HibikiButtonStyles.Violet,
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = false,
                    loading = state.regenerating,
                )
            }
        }
        HibikiButton(
            text = "CHIUDI",
            onClick = onDismiss,
            style = HibikiButtonStyles.Cancel,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
