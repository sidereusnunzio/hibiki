package com.hibiki.domain.model

data class StudyContext(
    val id: String,
    val name: String,
    val prompt: String,
    val expectedLanguage: String,
    val hasSubjects: Boolean,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
)

data class Subject(
    val id: String,
    val contextId: String,
    val displayName: String,
    val japaneseName: String,
    val prompt: String = "",
)

enum class PhraseSource {
    API,
    LOCAL_MATCH,
}

data class AudioSample(
    val id: String,
    val audioPath: String?,
    val audioFingerprint: ByteArray?,
    val durationMs: Long,
    val japaneseRaw: String,
    val confidence: Float?,
    val transcriptionModel: String,
    val transcriptionPromptVersion: Int,
    val createdAt: Long,
)

data class Phrase(
    val id: String,
    val audioSampleId: String,
    val contextId: String,
    val subjectId: String?,
    val audioPath: String?,
    val audioFingerprint: ByteArray?,
    val durationMs: Long,
    val japaneseRaw: String,
    val japaneseCorrected: String?,
    val kana: String,
    val romaji: String,
    val literalTranslation: String,
    val naturalTranslation: String,
    val confidence: Float?,
    val verified: Boolean,
    val source: PhraseSource,
    val createdAt: Long,
    val transcriptionModel: String,
    val analysisModel: String,
    val transcriptionPromptVersion: Int,
    val analysisPromptVersion: Int,
) {
    val japaneseDisplay: String
        get() = japaneseCorrected?.takeIf { it.isNotBlank() } ?: japaneseRaw
}

data class LinguisticAnalysis(
    val kana: String,
    val romaji: String,
    val literalTranslation: String,
    val naturalTranslation: String,
)

data class PhraseListItem(
    val phrase: Phrase,
    val contextName: String,
    val subjectDisplayName: String?,
    val subjectJapaneseName: String?,
)

data class ArchiveFilters(
    val query: String = "",
    val contextId: String? = null,
    val subjectId: String? = null,
    val verifiedOnly: Boolean = false,
    val newestFirst: Boolean = true,
)

data class CaptureResult(
    val phrase: Phrase,
    val origin: PhraseSource,
    val similarity: Float? = null,
)

object BuiltInIds {
    const val GENERAL = "generale"
    const val UMAMUSUME = "umamusume"
}

object DefaultPrompts {
    const val TRANSCRIPTION_PROMPT_VERSION = 4
    const val ANALYSIS_PROMPT_VERSION = 4

    val CORE_TRANSCRIPTION_RULES = """
Trascrivi esattamente ciò che viene pronunciato in giapponese.
Non tradurre.
Non parafrasare.
Non correggere il registro.
Non aggiungere parole che non sono state pronunciate.
Restituisci esclusivamente il testo giapponese pronunciato.
""".trimIndent()

    val SUBJECT_DISAMBIGUATION = """
Il contesto serve esclusivamente ad aiutare a disambiguare l'audio.
Non dedurre o aggiungere parole sulla base del contesto o del profilo del personaggio.
Il profilo del personaggio aumenta la probabilità di riconoscere correttamente una forma udibile a esso compatibile; non va preferita in assenza di evidenza acustica.
""".trimIndent()

    val GENERAL = """
L'audio contiene parlato giapponese.
""".trimIndent()

    val UMAMUSUME = BuiltInUmamusume.CONTEXT_PROMPT

    val LANGUAGE_ANALYSIS = """
Sei un tutor di lingua giapponese per apprendenti italiani.

Ricevi una frase giapponese già trascritta. La frase fornita è immutabile e non devi restituirne una versione modificata.

Produci esclusivamente:

- kana: lettura completa in kana
- romaji: romanizzazione Hepburn modificata con macron per le vocali lunghe (ō, ū)
- literalTranslation: glossa in ordine giapponese; particelle e marche grammaticali come [tema], [oggetto], [verso], [interrogativo], [copula], ecc.; NON italiano naturale. Deve differire da naturalTranslation. Esempio: 私は学校へ行く → io [tema] scuola [verso] vado
- naturalTranslation: traduzione italiana naturale che preservi significato, registro e tono

Il contesto e il profilo del personaggio servono esclusivamente a interpretare registro, riferimenti, pronomi, nomi propri ed espressioni contestuali.

Non correggere o normalizzare mentalmente la frase prima di analizzarla.

Restituisci esclusivamente dati conformi allo Structured Output richiesto.
""".trimIndent()
}
