package com.hibiki.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.hibiki.domain.model.ArashiSyncState
import com.hibiki.ui.theme.Cyberpunk

const val ARASHI_KANJI = "嵐"

@Composable
fun ArashiKanji(
    syncState: ArashiSyncState,
    modifier: Modifier = Modifier,
) {
    val color = when (syncState) {
        ArashiSyncState.DO_NOT_SYNC -> return
        ArashiSyncState.PENDING -> Cyberpunk.TextMuted
        ArashiSyncState.SYNCED -> Cyberpunk.TextPrimary
    }
    Text(
        text = ARASHI_KANJI,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
