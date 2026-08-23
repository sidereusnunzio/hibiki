package com.hibiki.data.arashi

import com.hibiki.data.arashi.model.ArashiImportResultDto

sealed class ArashiExportOutcome {
    data class Success(val result: ArashiImportResultDto) : ArashiExportOutcome()
    data object Cancelled : ArashiExportOutcome()
    data class Invalid(val message: String) : ArashiExportOutcome()
}

object ArashiExportResultInterpreter {
    fun interpret(
        resultCode: Int,
        resultJson: String?,
        errorMessage: String?,
        expectedExportId: String,
    ): ArashiExportOutcome {
        if (resultCode != ArashiExportContract.RESULT_OK) {
            val error = errorMessage?.trim().orEmpty()
            return if (error.isNotEmpty()) {
                ArashiExportOutcome.Invalid(error)
            } else {
                ArashiExportOutcome.Cancelled
            }
        }
        val raw = resultJson?.trim().orEmpty()
        if (raw.isEmpty()) {
            return ArashiExportOutcome.Invalid("Risultato di Arashi assente o non valido")
        }
        val parsed = runCatching { ArashiExportJson.decodeResult(raw) }.getOrElse {
            return ArashiExportOutcome.Invalid("Risultato di Arashi non valido")
        }
        if (parsed.exportId != expectedExportId) {
            return ArashiExportOutcome.Invalid("exportId non corrisponde all'export corrente")
        }
        if (parsed.received < 0 || parsed.imported < 0 || parsed.updated < 0 ||
            parsed.duplicates < 0 || parsed.failed < 0
        ) {
            return ArashiExportOutcome.Invalid("Risultato di Arashi non valido")
        }
        return ArashiExportOutcome.Success(parsed)
    }
}
