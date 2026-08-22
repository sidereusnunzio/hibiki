package com.hibiki.domain

import com.hibiki.domain.model.DefaultPrompts
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionPromptBuilderTest {
    private val umamusume = StudyContext(
        id = "umamusume",
        name = "Umamusume",
        prompt = DefaultPrompts.UMAMUSUME,
        expectedLanguage = "ja",
        hasSubjects = true,
        isBuiltIn = true,
        sortOrder = 1,
    )

    private val vodka = Subject(
        id = "vodka",
        contextId = "umamusume",
        displayName = "Vodka",
        japaneseName = "ウオッカ",
        prompt = """
Vodka parla generalmente in modo informale, energico e ruvido.
Può utilizzare forme colloquiali come ない → ねえ.
""".trimIndent(),
    )

    @Test
    fun generalPromptKeepsItalianCoreRules() {
        val context = StudyContext(
            id = "generale",
            name = "Generale",
            prompt = DefaultPrompts.GENERAL,
            expectedLanguage = "ja",
            hasSubjects = false,
            isBuiltIn = true,
            sortOrder = 0,
        )
        val prompt = TranscriptionPromptBuilder.build(context, null)
        assertTrue(prompt.startsWith("L'audio contiene parlato giapponese."))
        assertTrue(prompt.contains("Trascrivi esattamente ciò che viene pronunciato in giapponese."))
        assertTrue(prompt.contains("Non tradurre."))
        assertTrue(prompt.contains("Non correggere il registro."))
        assertFalse(prompt.contains("Personaggio che parla"))
    }

    @Test
    fun umamusumePromptMatchesItalianConvention() {
        val prompt = TranscriptionPromptBuilder.build(umamusume, vodka)
        assertTrue(prompt.startsWith("Audio proveniente dal videogioco Umamusume: Pretty Derby"))
        assertTrue(prompt.contains("Personaggio che parla: Vodka (ウオッカ)."))
        assertTrue(prompt.contains("Contesto del personaggio:"))
        assertTrue(prompt.contains("ない → ねえ"))
        assertTrue(prompt.contains("Trascrivi esattamente ciò che viene pronunciato in giapponese."))
        assertTrue(prompt.contains("non va preferita in assenza di evidenza acustica"))
        assertTrue(
            prompt.indexOf("Personaggio che parla") < prompt.indexOf("Trascrivi esattamente"),
        )
    }

    @Test
    fun analysisUserMessageKeepsJapaneseImmutableAndSeparatesMetadata() {
        val message = TranscriptionPromptBuilder.analysisUserMessage(
            japanese = "やるじゃねえか！",
            context = umamusume,
            subject = vodka,
        )
        assertTrue(message.startsWith("Frase giapponese:"))
        assertTrue(message.contains("やるじゃねえか！"))
        assertTrue(message.contains("Contesto:"))
        assertTrue(message.contains("Personaggio:"))
        assertTrue(message.contains("Vodka (ウオッカ)"))
        assertTrue(message.contains("ねえ"))
        assertFalse(message.contains("Trascrivi esattamente"))
        assertFalse(message.contains("Personaggio che parla"))
    }

    @Test
    fun analysisSystemPromptDoesNotRequestJapaneseOutput() {
        val prompt = DefaultPrompts.LANGUAGE_ANALYSIS
        assertTrue(prompt.contains("frase fornita è immutabile"))
        assertTrue(prompt.contains("- kana:"))
        assertTrue(prompt.contains("- romaji:"))
        assertTrue(prompt.contains("- literalTranslation:"))
        assertTrue(prompt.contains("ordine giapponese"))
        assertTrue(prompt.contains("NON italiano naturale"))
        assertTrue(prompt.contains("[tema]"))
        assertTrue(prompt.contains("- naturalTranslation:"))
        assertFalse(prompt.contains("- japanese:"))
        assertFalse(prompt.contains("comprensibile e utile allo studio"))
    }
}
