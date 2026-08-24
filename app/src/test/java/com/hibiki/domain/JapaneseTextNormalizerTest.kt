package com.hibiki.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JapaneseTextNormalizerTest {
    @Test
    fun trimsAndCollapsesWhitespace() {
        assertEquals("やるじゃねえか", JapaneseTextNormalizer.normalize("  やるじゃねえか！  "))
        assertEquals("やるじゃねえか", JapaneseTextNormalizer.normalize("やるじゃ\nねえか！"))
    }

    @Test
    fun ignoresPunctuationDifferences() {
        assertTrue(JapaneseTextNormalizer.areEquivalent("やるじゃねえか！", "やるじゃねえか"))
        assertTrue(JapaneseTextNormalizer.areEquivalent("今日は、いい天気。", "今日はいい天気"))
    }

    @Test
    fun rejectsClearlyDifferentPhrases() {
        assertFalse(JapaneseTextNormalizer.areEquivalent("おはよう", "こんばんは"))
    }
}
