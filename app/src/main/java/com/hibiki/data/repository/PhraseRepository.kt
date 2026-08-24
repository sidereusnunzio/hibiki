package com.hibiki.data.repository

import com.hibiki.data.audio.AudioFileStore
import com.hibiki.data.local.HibikiDatabaseProvider
import com.hibiki.data.local.toEntity
import com.hibiki.data.local.toModel
import com.hibiki.domain.JapaneseTextNormalizer
import com.hibiki.data.audio.AudioNormalizer
import com.hibiki.data.audio.PcmPreviewCodec
import com.hibiki.data.audio.RecordedClip
import com.hibiki.domain.model.AudioMatchConfig
import com.hibiki.domain.model.AudioPrototype
import com.hibiki.domain.model.ArchiveFilters
import com.hibiki.domain.model.ArashiSyncState
import com.hibiki.domain.model.AudioSample
import com.hibiki.domain.model.LinguisticAnalysis
import com.hibiki.domain.model.Phrase
import java.util.UUID
import com.hibiki.domain.model.PhraseListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class PhraseRepository(
    private val databaseProvider: HibikiDatabaseProvider,
    private val audioFileStore: AudioFileStore,
) {
    private val phraseDao get() = databaseProvider.getDatabase().phraseDao()
    private val sampleDao get() = databaseProvider.getDatabase().audioSampleDao()
    private val prototypeDao get() = databaseProvider.getDatabase().audioPrototypeDao()
    private val contextDao get() = databaseProvider.getDatabase().contextDao()
    private val subjectDao get() = databaseProvider.getDatabase().subjectDao()

    fun observeCount(): Flow<Int> =
        databaseProvider.generation.flatMapLatest { phraseDao.observeCount() }

    fun observeAudioCount(): Flow<Int> =
        databaseProvider.generation.flatMapLatest { phraseDao.observeAudioCount() }

    fun observePhrase(id: String): Flow<Phrase?> =
        databaseProvider.generation.flatMapLatest {
            combine(
                phraseDao.observeById(id),
                sampleDao.observeAll(),
            ) { phrase, samples ->
                val sample = phrase?.let { row -> samples.find { it.id == row.audioSampleId } }
                if (phrase == null || sample == null) null else phrase.toModel(sample)
            }
        }

    fun observeArchive(filters: Flow<ArchiveFilters>): Flow<List<PhraseListItem>> {
        return databaseProvider.generation.flatMapLatest {
            combine(
                phraseDao.observeAll(),
                sampleDao.observeAll(),
                contextDao.observeAll(),
                subjectDao.observeAll(),
                filters,
            ) { phrases, samples, contexts, subjects, currentFilters ->
                val sampleById = samples.associateBy { it.id }
                val contextById = contexts.associateBy { it.id }
                val subjectById = subjects.associateBy { it.id }
                phrases
                    .mapNotNull { row -> sampleById[row.audioSampleId]?.let { row.toModel(it) } }
                    .filter { phrase -> matches(phrase, currentFilters) }
                    .map { phrase ->
                        val context = contextById[phrase.contextId]
                        val subject = phrase.subjectId?.let(subjectById::get)
                        PhraseListItem(
                            phrase = phrase,
                            contextName = context?.name ?: phrase.contextId,
                            contextImagePath = context?.imagePath,
                            subjectDisplayName = subject?.displayName,
                            subjectJapaneseName = subject?.japaneseName,
                            subjectImagePath = subject?.imagePath,
                        )
                    }
                    .let { items ->
                        if (currentFilters.newestFirst) items else items.reversed()
                    }
            }
        }
    }

    suspend fun getAll(): List<Phrase> {
        val sampleById = sampleDao.getAll().associateBy { it.id }
        return phraseDao.getAll().mapNotNull { row ->
            sampleById[row.audioSampleId]?.let { row.toModel(it) }
        }
    }

    suspend fun getById(id: String): Phrase? {
        val row = phraseDao.getById(id) ?: return null
        val sample = sampleDao.getById(row.audioSampleId) ?: return null
        return row.toModel(sample)
    }

    suspend fun samplesForFingerprint(): List<AudioSample> =
        sampleDao.getWithFingerprint().map { it.toModel() }

    suspend fun findAnalysis(audioSampleId: String, contextId: String, subjectId: String?): Phrase? {
        val row = phraseDao.findAnalysis(audioSampleId, contextId, subjectId) ?: return null
        val sample = sampleDao.getById(row.audioSampleId) ?: return null
        return row.toModel(sample)
    }

    suspend fun findDuplicateAnalysis(contextId: String, subjectId: String?, japaneseRaw: String): Phrase? {
        if (JapaneseTextNormalizer.normalize(japaneseRaw).isEmpty()) return null
        val sampleById = sampleDao.getAll().associateBy { it.id }
        return phraseDao.getAll()
            .asSequence()
            .filter { row -> sameSubject(row.contextId, row.subjectId, contextId, subjectId) }
            .mapNotNull { row -> sampleById[row.audioSampleId]?.let { row.toModel(it) } }
            .firstOrNull { phrase ->
                JapaneseTextNormalizer.areEquivalent(japaneseRaw, phrase.japaneseRaw) ||
                    phrase.japaneseCorrected?.let { JapaneseTextNormalizer.areEquivalent(japaneseRaw, it) } == true
            }
    }

    /** Target di matching: una entry per prototipo acustico di ogni Phrase nel contesto. */
    suspend fun getMatchTargets(contextId: String, subjectId: String?): List<PhraseMatchTarget> {
        val sampleById = sampleDao.getAll().associateBy { it.id }
        val phrases = phraseDao.getAll()
            .filter { row -> matchesContextAndSubject(row.contextId, row.subjectId, contextId, subjectId) }
            .mapNotNull { row -> sampleById[row.audioSampleId]?.let { row.toModel(it) } }
        return phrases.flatMap { phrase ->
            prototypeDao.getByPhrase(phrase.id).map { prototype ->
                PhraseMatchTarget(
                    phrase = phrase,
                    prototypeId = prototype.id,
                    fingerprint = prototype.audioFingerprint,
                    durationMs = prototype.durationMs,
                    pcmPreview = prototype.pcmPreview,
                    audioPath = phrase.audioPath,
                )
            }
        }
    }

    suspend fun addPrototypeFromRecording(
        phraseId: String,
        fingerprint: ByteArray,
        durationMs: Long,
        recorded: RecordedClip,
    ) {
        if (fingerprint.isEmpty()) return
        val pcmPreview = PcmPreviewCodec.encode(AudioNormalizer.toPreviewPcm(recorded.trimmed))
        prototypeDao.insert(
            AudioPrototype(
                id = UUID.randomUUID().toString(),
                phraseId = phraseId,
                audioFingerprint = fingerprint,
                durationMs = durationMs,
                pcmPreview = pcmPreview,
                createdAt = System.currentTimeMillis(),
            ).toEntity(),
        )
        trimPrototypes(phraseId)
    }

    fun hasCompleteAnalysis(phrase: Phrase): Boolean =
        phrase.kana.isNotBlank() &&
            phrase.romaji.isNotBlank() &&
            phrase.naturalTranslation.isNotBlank()

    private suspend fun trimPrototypes(phraseId: String) {
        val prototypes = prototypeDao.getByPhrase(phraseId).sortedBy { it.createdAt }
        val excess = prototypes.size - AudioMatchConfig.MAX_PROTOTYPES_PER_PHRASE
        if (excess <= 0) return
        prototypes.take(excess).forEach { prototypeDao.deleteById(it.id) }
    }

    suspend fun insertPhrase(phrase: Phrase, initialFingerprint: ByteArray?, pcmPreview: ByteArray?) {
        phraseDao.insert(phrase.toEntity())
        if (initialFingerprint != null && !initialFingerprint.isEmpty()) {
            prototypeDao.insert(
                AudioPrototype(
                    id = "${phrase.id}:p0",
                    phraseId = phrase.id,
                    audioFingerprint = initialFingerprint,
                    durationMs = phrase.durationMs,
                    pcmPreview = pcmPreview,
                    createdAt = phrase.createdAt,
                ).toEntity(),
            )
        }
    }

    suspend fun updateLinguisticFields(id: String, analysis: LinguisticAnalysis, japaneseCorrected: String?) {
        val current = phraseDao.getById(id) ?: return
        val now = System.currentTimeMillis()
        phraseDao.update(
            current.copy(
                japaneseCorrected = japaneseCorrected,
                kana = analysis.kana,
                romaji = analysis.romaji,
                literalTranslation = analysis.literalTranslation,
                naturalTranslation = analysis.naturalTranslation,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateManualFields(phrase: Phrase) {
        phraseDao.update(phrase.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun setVerified(id: String, verified: Boolean) {
        val current = phraseDao.getById(id) ?: return
        if (current.verified == verified) return
        phraseDao.update(current.copy(verified = verified, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setArashiSyncState(ids: Collection<String>, state: ArashiSyncState) {
        if (ids.isEmpty()) return
        phraseDao.setArashiSyncState(ids.toList(), state.name)
    }

    suspend fun delete(id: String) {
        val current = phraseDao.getById(id) ?: return
        val sampleId = current.audioSampleId
        phraseDao.deleteById(id)
        if (phraseDao.countBySample(sampleId) == 0) {
            val sample = sampleDao.getById(sampleId)
            sample?.audioPath?.let { audioFileStore.delete(it) }
            sampleDao.deleteById(sampleId)
        }
    }

    private fun sameSubject(
        storedContextId: String,
        storedSubjectId: String?,
        contextId: String,
        subjectId: String?,
    ): Boolean = matchesContextAndSubject(storedContextId, storedSubjectId, contextId, subjectId)

    /**
     * subjectId valorizzato → solo quel personaggio.
     * subjectId null → tutte le frasi del contesto (qualsiasi personaggio).
     */
    private fun matchesContextAndSubject(
        storedContextId: String,
        storedSubjectId: String?,
        contextId: String,
        subjectId: String?,
    ): Boolean =
        storedContextId == contextId && (subjectId == null || storedSubjectId == subjectId)

    suspend fun insertSample(sample: AudioSample) {
        sampleDao.insert(sample.toEntity())
    }

    private fun matches(phrase: Phrase, filters: ArchiveFilters): Boolean {
        if (filters.contextId != null && phrase.contextId != filters.contextId) return false
        if (filters.subjectId != null && phrase.subjectId != filters.subjectId) return false
        if (filters.verifiedOnly && !phrase.verified) return false
        val query = filters.query.trim()
        if (query.isEmpty()) return true
        val haystack = listOfNotNull(
            phrase.japaneseDisplay,
            phrase.japaneseRaw,
            phrase.kana,
            phrase.romaji,
            phrase.literalTranslation,
            phrase.naturalTranslation,
        ).joinToString("\n")
        return haystack.contains(query, ignoreCase = true)
    }
}
