package com.hibiki.data.repository

import com.hibiki.domain.model.Phrase

data class PhraseMatchTarget(
    val phrase: Phrase,
    val prototypeId: String,
    val fingerprint: ByteArray?,
    val durationMs: Long,
    val pcmPreview: ByteArray?,
    val audioPath: String?,
)
