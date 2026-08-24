package com.hibiki.domain

import java.text.Normalizer

object JapaneseTextNormalizer {
    private val punctuation = Regex("[。、！？!?,\\.…〜~「」『』（）()\\[\\]\"'\\\\-―ー·｡､!?]+")

    fun normalize(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFKC)
            .replace(Regex("\\s+"), "")
            .replace(punctuation, "")

    fun areEquivalent(a: String, b: String, minSimilarity: Float = 0.90f): Boolean {
        val left = normalize(a)
        val right = normalize(b)
        if (left == right) return true
        if (left.isEmpty() || right.isEmpty()) return false
        if (similarityRatio(left, right) >= minSimilarity) return true
        val shorter = if (left.length <= right.length) left else right
        val longer = if (left.length <= right.length) right else left
        return longer.contains(shorter) && shorter.length.toFloat() / longer.length >= 0.85f
    }

    private fun similarityRatio(a: String, b: String): Float {
        if (a == b) return 1f
        val distance = levenshtein(a, b)
        return 1f - distance.toFloat() / maxOf(a.length, b.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in a.indices) {
            curr[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                curr[j + 1] = minOf(
                    curr[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + cost,
                )
            }
            for (j in prev.indices) prev[j] = curr[j]
        }
        return prev[b.length]
    }
}
