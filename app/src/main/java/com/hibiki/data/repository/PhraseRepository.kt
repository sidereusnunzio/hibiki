package com.hibiki.data.repository

import com.hibiki.data.audio.AudioFileStore
import com.hibiki.data.local.HibikiDatabaseProvider
import com.hibiki.data.local.toEntity
import com.hibiki.data.local.toModel
import com.hibiki.domain.model.ArchiveFilters
import com.hibiki.domain.model.AudioSample
import com.hibiki.domain.model.LinguisticAnalysis
import com.hibiki.domain.model.Phrase
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

    suspend fun insertSample(sample: AudioSample) {
        sampleDao.insert(sample.toEntity())
    }

    suspend fun insertPhrase(phrase: Phrase) {
        phraseDao.insert(phrase.toEntity())
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
