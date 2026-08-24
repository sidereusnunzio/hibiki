package com.hibiki.data.audio

data class RecordedClip(
    val trimmed: PcmClip,
    val raw: PcmClip,
)
