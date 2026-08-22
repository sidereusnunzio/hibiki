package com.hibiki.data.audio

import com.hibiki.domain.model.AudioMatchConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sin

object AudioFingerprinter {
    private const val TARGET_RATE = 16_000
    private const val FRAME_SIZE = 2048
    private const val HOP = 512
    private const val BANDS = 32
    private const val MIN_HZ = 300.0
    private const val MAX_HZ = 3000.0

    fun compute(clip: PcmClip): ByteArray {
        val mono = downsample(clip, TARGET_RATE)
        if (mono.size < FRAME_SIZE) return ByteArray(0)
        val window = hannWindow(FRAME_SIZE)
        val bandEdges = logBandEdges(FRAME_SIZE, TARGET_RATE)
        val energies = mutableListOf<DoubleArray>()
        var offset = 0
        val re = DoubleArray(FRAME_SIZE)
        val im = DoubleArray(FRAME_SIZE)
        while (offset + FRAME_SIZE <= mono.size) {
            for (i in 0 until FRAME_SIZE) {
                re[i] = mono[offset + i] * window[i]
                im[i] = 0.0
            }
            fft(re, im)
            val bands = DoubleArray(BANDS)
            for (band in 0 until BANDS) {
                val start = bandEdges[band]
                val end = bandEdges[band + 1]
                var sum = 0.0
                for (bin in start until end) {
                    val real = re[bin]
                    val imag = im[bin]
                    sum += real * real + imag * imag
                }
                bands[band] = log10(sum + 1e-9)
            }
            energies += bands
            offset += HOP
        }
        return packBits(energies)
    }

    fun durationCompatible(aMs: Long, bMs: Long): Boolean {
        val delta = kotlin.math.abs(aMs - bMs)
        val longer = maxOf(aMs, bMs)
        val tolerance = maxOf(
            AudioMatchConfig.DURATION_TOLERANCE_MIN_MS,
            (longer * AudioMatchConfig.DURATION_TOLERANCE_RATIO).toLong(),
        )
        return delta <= tolerance
    }

    fun similarity(a: ByteArray, b: ByteArray): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val (left, right) = align(a, b)
        val bits = min(left.size, right.size) * 8
        if (bits == 0) return 0f
        var distance = 0
        for (i in 0 until min(left.size, right.size)) {
            distance += Integer.bitCount((left[i].toInt() xor right[i].toInt()) and 0xFF)
        }
        return 1f - distance.toFloat() / bits.toFloat()
    }

    private fun align(a: ByteArray, b: ByteArray): Pair<ByteArray, ByteArray> {
        if (a.size == b.size) return a to b
        return if (a.size < b.size) {
            a to b.copyOfRange(0, a.size)
        } else {
            a.copyOfRange(0, b.size) to b
        }
    }

    private fun downsample(clip: PcmClip, targetRate: Int): DoubleArray {
        val step = clip.sampleRate.toDouble() / targetRate
        if (step <= 1.0) {
            return DoubleArray(clip.samples.size) { clip.samples[it] / 32768.0 }
        }
        val outSize = (clip.samples.size / step).toInt()
        val out = DoubleArray(outSize.coerceAtLeast(0))
        var src = 0.0
        for (i in out.indices) {
            val index = src.toInt().coerceAtMost(clip.samples.lastIndex)
            out[i] = clip.samples[index] / 32768.0
            src += step
        }
        return out
    }

    private fun hannWindow(size: Int): DoubleArray =
        DoubleArray(size) { i -> 0.5 - 0.5 * cos(2.0 * PI * i / (size - 1)) }

    private fun logBandEdges(frameSize: Int, sampleRate: Int): IntArray {
        val nyquist = sampleRate / 2.0
        val binHz = sampleRate.toDouble() / frameSize
        val edges = IntArray(BANDS + 1)
        val minLog = ln(MIN_HZ)
        val maxLog = ln(min(MAX_HZ, nyquist - 1))
        for (i in 0..BANDS) {
            val hz = kotlin.math.exp(minLog + (maxLog - minLog) * i / BANDS)
            edges[i] = (hz / binHz).toInt().coerceIn(1, frameSize / 2 - 1)
        }
        for (i in 1..BANDS) {
            if (edges[i] <= edges[i - 1]) edges[i] = (edges[i - 1] + 1).coerceAtMost(frameSize / 2)
        }
        return edges
    }

    private fun packBits(energies: List<DoubleArray>): ByteArray {
        if (energies.size < 2) return ByteArray(0)
        val bits = BooleanArray((energies.size - 1) * BANDS)
        var bitIndex = 0
        for (t in 1 until energies.size) {
            val prev = energies[t - 1]
            val current = energies[t]
            for (band in 0 until BANDS) {
                val neighbor = current[(band + 1).coerceAtMost(BANDS - 1)]
                bits[bitIndex++] = (current[band] - prev[band]) > (neighbor - current[band])
            }
        }
        val bytes = ByteArray((bits.size + 7) / 8)
        for (i in bits.indices) {
            if (bits[i]) {
                bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (7 - i % 8))).toByte()
            }
        }
        return bytes
    }

    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wlenRe = cos(angle)
            val wlenIm = sin(angle)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * wRe - im[i + k + len / 2] * wIm
                    val vIm = re[i + k + len / 2] * wIm + im[i + k + len / 2] * wRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe
                    im[i + k + len / 2] = uIm - vIm
                    val nextWRe = wRe * wlenRe - wIm * wlenIm
                    wIm = wRe * wlenIm + wIm * wlenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
