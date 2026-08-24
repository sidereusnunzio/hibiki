package com.hibiki.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hibiki.domain.model.OverlayStage
import com.hibiki.domain.model.OverlayUiState
import com.hibiki.domain.model.PhraseSource
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonColors
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.components.SectionLabel
import com.hibiki.ui.theme.Cyberpunk

private enum class OverlayMenu { Context, Subject }

private val OverlayBufferOff = HibikiButtonColors(
    containerColor = Cyberpunk.withAlpha(Cyberpunk.PanelElevated, 0.28f),
    contentColor = Cyberpunk.NeonLime,
)
private val OverlayBufferOn = HibikiButtonColors(
    containerColor = Cyberpunk.withAlpha(Cyberpunk.NeonLime, 0.55f),
    contentColor = Cyberpunk.Void,
)
private val OverlayBufferOffSolid = HibikiButtonColors(
    containerColor = Cyberpunk.PanelElevated,
    contentColor = Cyberpunk.NeonLime,
)
private val OverlayRecord = HibikiButtonColors(
    containerColor = Cyberpunk.withAlpha(Cyberpunk.NeonMagenta, 0.28f),
    contentColor = Cyberpunk.NeonMagenta,
)
private val OverlayRecordActive = HibikiButtonColors(
    containerColor = Cyberpunk.withAlpha(Cyberpunk.NeonMagenta, 0.45f),
    contentColor = Cyberpunk.NeonMagenta,
)

@Composable
fun OverlayPanel(
    state: OverlayUiState,
    onDrag: (dx: Int, dy: Int) -> Unit,
    onToggleCollapsed: () -> Unit,
    onSelectContext: (StudyContext) -> Unit,
    onSelectSubject: (Subject?) -> Unit,
    onListenToggle: () -> Unit,
    onBufferToggle: () -> Unit,
    onOpenPhrasePanel: () -> Unit,
    onCloseOverlay: () -> Unit,
    onCloseApp: () -> Unit,
    onCloseConfirmVisible: (Boolean) -> Unit,
    onDropdownFocus: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    var openMenu by remember { mutableStateOf<OverlayMenu?>(null) }
    var showCloseConfirm by remember { mutableStateOf(false) }
    fun setMenu(menu: OverlayMenu?) {
        openMenu = menu
        onDropdownFocus(menu != null || showCloseConfirm)
    }
    fun setCloseConfirm(visible: Boolean) {
        showCloseConfirm = visible
        onCloseConfirmVisible(visible)
        onDropdownFocus(visible || openMenu != null)
    }
    val hasSubjects = state.selectedContext?.hasSubjects == true
    LaunchedEffect(hasSubjects, state.collapsed) {
        if (state.collapsed || (!hasSubjects && openMenu == OverlayMenu.Subject)) {
            setMenu(null)
        }
    }
    if (showCloseConfirm) {
        OverlayCloseConfirm(
            listening = state.stage == OverlayStage.LISTENING,
            onDismiss = { setCloseConfirm(false) },
            onCloseOverlay = {
                setCloseConfirm(false)
                onCloseOverlay()
            },
            onCloseApp = {
                setCloseConfirm(false)
                onCloseApp()
            },
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (state.collapsed) Cyberpunk.withAlpha(Cyberpunk.Panel, 0.38f)
                    else Cyberpunk.PanelTranslucent,
                    shape,
                )
                .border(1.dp, Cyberpunk.GridLine, shape)
                .padding(if (state.collapsed) 4.dp else 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Sposta",
                    tint = Cyberpunk.MutedCyan,
                    modifier = Modifier
                        .size(if (state.collapsed) 20.dp else 24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                            }
                        },
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onToggleCollapsed,
                    modifier = if (state.collapsed) Modifier.size(32.dp) else Modifier,
                ) {
                    Icon(
                        imageVector = if (state.collapsed) Icons.Filled.Add else Icons.Filled.Remove,
                        contentDescription = if (state.collapsed) "Mostra configurazione" else "Nascondi configurazione",
                        tint = Cyberpunk.TextMuted,
                        modifier = Modifier.size(if (state.collapsed) 16.dp else 24.dp),
                    )
                }
                IconButton(
                    onClick = {
                        setMenu(null)
                        setCloseConfirm(true)
                    },
                    modifier = if (state.collapsed) Modifier.size(32.dp) else Modifier,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Spegni overlay",
                        tint = Cyberpunk.NeonMagenta,
                        modifier = Modifier.size(if (state.collapsed) 16.dp else 24.dp),
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
                        selectedLabel = state.selectedSubject?.let { subjectLabel(it) } ?: "Qualsiasi",
                        options = listOf("Qualsiasi" to null) +
                            state.subjects.map { subjectLabel(it) to it },
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
                onListenToggle = onListenToggle,
                onBufferToggle = onBufferToggle,
                onOpenPhrasePanel = onOpenPhrasePanel,
            )
        }
    }
}

