package com.hibiki.data.api

import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.AppException
import com.hibiki.domain.model.LinguisticAnalysis
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class TranscriptionResult(
    val text: String,
    val confidence: Float? = null,
)

interface TranscriptionProvider {
    val id: String
    suspend fun transcribe(audioFile: File, prompt: String, language: String): TranscriptionResult
}

interface LanguageAnalysisProvider {
    val id: String
    suspend fun analyze(japanese: String): LinguisticAnalysis
    suspend fun testConnection(transcriptionModel: String, analysisModel: String)
}

fun mapHttpError(code: Int, body: String): AppException {
    val message = body.take(240)
    return when (code) {
        401, 403 -> AppException(AppError.ApiAuth)
        429 -> AppException(AppError.RateLimit)
        else -> AppException(AppError.Unknown("Errore API $code${if (message.isBlank()) "" else ": $message"}"))
    }
}

fun mapNetworkError(error: Throwable): AppException {
    if (error is AppException) return error
    return when (error) {
        is SocketTimeoutException -> AppException(AppError.Timeout, error)
        is UnknownHostException -> AppException(AppError.NoNetwork, error)
        is IOException -> {
            val text = error.message.orEmpty()
            when {
                text.contains("Unable to resolve host", ignoreCase = true) -> AppException(AppError.NoNetwork, error)
                text.contains("timeout", ignoreCase = true) -> AppException(AppError.Timeout, error)
                else -> AppException(AppError.Unknown(text.ifBlank { "Errore di rete" }), error)
            }
        }
        else -> AppException(AppError.Unknown(error.message ?: "Errore inatteso"), error)
    }
}
