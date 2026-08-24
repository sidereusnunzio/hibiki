package com.hibiki.data.audio

object PcmPreviewCodec {
    fun encode(samples: ShortArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val value = samples[i].toInt()
            bytes[i * 2] = (value and 0xFF).toByte()
            bytes[i * 2 + 1] = (value shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    fun decode(bytes: ByteArray?): ShortArray? {
        if (bytes == null || bytes.isEmpty() || bytes.size % 2 != 0) return null
        return ShortArray(bytes.size / 2) { index ->
            val lo = bytes[index * 2].toInt() and 0xFF
            val hi = bytes[index * 2 + 1].toInt() shl 8
            (hi or lo).toShort()
        }
    }
}
