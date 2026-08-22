package com.hibiki.domain

import com.hibiki.domain.model.DefaultPrompts
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject

object TranscriptionPromptBuilder {
    fun build(context: StudyContext, subject: Subject?): String {
        val extra = extraBlocks(context, subject)
        return buildString {
            if (extra.isNotBlank()) {
                appendLine(extra)
                appendLine()
            }
            append(DefaultPrompts.CORE_TRANSCRIPTION_RULES)
        }.trim()
    }

    fun analysisUserMessage(
        japanese: String,
        context: StudyContext? = null,
        subject: Subject? = null,
    ): String {
        return buildString {
            appendLine("Frase giapponese:")
            appendLine(japanese.trim())
            if (context != null) {
                val contextPrompt = context.prompt.trim()
                if (contextPrompt.isNotBlank()) {
                    appendLine()
                    appendLine("Contesto:")
                    appendLine(contextPrompt)
                }
                if (subject != null) {
                    appendLine()
                    appendLine("Personaggio:")
                    appendLine("${subject.displayName} (${subject.japaneseName})")
                    val subjectPrompt = subject.prompt.trim()
                    if (subjectPrompt.isNotBlank()) {
                        appendLine()
                        appendLine("Contesto del personaggio:")
                        appendLine(subjectPrompt)
                    }
                }
            }
        }.trim()
    }

    private fun extraBlocks(context: StudyContext, subject: Subject?): String {
        return buildString {
            val contextPrompt = context.prompt.trim()
            if (contextPrompt.isNotBlank()) {
                appendLine(contextPrompt)
            }
            if (subject != null) {
                if (isNotEmpty()) appendLine()
                appendLine("Personaggio che parla: ${subject.displayName} (${subject.japaneseName}).")
                val subjectPrompt = subject.prompt.trim()
                if (subjectPrompt.isNotBlank()) {
                    appendLine()
                    appendLine("Contesto del personaggio:")
                    appendLine(subjectPrompt)
                }
            }
            if (isNotEmpty()) {
                appendLine()
                append(DefaultPrompts.SUBJECT_DISAMBIGUATION)
            }
        }.trim()
    }
}
