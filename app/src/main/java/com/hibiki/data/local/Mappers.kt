package com.hibiki.data.local

import com.hibiki.data.local.entity.AudioPrototypeEntity
import com.hibiki.data.local.entity.AudioSampleEntity
import com.hibiki.data.local.entity.ContextEntity
import com.hibiki.data.local.entity.PhraseEntity
import com.hibiki.data.local.entity.SubjectEntity
import com.hibiki.domain.model.ArashiSyncState
import com.hibiki.domain.model.AudioPrototype
import com.hibiki.domain.model.AudioSample
import com.hibiki.domain.model.Phrase
import com.hibiki.domain.model.PhraseSource
import com.hibiki.domain.model.StudyContext
import com.hibiki.domain.model.Subject

fun ContextEntity.toModel() = StudyContext(
    id = id,
    name = name,
    prompt = prompt,
    expectedLanguage = expectedLanguage,
    hasSubjects = hasSubjects,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
    imagePath = imagePath,
)

fun StudyContext.toEntity() = ContextEntity(
    id = id,
    name = name,
    prompt = prompt,
    expectedLanguage = expectedLanguage,
    hasSubjects = hasSubjects,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
    imagePath = imagePath,
)

fun SubjectEntity.toModel() = Subject(
    id = id,
    contextId = contextId,
    displayName = displayName,
    japaneseName = japaneseName,
    prompt = prompt,
    imagePath = imagePath,
    overlayEnabled = overlayEnabled,
)

fun Subject.toEntity() = SubjectEntity(
    id = id,
    contextId = contextId,
    displayName = displayName,
    japaneseName = japaneseName,
    prompt = prompt,
    imagePath = imagePath,
    overlayEnabled = overlayEnabled,
)

fun AudioSampleEntity.toModel() = AudioSample(
    id = id,
    audioPath = audioPath,
    audioFingerprint = audioFingerprint,
    durationMs = durationMs,
    japaneseRaw = japaneseRaw,
    confidence = confidence,
    transcriptionModel = transcriptionModel,
    transcriptionPromptVersion = transcriptionPromptVersion,
    createdAt = createdAt,
)

fun AudioSample.toEntity() = AudioSampleEntity(
    id = id,
    audioPath = audioPath,
    audioFingerprint = audioFingerprint,
    durationMs = durationMs,
    japaneseRaw = japaneseRaw,
    confidence = confidence,
    transcriptionModel = transcriptionModel,
    transcriptionPromptVersion = transcriptionPromptVersion,
    createdAt = createdAt,
)

fun AudioPrototypeEntity.toModel() = AudioPrototype(
    id = id,
    phraseId = phraseId,
    audioFingerprint = audioFingerprint,
    durationMs = durationMs,
    pcmPreview = pcmPreview,
    createdAt = createdAt,
)

fun AudioPrototype.toEntity() = AudioPrototypeEntity(
    id = id,
    phraseId = phraseId,
    audioFingerprint = audioFingerprint,
    durationMs = durationMs,
    pcmPreview = pcmPreview,
    createdAt = createdAt,
)

fun PhraseEntity.toModel(sample: AudioSampleEntity) = Phrase(
    id = id,
    audioSampleId = audioSampleId,
    contextId = contextId,
    subjectId = subjectId,
    audioPath = sample.audioPath,
    audioFingerprint = sample.audioFingerprint,
    durationMs = sample.durationMs,
    japaneseRaw = sample.japaneseRaw,
    japaneseCorrected = japaneseCorrected,
    kana = kana,
    romaji = romaji,
    literalTranslation = literalTranslation,
    naturalTranslation = naturalTranslation,
    confidence = sample.confidence,
    verified = verified,
    arashiSyncState = runCatching { ArashiSyncState.valueOf(arashiSyncState) }
        .getOrDefault(ArashiSyncState.DO_NOT_SYNC),
    source = runCatching { PhraseSource.valueOf(source) }.getOrDefault(PhraseSource.API),
    createdAt = createdAt,
    updatedAt = updatedAt,
    transcriptionModel = sample.transcriptionModel,
    analysisModel = analysisModel,
    transcriptionPromptVersion = sample.transcriptionPromptVersion,
    analysisPromptVersion = analysisPromptVersion,
)

fun Phrase.toEntity() = PhraseEntity(
    id = id,
    audioSampleId = audioSampleId,
    contextId = contextId,
    subjectId = subjectId,
    japaneseCorrected = japaneseCorrected,
    kana = kana,
    romaji = romaji,
    literalTranslation = literalTranslation,
    naturalTranslation = naturalTranslation,
    verified = verified,
    arashiSyncState = arashiSyncState.name,
    source = source.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    analysisModel = analysisModel,
    analysisPromptVersion = analysisPromptVersion,
)
