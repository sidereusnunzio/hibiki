package com.hibiki.ui.archive

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.ui.components.ArashiKanji
import com.hibiki.ui.components.ARASHI_KANJI
import com.hibiki.ui.components.SquareThumb
import com.hibiki.ui.theme.Cyberpunk

private val HeroCardContentHorizontalPadding = 20.dp
private val HeroActionColumnHorizontalInset = 22.dp
private val HeroCardLiteralTopSpacing = 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhraseHeroCard(
    state: PhraseDetailUi,
    onPlay: () -> Unit,
    onInspect: () -> Unit,
    onAdvancedEdit: () -> Unit,
    onSyncWithArashi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val japanese = state.phrase.japaneseDisplay
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Cyberpunk.PanelTranslucent,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = HeroCardContentHorizontalPadding + HeroActionColumnHorizontalInset,
                            end = HeroCardContentHorizontalPadding + HeroActionColumnHorizontalInset,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = japanese.ifBlank { "—" },
                            style = sentenceHeadlineStyle(japanese),
                            color = Cyberpunk.TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heroHeadlineTapActions(
                                    copyText = japanese,
                                    onPlay = state.phrase.audioPath?.let { { onPlay() } },
                                ),
                        )
                        if (state.phrase.kana.isNotBlank()) {
                            Text(
                                text = state.phrase.kana,
                                style = MaterialTheme.typography.titleMedium,
                                color = Cyberpunk.TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (state.phrase.romaji.isNotBlank()) {
                            Text(
                                text = state.phrase.romaji,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Cyberpunk.TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (state.phrase.literalTranslation.isNotBlank()) {
                            Text(
                                text = state.phrase.literalTranslation,
                                color = Cyberpunk.TextMuted,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = HeroCardLiteralTopSpacing),
                            )
                        }
                        if (state.phrase.naturalTranslation.isNotBlank()) {
                            Text(
                                text = state.phrase.naturalTranslation,
                                color = Cyberpunk.NeonLime,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    PhraseContextRow(state = state)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 4.dp, start = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "文",
                        style = MaterialTheme.typography.titleSmall,
                        color = Cyberpunk.TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
                if (state.phrase.audioPath != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onPlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Riproduci",
                            tint = Cyberpunk.TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PhraseHeroActionsMenu(
                    onInspect = onInspect,
                    onAdvancedEdit = onAdvancedEdit,
                    onSyncWithArashi = onSyncWithArashi,
                    syncingWithArashi = state.syncingWithArashi,
                )
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ArashiKanji(syncState = state.phrase.arashiSyncState)
                }
            }
        }
    }
}

@Composable
private fun PhraseContextRow(state: PhraseDetailUi) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SquareThumb(
            path = state.contextImagePath,
            contentDescription = state.contextName,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clickable {
                    showPromptToast(
                        context = context,
                        title = state.contextName,
                        prompt = state.contextPrompt,
                    )
                },
        )
        state.subject?.let { subject ->
            SquareThumb(
                path = state.subjectImagePath,
                contentDescription = subject.displayName,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clickable {
                        showPromptToast(
                            context = context,
                            title = "${subject.displayName} · ${subject.japaneseName}",
                            prompt = subject.prompt,
                        )
                    },
            )
        }
    }
}

private fun showPromptToast(context: android.content.Context, title: String, prompt: String) {
    val body = prompt.trim().ifBlank { "—" }
    Toast.makeText(context, "$title\n\n$body", Toast.LENGTH_LONG).show()
}

@Composable
private fun PhraseHeroActionsMenu(
    onInspect: () -> Unit,
    onAdvancedEdit: () -> Unit,
    onSyncWithArashi: () -> Unit,
    syncingWithArashi: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Azioni",
                tint = Cyberpunk.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Cyberpunk.PanelElevated,
        ) {
            DropdownMenuItem(
                text = { Text("Modifica avanzata", color = Cyberpunk.TextPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = Cyberpunk.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onAdvancedEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("Sincronizza con Arashi", color = Cyberpunk.TextPrimary) },
                enabled = !syncingWithArashi,
                leadingIcon = {
                    Text(
                        text = ARASHI_KANJI,
                        style = MaterialTheme.typography.titleSmall,
                        color = Cyberpunk.TextPrimary,
                    )
                },
                onClick = {
                    expanded = false
                    onSyncWithArashi()
                },
            )
            HorizontalDivider(color = Cyberpunk.GridLine)
            DropdownMenuItem(
                text = { Text("Ispeziona scheda", color = Cyberpunk.TextPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                        contentDescription = null,
                        tint = Cyberpunk.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onInspect()
                },
            )
        }
    }
}

@Composable
private fun sentenceHeadlineStyle(japanese: String): TextStyle {
    val base = when (japanese.length) {
        in 0..12 -> MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, letterSpacing = 0.sp)
        in 13..24 -> MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, letterSpacing = 0.sp)
        in 25..40 -> MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = 0.sp)
        else -> MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, letterSpacing = 0.sp)
    }
    return base.copy(lineHeight = base.fontSize * 1.15f)
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.heroHeadlineTapActions(
    copyText: String,
    onPlay: (() -> Unit)?,
): Modifier = composed {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    combinedClickable(
        onClick = { onPlay?.invoke() },
        onDoubleClick = {
            val text = copyText.trim()
            if (text.isEmpty()) return@combinedClickable
            clipboard.setText(AnnotatedString(text))
            Toast.makeText(context, "Copiato", Toast.LENGTH_SHORT).show()
        },
    )
}
