package com.hibiki.ui.archive

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ordered phrase IDs from the archive list (current filters) for swipe and random navigation.
 */
class ArchiveBrowseSession {
    private val _phraseIds = MutableStateFlow<List<String>>(emptyList())
    val phraseIds: StateFlow<List<String>> = _phraseIds.asStateFlow()

    fun updatePhraseIds(ids: List<String>) {
        if (_phraseIds.value != ids) {
            _phraseIds.value = ids
        }
    }

    fun randomPhraseId(excludeId: String): BrowseSessionRandomPick =
        pickRandomFromSession(_phraseIds.value, excludeId)
}

sealed interface BrowseSessionRandomPick {
    data class Id(val id: String) : BrowseSessionRandomPick
    data object StayInSession : BrowseSessionRandomPick
    data object NotInSession : BrowseSessionRandomPick
}

internal fun pickRandomFromSession(
    ids: List<String>,
    excludeId: String,
): BrowseSessionRandomPick {
    if (excludeId !in ids) return BrowseSessionRandomPick.NotInSession
    val candidates = ids.filter { it != excludeId }
    val nextId = candidates.randomOrNull() ?: return BrowseSessionRandomPick.StayInSession
    return BrowseSessionRandomPick.Id(nextId)
}
