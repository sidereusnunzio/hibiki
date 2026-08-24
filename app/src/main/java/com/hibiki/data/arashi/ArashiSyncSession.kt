package com.hibiki.data.arashi

import android.content.Context
import android.content.Intent
import com.hibiki.data.arashi.model.ArashiImportResultDto
import com.hibiki.data.repository.PhraseRepository
import com.hibiki.data.repository.SettingsRepository
import com.hibiki.domain.model.ArashiSyncState
import com.hibiki.domain.model.LastArashiExport
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

sealed interface ArashiSyncOutcome {
    data object Cancelled : ArashiSyncOutcome
    data class Success(val summary: String, val phraseCount: Int) : ArashiSyncOutcome
    data class Error(val message: String) : ArashiSyncOutcome
}

class ArashiSyncSession(
    private val arashiExportService: ArashiExportService,
    private val phraseRepository: PhraseRepository,
    private val settingsRepository: SettingsRepository,
) {
    private var pendingPackage: ArashiExportPackage? = null

    private val _launch = MutableSharedFlow<Intent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val launch: SharedFlow<Intent> = _launch.asSharedFlow()

    suspend fun startPendingExport(context: Context): ArashiSyncOutcome? {
        return startExport(context, phraseIds = null)
    }

    suspend fun startSinglePhraseExport(context: Context, phraseId: String): ArashiSyncOutcome? {
        return startExport(context, phraseIds = listOf(phraseId))
    }

    fun onActivityResult(
        context: Context,
        resultCode: Int,
        resultJson: String?,
        errorMessage: String?,
    ): ArashiActivityResult {
        val pending = pendingPackage
        val expectedId = pending?.exportId.orEmpty()
        val outcome = ArashiExportResultInterpreter.interpret(
            resultCode = resultCode,
            resultJson = resultJson,
            errorMessage = errorMessage,
            expectedExportId = expectedId,
        )
        clearPendingPackage(context)
        return when (outcome) {
            ArashiExportOutcome.Cancelled -> ArashiActivityResult.Done(ArashiSyncOutcome.Cancelled)
            is ArashiExportOutcome.Invalid -> ArashiActivityResult.Done(ArashiSyncOutcome.Error(outcome.message))
            is ArashiExportOutcome.Success -> {
                if (pending == null) {
                    ArashiActivityResult.Done(
                        ArashiSyncOutcome.Success(
                            summary = formatSummary(outcome.result),
                            phraseCount = outcome.result.received,
                        ),
                    )
                } else {
                    ArashiActivityResult.SuccessPending(pending, outcome.result)
                }
            }
        }
    }

    suspend fun finalizeSuccess(pending: ArashiExportPackage, result: ArashiImportResultDto): ArashiSyncOutcome.Success {
        phraseRepository.setArashiSyncState(pending.exportedPhraseIds, ArashiSyncState.SYNCED)
        settingsRepository.setLastArashiExport(
            LastArashiExport(
                exportedAtEpochMs = pending.exportedAtEpochMs,
                exportId = pending.exportId,
                phraseCount = pending.phraseCount,
                exportType = pending.exportType,
            ),
        )
        return ArashiSyncOutcome.Success(
            summary = formatSummary(result),
            phraseCount = pending.phraseCount,
        )
    }

    fun onLaunchFailed(context: Context, error: Throwable): ArashiSyncOutcome {
        clearPendingPackage(context)
        return ArashiSyncOutcome.Error(
            error.message ?: "Arashi non è installata o l'import non è disponibile",
        )
    }

    fun clearPending(context: Context) {
        clearPendingPackage(context)
    }

    private suspend fun startExport(context: Context, phraseIds: List<String>?): ArashiSyncOutcome? {
        clearPendingPackage(context)
        val outputDir = File(context.cacheDir, "arashi_export")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "hibiki-arashi-export.zip")
        return try {
            if (!ArashiExportIntents.isArashiImportAvailable(context)) {
                return ArashiSyncOutcome.Error("Arashi non è installata o l'import non è disponibile")
            }
            val created = arashiExportService.createPackage(
                outputFile = outputFile,
                phraseIds = phraseIds,
            )
            val uri = ArashiExportIntents.fileProviderUri(context, created.file)
            context.grantUriPermission(
                ArashiExportContract.ARASHI_PACKAGE,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            pendingPackage = created
            val launched = _launch.tryEmit(ArashiExportIntents.importIntent(uri, created.exportId))
            if (!launched) {
                clearPendingPackage(context)
                ArashiSyncOutcome.Error("Impossibile aprire Arashi")
            } else {
                null
            }
        } catch (error: ArashiExportException) {
            outputFile.delete()
            ArashiSyncOutcome.Error(error.message ?: "Export fallito")
        } catch (error: Exception) {
            outputFile.delete()
            ArashiSyncOutcome.Error(error.message ?: "Export verso Arashi fallito")
        }
    }

    private fun clearPendingPackage(context: Context) {
        pendingPackage?.let { pending ->
            runCatching {
                val uri = ArashiExportIntents.fileProviderUri(context, pending.file)
                context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pending.file.delete()
        }
        pendingPackage = null
    }

    private fun formatSummary(result: ArashiImportResultDto): String {
        return buildString {
            append("Export verso Arashi completato")
            append('\n')
            append("${result.imported} nuove frasi importate")
            append('\n')
            append("${result.updated} frasi aggiornate")
            append('\n')
            append("${result.duplicates} già presenti")
            append('\n')
            append("${result.failed} ${if (result.failed == 1) "errore" else "errori"}")
        }
    }
}

sealed interface ArashiActivityResult {
    data class Done(val outcome: ArashiSyncOutcome) : ArashiActivityResult
    data class SuccessPending(
        val pending: ArashiExportPackage,
        val result: ArashiImportResultDto,
    ) : ArashiActivityResult
}
