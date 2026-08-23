package com.hibiki.ui.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveBrowseSessionTest {
    @Test
    fun notInSession_whenExcludedIdMissing() {
        assertEquals(
            BrowseSessionRandomPick.NotInSession,
            pickRandomFromSession(listOf("a", "b"), excludeId = "c"),
        )
    }

    @Test
    fun stayInSession_whenOnlyOneItem() {
        assertEquals(
            BrowseSessionRandomPick.StayInSession,
            pickRandomFromSession(listOf("a"), excludeId = "a"),
        )
    }

    @Test
    fun picksAnotherIdFromSession() {
        val pick = pickRandomFromSession(listOf("a", "b", "c"), excludeId = "b")
        assertTrue(pick is BrowseSessionRandomPick.Id)
        assertTrue((pick as BrowseSessionRandomPick.Id).id in setOf("a", "c"))
    }
}
