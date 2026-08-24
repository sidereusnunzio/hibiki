package com.hibiki.data.audio

import com.hibiki.domain.model.AudioMatchConfig
import kotlin.math.abs
import kotlin.math.roundToInt

data class AudioAlignmentResult(
    val alignmentScore: Float,
    /** Frazione della clip di riferimento coperta dallo spezzone trovato (informativa, non gate). */
    val matchedCoverage: Float,
)

/**
 * Matching per spezzone: estrae un segmento dalla zona sonora della nuova registrazione
 * (intorno a metà / poco dopo metà della regione attiva) e lo cerca nella clip d'archivio
 * con sliding offset. La durata totale delle due clip non è un criterio di decisione.
 */
object AudioAligner {
    fun findMidSegmentMatch(newClip: PcmClip, referenceClip: PcmClip): AudioAlignmentResult? {
        val query = AudioNormalizer.toMatchPcm(newClip)
        val reference = AudioNormalizer.toMatchPcm(referenceClip)
        if (query.samples.isEmpty() || reference.samples.isEmpty()) return null

        val probe = extractMidProbe(query.samples, query.sampleRate) ?: return null
        if (probe.size > reference.samples.size) {
            return slideMatch(needle = reference.samples, haystack = query.samples, sampleRate = query.sampleRate)
        }
        return slideMatch(needle = probe, haystack = reference.samples, sampleRate = query.sampleRate)
    }

    fun bestMidSegmentMatch(newClips: List<PcmClip>, referenceClip: PcmClip): AudioAlignmentResult? {
        return newClips.mapNotNull { findMidSegmentMatch(it, referenceClip) }
            .maxByOrNull { it.alignmentScore }
    }

    /**
     * Spezzone centrato sulla regione ad energia (non sulla durata grezza record/stop),
     * leggermente dopo metà di quella regione.
     */
    fun extractMidProbe(samples: ShortArray, sampleRate: Int): ShortArray? {
        if (samples.isEmpty() || sampleRate <= 0) return null
        val minProbe = (sampleRate * AudioMatchConfig.PROBE_MIN_MS / 1000L).toInt().coerceAtLeast(1)
        val targetProbe = (sampleRate * AudioMatchConfig.PROBE_DURATION_MS / 1000L).toInt().coerceAtLeast(minProbe)
        if (samples.size < minProbe) return samples.copyOf()
        val probeLen = targetProbe.coerceAtMost(samples.size)
        val center = activeRegionCenter(samples, sampleRate)
        val start = (center - probeLen / 2).coerceIn(0, samples.size - probeLen)
        return samples.copyOfRange(start, start + probeLen)
    }

    /** Indice (sample) poco dopo metà della regione attiva. */
    fun activeRegionCenter(samples: ShortArray, sampleRate: Int): Int {
        val (activeStart, activeEnd) = findActiveRegion(samples, sampleRate)
        val activeLen = (activeEnd - activeStart).coerceAtLeast(1)
        return (activeStart + activeLen * AudioMatchConfig.PROBE_CENTER_RATIO)
            .roundToInt()
            .coerceIn(0, samples.lastIndex)
    }

    private fun findActiveRegion(samples: ShortArray, sampleRate: Int): Pair<Int, Int> {
        val window = (sampleRate * 20 / 1000).coerceAtLeast(1) // 20 ms
        val energies = ArrayList<Double>((samples.size / window) + 1)
        var i = 0
        while (i < samples.size) {
            val end = (i + window).coerceAtMost(samples.size)
            var sum = 0.0
            for (j in i until end) sum += abs(samples[j].toDouble())
            energies += sum / (end - i)
            i = end
        }
        if (energies.isEmpty()) return 0 to samples.size
        val peak = energies.maxOrNull() ?: return 0 to samples.size
        if (peak < 1.0) return 0 to samples.size
        val threshold = peak * AudioMatchConfig.ACTIVE_ENERGY_RATIO
        val first = energies.indexOfFirst { it >= threshold }.coerceAtLeast(0)
        val last = energies.indexOfLast { it >= threshold }.coerceAtLeast(first)
        val start = first * window
        val end = ((last + 1) * window).coerceAtMost(samples.size)
        return start to end.coerceAtLeast(start + 1)
    }

    private fun slideMatch(
        needle: ShortArray,
        haystack: ShortArray,
        sampleRate: Int,
    ): AudioAlignmentResult? {
        if (needle.isEmpty() || haystack.isEmpty() || needle.size > haystack.size) return null
        val hop = (sampleRate * AudioMatchConfig.SEGMENT_SEARCH_HOP_MS / 1000L)
            .toInt()
            .coerceAtLeast(1)
        val maxOffset = haystack.size - needle.size
        var bestScore = 0.0
        var bestOffset = 0
        var offset = 0
        while (offset <= maxOffset) {
            val score = AudioNormalizer.normalizedCrossCorrelation(needle, haystack, offset)
            if (score > bestScore) {
                bestScore = score
                bestOffset = offset
            }
            offset += hop
        }
        val refineFrom = (bestOffset - hop).coerceAtLeast(0)
        val refineTo = (bestOffset + hop).coerceAtMost(maxOffset)
        for (fine in refineFrom..refineTo) {
            val score = AudioNormalizer.normalizedCrossCorrelation(needle, haystack, fine)
            if (score > bestScore) bestScore = score
        }
        return AudioAlignmentResult(
            alignmentScore = bestScore.toFloat().coerceIn(0f, 1f),
            matchedCoverage = (needle.size.toFloat() / haystack.size.toFloat()).coerceIn(0f, 1f),
        )
    }
}
