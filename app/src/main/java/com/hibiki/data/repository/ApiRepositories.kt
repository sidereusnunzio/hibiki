package com.hibiki.data.repository

import com.hibiki.data.api.LanguageAnalysisProvider
import com.hibiki.data.api.TranscriptionProvider
import com.hibiki.data.api.TranscriptionResult
import com.hibiki.domain.TranscriptionPromptBuilder
import com.hibiki.domain.model.LinguisticAnalysis
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import java.io.File

class TranscriptionRepository(
    private val provider: TranscriptionProvider,
) {
    suspend fun transcribe(audioFile: File, prompt: String, language: String): TranscriptionResult =
        provider.transcribe(audioFile, prompt, language)
}

class LanguageAnalysisRepository(
    private val provider: LanguageAnalysisProvider,
) {
    suspend fun analyze(
        japanese: String,
        context: StudyContext? = null,
        subject: Subject? = null,
    ): LinguisticAnalysis = provider.analyze(
        TranscriptionPromptBuilder.analysisUserMessage(japanese, context, subject),
    )

    suspend fun testConnection(transcriptionModel: String, analysisModel: String) {
        provider.testConnection(transcriptionModel, analysisModel)
    }
}
