package com.hibiki.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.hibiki.domain.model.CaptureResult
import com.hibiki.domain.model.OverlayDisplayPrefs
import com.hibiki.domain.model.OverlayStage
import com.hibiki.domain.model.OverlayUiState
import com.hibiki.domain.model.PhraseSource
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.components.SectionLabel
import com.hibiki.ui.theme.Cyberpunk

private enum class OverlayMenu { Context, Subject }

@Composable
fun OverlayPanel(
    state: OverlayUiState,
    displayPrefs: OverlayDisplayPrefs,
    onDrag: (dx: Int, dy: Int) -> Unit,
    onToggleCollapsed: () -> Unit,
    onSelectContext: (StudyContext) -> Unit,
    onSelectSubject: (Subject) -> Unit,
    onListenToggle: () -> Unit,
    onPlay: () -> Unit,
    onClose: () -> Unit,
    onDropdownFocus: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    var openMenu by remember { mutableStateOf<OverlayMenu?>(null) }
    fun setMenu(menu: OverlayMenu?) {
        openMenu = menu
        onDropdownFocus(menu != null)
    }
    val hasSubjects = state.selectedContext?.hasSubjects == true
    LaunchedEffect(hasSubjects, state.collapsed) {
        if (state.collapsed || (!hasSubjects && openMenu == OverlayMenu.Subject)) {
            setMenu(null)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cyberpunk.PanelTranslucent, shape)
            .border(1.dp, Cyberpunk.GridLine, shape)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Sposta",
                tint = Cyberpunk.MutedCyan,
                modifier = Modifier.pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                    }
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleCollapsed) {
                Icon(
                    imageVector = if (state.collapsed) Icons.Filled.Add else Icons.Filled.Remove,
                    contentDescription = if (state.collapsed) "Mostra configurazione" else "Nascondi configurazione",
                    tint = Cyberpunk.TextMuted,
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Spegni overlay",
                    tint = Cyberpunk.NeonMagenta,
                )
            }
        }
        if (!state.collapsed) {
            OverlaySelect(
                label = "Contesto",
                selectedLabel = state.selectedContext?.name ?: "Seleziona contesto",
                options = state.contexts.map { it.name to it },
                expanded = openMenu == OverlayMenu.Context,
                onToggle = { setMenu(if (openMenu == OverlayMenu.Context) null else OverlayMenu.Context) },
                onSelect = {
                    onSelectContext(it)
                    setMenu(null)
                },
            )
            if (hasSubjects) {
                Spacer(modifier = Modifier.height(8.dp))
                OverlaySelect(
                    label = "Personaggio",
                    selectedLabel = state.selectedSubject?.let { subjectLabel(it) } ?: "Seleziona personaggio",
                    options = state.subjects.map { subjectLabel(it) to it },
                    expanded = openMenu == OverlayMenu.Subject,
                    onToggle = { setMenu(if (openMenu == OverlayMenu.Subject) null else OverlayMenu.Subject) },
                    onSelect = {
                        onSelectSubject(it)
                        setMenu(null)
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OverlayBody(
            state = state,
            displayPrefs = displayPrefs,
            onListenToggle = onListenToggle,
            onPlay = onPlay,
        )
    }
}

@Composable
private fun OverlayBody(
    state: OverlayUiState,
    displayPrefs: OverlayDisplayPrefs,
    onListenToggle: () -> Unit,
    onPlay: () -> Unit,
) {
    val busy = state.stage != OverlayStage.IDLE &&
        state.stage != OverlayStage.RESULT &&
        state.stage != OverlayStage.ERROR &&
        state.stage != OverlayStage.LISTENING
    val localMatch = state.result?.origin == PhraseSource.LOCAL_MATCH &&
        state.stage == OverlayStage.RESULT
    Text(
        text = stageLabel(state),
        color = when {
            localMatch -> Cyberpunk.NeonLime
            state.stage == OverlayStage.LISTENING -> Cyberpunk.NeonMagenta
            state.stage == OverlayStage.ERROR -> Cyberpunk.NeonMagenta
            state.stage == OverlayStage.RESULT -> Cyberpunk.NeonCyan
            state.stage == OverlayStage.IDLE -> Cyberpunk.TextMuted
            else -> Cyberpunk.NeonViolet
        },
        style = MaterialTheme.typography.labelLarge,
    )
    if (state.stage == OverlayStage.ERROR) {
        Text(state.errorMessage.orEmpty(), color = Cyberpunk.NeonMagenta, style = MaterialTheme.typography.bodyMedium)
    }
    state.result?.let { OverlayResult(it, displayPrefs, onPlay) }
    Spacer(modifier = Modifier.height(8.dp))
    val listening = state.stage == OverlayStage.LISTENING
    HibikiButton(
        text = if (listening) "STOP" else "LISTEN",
        onClick = onListenToggle,
        style = if (listening) HibikiButtonStyles.Destructive else HibikiButtonStyles.Primary,
        enabled = !busy,
        loading = busy,
    )
}

@Composable
private fun OverlayResult(
    result: CaptureResult,
    displayPrefs: OverlayDisplayPrefs,
    onPlay: () -> Unit,
) {
    Spacer(modifier = Modifier.height(8.dp))
    if (result.origin == PhraseSource.LOCAL_MATCH) {
        val percent = ((result.similarity ?: 0f) * 100).toInt()
        SectionLabel("Origine")
        Text(
            text = "MATCH LOCALE  $percent%",
            color = Cyberpunk.NeonLime,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
    val phrase = result.phrase
    if (displayPrefs.showJapanese) OverlayField("日本語", phrase.japaneseDisplay)
    if (displayPrefs.showKana) OverlayField("かな", phrase.kana)
    if (displayPrefs.showRomaji) OverlayField("Rōmaji", phrase.romaji)
    if (displayPrefs.showLiteral) OverlayField("Traduzione letterale", phrase.literalTranslation)
    if (displayPrefs.showNatural) OverlayField("Traduzione", phrase.naturalTranslation)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (phrase.audioPath != null) {
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Cyberpunk.NeonCyan)
            }
        }
        Text("dettagli nell'archivio", color = Cyberpunk.TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OverlayField(label: String, value: String) {
    SectionLabel(label)
    Text(value, color = Cyberpunk.TextPrimary, style = MaterialTheme.typography.bodyLarge)
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun <T> OverlaySelect(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, T>>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (T) -> Unit,
) {
    val fieldShape = RoundedCornerShape(2.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Cyberpunk.Panel, fieldShape)
                .border(
                    1.dp,
                    if (expanded) Cyberpunk.NeonCyan else Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.35f),
                    fieldShape,
                )
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedLabel,
                color = Cyberpunk.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Cyberpunk.MutedCyan,
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(Cyberpunk.PanelElevated, fieldShape)
                    .border(1.dp, Cyberpunk.GridLine, fieldShape)
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEach { (name, value) ->
                    Text(
                        text = name,
                        color = if (name == selectedLabel) Cyberpunk.NeonCyan else Cyberpunk.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
                if (options.isEmpty()) {
                    Text(
                        text = "Nessuna voce",
                        color = Cyberpunk.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

private fun subjectLabel(subject: Subject): String {
    val japanese = subject.japaneseName.trim()
    return if (japanese.isBlank()) subject.displayName else "${subject.displayName} (${japanese})"
}

private fun stageLabel(state: OverlayUiState): String = when (state.stage) {
    OverlayStage.IDLE -> "IDLE"
    OverlayStage.LISTENING -> "LISTENING" + (state.remainingSeconds?.let { "  ${it}s" } ?: "")
    OverlayStage.PROCESSING_AUDIO -> "PROCESSING AUDIO"
    OverlayStage.SEARCHING_LOCAL_ARCHIVE -> "SEARCHING LOCAL ARCHIVE"
    OverlayStage.TRANSCRIBING -> "TRANSCRIBING"
    OverlayStage.ANALYZING -> "ANALYZING"
    OverlayStage.RESULT -> {
        val local = state.result?.origin == PhraseSource.LOCAL_MATCH
        val percent = ((state.result?.similarity ?: 0f) * 100).toInt()
        if (local) "MATCH LOCALE  $percent%" else "API"
    }
    OverlayStage.ERROR -> "ERROR"
}
