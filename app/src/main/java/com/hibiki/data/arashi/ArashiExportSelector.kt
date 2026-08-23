package com.hibiki.data.arashi

import com.hibiki.data.arashi.model.ArashiExportType
import com.hibiki.domain.model.Phrase
import java.time.Instant
import java.time.temporal.ChronoUnit

object ArashiExportTime {
    fun formatEpochMs(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).truncatedTo(ChronoUnit.SECONDS).toString()

    fun parseToEpochMs(iso: String): Long = Instant.parse(iso).toEpochMilli()
}

object ArashiExportSelector {
    fun select(
        phrases: List<Phrase>,
        type: ArashiExportType,
        lastSuccessfulExportAtMs: Long?,
    ): List<Phrase> {
        val effectiveType = effectiveType(type, lastSuccessfulExportAtMs)
        return if (effectiveType == ArashiExportType.PARTIAL && lastSuccessfulExportAtMs != null) {
            phrases.filter { it.updatedAt > lastSuccessfulExportAtMs }
        } else {
            phrases
        }
    }

    fun effectiveType(
        requested: ArashiExportType,
        lastSuccessfulExportAtMs: Long?,
    ): ArashiExportType {
        return if (requested == ArashiExportType.PARTIAL && lastSuccessfulExportAtMs == null) {
            ArashiExportType.FULL
        } else {
            requested
        }
    }
}
