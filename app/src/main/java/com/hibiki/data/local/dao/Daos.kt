package com.hibiki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.hibiki.data.local.entity.AudioPrototypeEntity
import com.hibiki.data.local.entity.AudioSampleEntity
import com.hibiki.data.local.entity.ContextEntity
import com.hibiki.data.local.entity.PhraseEntity
import com.hibiki.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextDao {
    @Query("SELECT * FROM contexts ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<ContextEntity>>

    @Query("SELECT * FROM contexts ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<ContextEntity>

    @Query("SELECT * FROM contexts WHERE id = :id")
    suspend fun getById(id: String): ContextEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ContextEntity)

    @Update
    suspend fun update(entity: ContextEntity)

    @Query("DELETE FROM contexts WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteIfNotBuiltIn(id: String): Int

    @Query("SELECT imagePath FROM contexts WHERE imagePath IS NOT NULL AND imagePath != ''")
    suspend fun getAllImagePaths(): List<String>
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY displayName ASC")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY displayName ASC")
    suspend fun getAll(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE contextId = :contextId ORDER BY displayName ASC")
    fun observeByContext(contextId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE contextId = :contextId ORDER BY displayName ASC")
    suspend fun getByContext(contextId: String): List<SubjectEntity>

    @Query(
        """
        SELECT * FROM subjects
        WHERE contextId = :contextId
          AND (displayName LIKE '%' || :query || '%' OR japaneseName LIKE '%' || :query || '%')
        ORDER BY displayName ASC
        """,
    )
    fun observeByContextFiltered(contextId: String, query: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: String): SubjectEntity?

    @Upsert
    suspend fun upsert(entity: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT imagePath FROM subjects WHERE imagePath IS NOT NULL AND imagePath != ''")
    suspend fun getAllImagePaths(): List<String>

    @Query("UPDATE subjects SET imagePath = :imagePath WHERE id = :id")
    suspend fun updateImagePath(id: String, imagePath: String)
}

@Dao
interface AudioPrototypeDao {
    @Query("SELECT * FROM audio_prototypes WHERE phraseId = :phraseId ORDER BY createdAt ASC")
    suspend fun getByPhrase(phraseId: String): List<AudioPrototypeEntity>

    @Query("SELECT COUNT(*) FROM audio_prototypes WHERE phraseId = :phraseId")
    suspend fun countByPhrase(phraseId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AudioPrototypeEntity)

    @Query("DELETE FROM audio_prototypes WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AudioSampleDao {
    @Query("SELECT * FROM audio_samples")
    fun observeAll(): Flow<List<AudioSampleEntity>>

    @Query("SELECT * FROM audio_samples")
    suspend fun getAll(): List<AudioSampleEntity>

    @Query("SELECT * FROM audio_samples WHERE audioFingerprint IS NOT NULL")
    suspend fun getWithFingerprint(): List<AudioSampleEntity>

    @Query("SELECT * FROM audio_samples WHERE id = :id")
    suspend fun getById(id: String): AudioSampleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AudioSampleEntity)

    @Query("DELETE FROM audio_samples WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT audioPath FROM audio_samples WHERE audioPath IS NOT NULL AND audioPath != ''")
    suspend fun getAllAudioPaths(): List<String>
}

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PhraseEntity>>

    @Query("SELECT * FROM phrases ORDER BY createdAt DESC")
    suspend fun getAll(): List<PhraseEntity>

    @Query("SELECT * FROM phrases WHERE id = :id")
    suspend fun getById(id: String): PhraseEntity?

    @Query("SELECT * FROM phrases WHERE id = :id")
    fun observeById(id: String): Flow<PhraseEntity?>

    @Query(
        """
        SELECT * FROM phrases
        WHERE audioSampleId = :audioSampleId
          AND contextId = :contextId
          AND ((:subjectId IS NULL AND subjectId IS NULL) OR subjectId = :subjectId)
        LIMIT 1
        """,
    )
    suspend fun findAnalysis(
        audioSampleId: String,
        contextId: String,
        subjectId: String?,
    ): PhraseEntity?

    @Query(
        """
        SELECT * FROM phrases
        WHERE audioSampleId = :audioSampleId
          AND contextId = :contextId
        LIMIT 1
        """,
    )
    suspend fun findAnalysisInContext(
        audioSampleId: String,
        contextId: String,
    ): PhraseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PhraseEntity)

    @Update
    suspend fun update(entity: PhraseEntity)

    @Query("UPDATE phrases SET arashiSyncState = :state WHERE id IN (:ids)")
    suspend fun setArashiSyncState(ids: List<String>, state: String)

    @Query("DELETE FROM phrases WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM phrases")
    fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM phrases
        INNER JOIN audio_samples ON audio_samples.id = phrases.audioSampleId
        WHERE audio_samples.audioPath IS NOT NULL AND audio_samples.audioPath != ''
        """,
    )
    fun observeAudioCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM phrases WHERE subjectId = :subjectId")
    suspend fun countBySubject(subjectId: String): Int

    @Query("SELECT COUNT(*) FROM phrases WHERE contextId = :contextId")
    suspend fun countByContext(contextId: String): Int

    @Query("SELECT COUNT(*) FROM phrases WHERE audioSampleId = :audioSampleId")
    suspend fun countBySample(audioSampleId: String): Int
}