@Composable
private fun OverlayCloseConfirm(
    listening: Boolean,
    onDismiss: () -> Unit,
    onCloseOverlay: () -> Unit,
    onCloseApp: () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cyberpunk.withAlpha(Cyberpunk.Void, 0.94f), shape)
            .border(1.dp, Cyberpunk.NeonMagenta, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Chiudere l'overlay?",
            color = Cyberpunk.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = buildString {
                append("Scegli se tornare alla home dell'app o chiudere del tutto.")
                if (listening) append(" L'ascolto in corso verrà interrotto.")
            },
            color = Cyberpunk.TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        HibikiButton(
            modifier = Modifier.fillMaxWidth(),
            text = "CHIUDI OVERLAY",
            onClick = onCloseOverlay,
            style = HibikiButtonStyles.Violet,
            fillMaxWidth = false,
        )
        HibikiButton(
            modifier = Modifier.fillMaxWidth(),
            text = "CHIUDI APP",
            onClick = onCloseApp,
            style = HibikiButtonStyles.Destructive,
            fillMaxWidth = false,
        )
        HibikiButton(
            modifier = Modifier.fillMaxWidth(),
            text = "ANNULLA",
            onClick = onDismiss,
            style = HibikiButtonStyles.Cancel,
            fillMaxWidth = false,
        )
    }
}

