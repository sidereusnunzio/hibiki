package com.hibiki.domain.model

sealed class AppError(open val userMessage: String) {
    data object OverlayPermissionMissing : AppError("Permesso overlay assente")
    data object MediaProjectionDenied : AppError("Cattura schermo/audio non autorizzata")
    data object AudioNotCapturable : AppError("Audio interno non catturabile")
    data object EmptyRecording : AppError("-")
    data object NoNetwork : AppError("Nessuna connessione")
    data object MissingApiKey : AppError("API key mancante")
    data object ApiAuth : AppError("Autenticazione API non valida")
    data object RateLimit : AppError("Limite di richieste API raggiunto")
    data object Timeout : AppError("Timeout della richiesta")
    data object InvalidJson : AppError("Risposta JSON non valida")
    data object TranscriptionFailed : AppError("Trascrizione non riuscita")
    data object AnalysisFailed : AppError("Analisi linguistica non riuscita")
    data object FileSaveFailed : AppError("Salvataggio file non riuscito")
    data object InsufficientStorage : AppError("Spazio insufficiente")
    data class Unknown(override val userMessage: String) : AppError(userMessage)

    companion object {
        fun fromThrowable(error: Throwable): AppError {
            return when (error) {
                is AppException -> error.error
                else -> Unknown(error.message?.takeIf { it.isNotBlank() } ?: "Errore inatteso")
            }
        }
    }
}

class AppException(val error: AppError, cause: Throwable? = null) : Exception(error.userMessage, cause)
