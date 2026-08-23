package com.hibiki.data.repository

import com.hibiki.data.local.HibikiDatabaseProvider
import com.hibiki.data.local.toEntity
import com.hibiki.data.local.toModel
import com.hibiki.data.media.ImageFileStore
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.AppException
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ContextRepository(
    private val databaseProvider: HibikiDatabaseProvider,
    private val imageFileStore: ImageFileStore,
) {
    private val contextDao get() = databaseProvider.getDatabase().contextDao()
    private val subjectDao get() = databaseProvider.getDatabase().subjectDao()
    private val phraseDao get() = databaseProvider.getDatabase().phraseDao()

    fun observeContexts(): Flow<List<StudyContext>> =
        databaseProvider.generation.flatMapLatest {
            contextDao.observeAll().map { rows -> rows.map { it.toModel() } }
        }

    suspend fun getContext(id: String): StudyContext? =
        contextDao.getById(id)?.toModel()

    suspend fun createContext(
        name: String,
        prompt: String,
        expectedLanguage: String,
        hasSubjects: Boolean,
        imagePath: String? = null,
    ): StudyContext {
        val entity = StudyContext(
            id = slug(name),
            name = name.trim(),
            prompt = prompt.trim(),
            expectedLanguage = expectedLanguage.trim().ifBlank { "ja" },
            hasSubjects = hasSubjects,
            isBuiltIn = false,
            sortOrder = 100,
            imagePath = imagePath,
        ).toEntity()
        contextDao.insert(entity)
        return entity.toModel()
    }

    suspend fun updateContext(context: StudyContext) {
        contextDao.update(context.toEntity())
    }

    suspend fun deleteContext(id: String) {
        val existing = contextDao.getById(id) ?: return
        if (existing.isBuiltIn) {
            throw AppException(AppError.Unknown("I contesti predefiniti non si possono eliminare"))
        }
        if (phraseDao.countByContext(id) > 0) {
            throw AppException(AppError.Unknown("Il contesto ha frasi archiviate"))
        }
        val subjects = subjectDao.getByContext(id)
        if (subjects.isNotEmpty()) {
            throw AppException(AppError.Unknown("Elimina prima i personaggi del contesto"))
        }
        imageFileStore.delete(existing.imagePath)
        contextDao.deleteIfNotBuiltIn(id)
    }

    fun observeSubjects(contextId: String, query: String = ""): Flow<List<Subject>> {
        return databaseProvider.generation.flatMapLatest {
            val trimmed = query.trim()
            val flow = if (trimmed.isEmpty()) {
                subjectDao.observeByContext(contextId)
            } else {
                subjectDao.observeByContextFiltered(contextId, trimmed)
            }
            flow.map { rows -> rows.map { it.toModel() } }
        }
    }

    suspend fun getSubjects(contextId: String): List<Subject> =
        subjectDao.getByContext(contextId).map { it.toModel() }

    suspend fun getSubject(id: String): Subject? =
        subjectDao.getById(id)?.toModel()

    suspend fun saveSubject(subject: Subject) {
        val id = subject.id.ifBlank { slug(subject.displayName) }
        val previous = subjectDao.getById(id)
        if (previous?.imagePath != null && previous.imagePath != subject.imagePath) {
            imageFileStore.delete(previous.imagePath)
        }
        subjectDao.upsert(subject.copy(id = id).toEntity())
    }

    suspend fun deleteSubject(id: String) {
        if (phraseDao.countBySubject(id) > 0) {
            throw AppException(AppError.Unknown("Il personaggio ha frasi archiviate"))
        }
        val existing = subjectDao.getById(id)
        imageFileStore.delete(existing?.imagePath)
        subjectDao.deleteById(id)
    }

    private fun slug(value: String): String {
        val base = value.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "item" }
        return "${base}_${UUID.randomUUID().toString().take(6)}"
    }
}
