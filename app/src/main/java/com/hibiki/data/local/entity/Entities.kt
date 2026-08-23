package com.hibiki.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "contexts")
data class ContextEntity(
    @PrimaryKey val id: String,
    val name: String,
    val prompt: String,
    val expectedLanguage: String,
    val hasSubjects: Boolean,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
    val imagePath: String? = null,
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = ContextEntity::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("contextId")],
)
data class SubjectEntity(
    @PrimaryKey val id: String,
    val contextId: String,
    val displayName: String,
    val japaneseName: String,
    val prompt: String,
    val imagePath: String? = null,
)

@Entity(tableName = "audio_samples")
data class AudioSampleEntity(
    @PrimaryKey val id: String,
    val audioPath: String?,
    val audioFingerprint: ByteArray?,
    val durationMs: Long,
    val japaneseRaw: String,
    val confidence: Float?,
    val transcriptionModel: String,
    val transcriptionPromptVersion: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "phrases",
    foreignKeys = [
        ForeignKey(
            entity = AudioSampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["audioSampleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ContextEntity::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("audioSampleId"),
        Index("contextId"),
        Index("subjectId"),
        Index("createdAt"),
        Index("updatedAt"),
    ],
)
data class PhraseEntity(
    @PrimaryKey val id: String,
    val audioSampleId: String,
    val contextId: String,
    val subjectId: String?,
    val japaneseCorrected: String?,
    val kana: String,
    val romaji: String,
    val literalTranslation: String,
    val naturalTranslation: String,
    val verified: Boolean,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long,
    val analysisModel: String,
    val analysisPromptVersion: Int,
)
