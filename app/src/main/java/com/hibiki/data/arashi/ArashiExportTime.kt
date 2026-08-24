package com.hibiki.data.arashi

import java.time.Instant
import java.time.temporal.ChronoUnit

object ArashiExportTime {
    fun formatEpochMs(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).truncatedTo(ChronoUnit.SECONDS).toString()

    fun parseToEpochMs(iso: String): Long = Instant.parse(iso).toEpochMilli()
}
