package com.hibiki.domain.model

import com.hibiki.data.arashi.model.ArashiExportType

data class LastArashiExport(
    val exportedAtEpochMs: Long,
    val exportId: String,
    val phraseCount: Int,
    val exportType: ArashiExportType,
)