@Composable
private fun OverlayBody(
    state: OverlayUiState,
    onListenToggle: () -> Unit,
    onBufferToggle: () -> Unit,
    onOpenPhrasePanel: () -> Unit,
) {
    val busy = state.stage != OverlayStage.IDLE &&
        state.stage != OverlayStage.RESULT &&
        state.stage != OverlayStage.ERROR &&
        state.stage != OverlayStage.LISTENING
    val localMatch = isArchiveMatchOrigin(state.result?.origin) &&
        state.stage == OverlayStage.RESULT
    OverlayStatus(
        state = state,
        collapsed = state.collapsed,
        localMatch = localMatch,
    )
    if (!state.collapsed && state.result != null && !state.phrasePanelVisible && state.stage == OverlayStage.RESULT) {
        HibikiButton(
            text = "FRASE",
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Mostra frase",
            onClick = onOpenPhrasePanel,
            style = if (localMatch) HibikiButtonStyles.Primary else HibikiButtonStyles.Violet,
            fillMaxWidth = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
    }
    val listening = state.stage == OverlayStage.LISTENING
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (state.collapsed) 4.dp else 8.dp),
    ) {
        if (state.collapsed) {
            OverlayGlyphButton(
                icon = Icons.Filled.MoreTime,
                contentDescription = "Buffer",
                colors = if (state.bufferEnabled) OverlayBufferOn else OverlayBufferOff,
                onClick = onBufferToggle,
                enabled = !busy && !listening,
                modifier = Modifier.weight(1f),
            )
            OverlayGlyphButton(
                icon = if (listening) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                contentDescription = if (listening) "Stop" else "Listen",
                colors = if (listening) OverlayRecordActive else OverlayRecord,
                onClick = onListenToggle,
                enabled = !busy,
                loading = busy,
                modifier = Modifier.weight(1f),
            )
        } else {
            HibikiButton(
                text = "BUFFER",
                icon = Icons.Filled.MoreTime,
                contentDescription = "Buffer",
                onClick = onBufferToggle,
                style = if (state.bufferEnabled) HibikiButtonStyles.Lime else OverlayBufferOffSolid,
                enabled = !busy && !listening,
                fillMaxWidth = false,
                modifier = Modifier.weight(1f),
            )
            HibikiButton(
                text = if (listening) "STOP" else "LISTEN",
                icon = if (listening) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                contentDescription = if (listening) "Stop" else "Listen",
                onClick = onListenToggle,
                style = HibikiButtonStyles.Destructive,
                enabled = !busy,
                loading = busy,
                fillMaxWidth = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OverlayGlyphButton(
    icon: ImageVector,
    contentDescription: String,
    colors: HibikiButtonColors,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(2.dp)
    val container = if (enabled) colors.containerColor else Cyberpunk.withAlpha(Cyberpunk.PanelElevated, 0.18f)
    val content = if (enabled) colors.contentColor else Cyberpunk.withAlpha(Cyberpunk.TextMuted, 0.55f)
    Box(
        modifier = modifier
            .height(32.dp)
            .background(container, shape)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = content,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = content,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun OverlayStatus(
    state: OverlayUiState,
    collapsed: Boolean,
    localMatch: Boolean,
) {
    val apiHighlight = state.stage == OverlayStage.TRANSCRIBING || state.stage == OverlayStage.ANALYZING
    val statusColor = stageStatusColor(state, localMatch, apiHighlight)
    val statusText = when {
        collapsed -> collapsedStatusText(state)
        state.stage == OverlayStage.ERROR && !state.errorMessage.isNullOrBlank() -> null
        else -> stageLabel(state)
    }
    if (statusText != null) {
        val shape = RoundedCornerShape(2.dp)
        Text(
            text = statusText,
            color = statusColor,
            style = if (collapsed) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (apiHighlight) {
                        Modifier
                            .background(Cyberpunk.NeonViolet, shape)
                            .padding(horizontal = 10.dp, vertical = if (collapsed) 3.dp else 5.dp)
                    } else {
                        Modifier
                    },
                ),
        )
    }
    if (state.stage == OverlayStage.ERROR && state.errorMessage != null) {
        Text(
            text = state.errorMessage.orEmpty(),
            color = Cyberpunk.NeonMagenta,
            style = if (collapsed) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            maxLines = if (collapsed) 2 else Int.MAX_VALUE,
        )
    }
    if (statusText != null || state.stage == OverlayStage.ERROR) {
        Spacer(modifier = Modifier.height(if (collapsed) 4.dp else 8.dp))
    }
}

private fun stageStatusColor(state: OverlayUiState, localMatch: Boolean, apiHighlight: Boolean) = when {
    apiHighlight -> Cyberpunk.TextPrimary
    state.stage == OverlayStage.LISTENING -> Cyberpunk.NeonMagenta
    state.stage == OverlayStage.ERROR -> Cyberpunk.NeonMagenta
    state.stage == OverlayStage.RESULT && localMatch -> Cyberpunk.NeonCyan
    state.stage == OverlayStage.RESULT -> Cyberpunk.NeonViolet
    state.stage == OverlayStage.PROCESSING_AUDIO ||
        state.stage == OverlayStage.SEARCHING_LOCAL_ARCHIVE -> Cyberpunk.TextPrimary
    state.stage == OverlayStage.IDLE && state.bufferEnabled -> Cyberpunk.NeonLime
    state.stage == OverlayStage.IDLE -> Cyberpunk.TextMuted
    else -> Cyberpunk.TextPrimary
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

private fun isArchiveMatchOrigin(origin: PhraseSource?): Boolean =
    origin == PhraseSource.LOCAL_MATCH || origin == PhraseSource.TEXT_MATCH_AFTER_TRANSCRIPTION

private fun collapsedStatusText(state: OverlayUiState): String? = when (state.stage) {
    OverlayStage.IDLE -> null
    OverlayStage.LISTENING -> state.remainingSeconds?.let { "REC ${it}s" } ?: "REC"
    OverlayStage.ERROR -> null
    OverlayStage.RESULT -> null
    else -> stageLabel(state)
}

private fun stageLabel(state: OverlayUiState): String = when (state.stage) {
    OverlayStage.IDLE -> if (state.bufferEnabled) "BUFFER" else "IDLE"
    OverlayStage.LISTENING -> "LISTENING" + (state.remainingSeconds?.let { "  ${it}s" } ?: "")
    OverlayStage.PROCESSING_AUDIO -> "AUDIO"
    OverlayStage.SEARCHING_LOCAL_ARCHIVE -> "ARCHIVIO"
    OverlayStage.TRANSCRIBING -> "API"
    OverlayStage.ANALYZING -> "ANALISI API"
    OverlayStage.RESULT -> {
        val local = isArchiveMatchOrigin(state.result?.origin)
        val percent = ((state.result?.similarity ?: 0f) * 100).toInt()
        if (local && percent > 0) "IN ARCHIVIO  $percent%" else if (local) "IN ARCHIVIO" else "COMPLETATO"
    }
    OverlayStage.ERROR -> "ERROR"
}
