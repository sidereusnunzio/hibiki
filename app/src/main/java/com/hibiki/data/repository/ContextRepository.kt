package com.hibiki.data.repository

import com.hibiki.data.local.HibikiDatabase
import com.hibiki.data.local.toEntity
import com.hibiki.data.local.toModel
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.AppException
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ContextRepository(
    private val database: HibikiDatabase,
) {
    private val contextDao = database.contextDao()
    private val subjectDao = database.subjectDao()
    private val phraseDao = database.phraseDao()

    fun observeContexts(): Flow<List<StudyContext>> =
        contextDao.observeAll().map { rows -> rows.map { it.toModel() } }

    suspend fun getContext(id: String): StudyContext? =
        contextDao.getById(id)?.toModel()

    suspend fun createContext(
        name: String,
        prompt: String,
        expectedLanguage: String,
        hasSubjects: Boolean,
    ): StudyContext {
        val entity = StudyContext(
            id = slug(name),
            name = name.trim(),
            prompt = prompt.trim(),
            expectedLanguage = expectedLanguage.trim().ifBlank { "ja" },
            hasSubjects = hasSubjects,
            isBuiltIn = false,
            sortOrder = 100,
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
        if (subjectDao.getByContext(id).isNotEmpty()) {
            throw AppException(AppError.Unknown("Elimina prima i personaggi del contesto"))
        }
        contextDao.deleteIfNotBuiltIn(id)
    }

    fun observeSubjects(contextId: String, query: String = ""): Flow<List<Subject>> {
        val trimmed = query.trim()
        val flow = if (trimmed.isEmpty()) {
            subjectDao.observeByContext(contextId)
        } else {
            subjectDao.observeByContextFiltered(contextId, trimmed)
        }
        return flow.map { rows -> rows.map { it.toModel() } }
    }

    suspend fun getSubjects(contextId: String): List<Subject> =
        subjectDao.getByContext(contextId).map { it.toModel() }

    suspend fun getSubject(id: String): Subject? =
        subjectDao.getById(id)?.toModel()

    suspend fun saveSubject(subject: Subject) {
        val id = subject.id.ifBlank { slug(subject.displayName) }
        subjectDao.upsert(subject.copy(id = id).toEntity())
    }

    suspend fun deleteSubject(id: String) {
        if (phraseDao.countBySubject(id) > 0) {
            throw AppException(AppError.Unknown("Il personaggio ha frasi archiviate"))
        }
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
