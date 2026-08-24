package com.hibiki.data.arashi

import com.hibiki.domain.model.ArashiSyncState
import com.hibiki.domain.model.Phrase

object ArashiExportSelector {
    fun selectPending(phrases: List<Phrase>): List<Phrase> =
        phrases.filter { it.arashiSyncState == ArashiSyncState.PENDING }
}
