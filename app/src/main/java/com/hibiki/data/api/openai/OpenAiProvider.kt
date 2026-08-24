package com.hibiki.data.api.openai

import com.hibiki.data.api.LanguageAnalysisProvider
import com.hibiki.data.api.TranscriptionProvider
import com.hibiki.data.api.TranscriptionResult
import com.hibiki.data.api.mapHttpError
import com.hibiki.data.api.mapNetworkError
import com.hibiki.domain.model.ApiProviderId
import com.hibiki.domain.model.AppError
import com.hibiki.domain.model.AppException
import com.hibiki.domain.model.DefaultPrompts
import com.hibiki.domain.model.LinguisticAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class OpenAiProvider(
    private val apiKeyProvider: () -> String?,
    private val transcriptionModelProvider: () -> String,
    private val analysisModelProvider: () -> String,
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : TranscriptionProvider, LanguageAnalysisProvider {

    override val id: String = ApiProviderId.OPENAI

    override suspend fun transcribe(
        audioFile: File,
        prompt: String,
        language: String,
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        val apiKey = requireApiKey()
        val model = transcriptionModelProvider().trim().ifBlank { "gpt-transcribe" }
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaType()),
            )
            .addFormDataPart("model", model)
            .addFormDataPart("prompt", prompt)
            .addFormDataPart("language", language)
        val request = Request.Builder()
            .url("$BASE_URL/audio/transcriptions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(builder.build())
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw mapHttpError(response.code, body)
                val parsed = json.decodeFromString(TranscriptionResponse.serializer(), body)
                val text = parsed.text?.trim().orEmpty()
                if (text.isBlank()) throw AppException(AppError.TranscriptionFailed)
                TranscriptionResult(text = text)
            }
        } catch (error: Exception) {
            throw mapNetworkError(error)
        }
    }

    override suspend fun analyze(japanese: String): LinguisticAnalysis = withContext(Dispatchers.IO) {
        val apiKey = requireApiKey()
        val model = analysisModelProvider().trim().ifBlank { "gpt-4o-mini" }
        val payload = json.encodeToString(
            ChatCompletionRequest.serializer(),
            ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = DefaultPrompts.LANGUAGE_ANALYSIS),
                    ChatMessage(
                        role = "user",
                        content = japanese,
                    ),
                ),
                responseFormat = ChatResponseFormat(
                    type = "json_schema",
                    jsonSchema = JsonSchemaWrapper(
                        name = "linguistic_analysis",
                        strict = true,
                        schema = linguisticSchema(),
                    ),
                ),
            ),
        )
        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON))
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw mapHttpError(response.code, body)
                val completion = json.decodeFromString(ChatCompletionResponse.serializer(), body)
                val content = completion.choices.firstOrNull()?.message?.content
                    ?: throw AppException(AppError.InvalidJson)
                runCatching {
                    json.decodeFromString(LinguisticAnalysisDto.serializer(), content)
                }.getOrElse {
                    throw AppException(AppError.InvalidJson, it)
                }.toModel()
            }
        } catch (error: Exception) {
            if (error is AppException && error.error == AppError.InvalidJson) throw error
            throw mapNetworkError(error)
        }
    }

    override suspend fun testConnection(
        transcriptionModel: String,
        analysisModel: String,
    ) = withContext(Dispatchers.IO) {
        val apiKey = requireApiKey()
        val request = Request.Builder()
            .url("$BASE_URL/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw mapHttpError(response.code, body)
                val models = json.decodeFromString(ModelsListResponse.serializer(), body)
                val ids = models.data.map { it.id }.toSet()
                val missing = listOf(transcriptionModel, analysisModel)
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it !in ids }
                if (missing.isNotEmpty()) {
                    throw AppException(AppError.Unknown("Modello non disponibile: ${missing.joinToString()}"))
                }
            }
        } catch (error: Exception) {
            throw mapNetworkError(error)
        }
    }

    private fun requireApiKey(): String {
        val key = apiKeyProvider()?.trim().orEmpty()
        if (key.isBlank()) throw AppException(AppError.MissingApiKey)
        return key
    }

    companion object {
        private const val BASE_URL = "https://api.openai.com/v1"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        fun linguisticSchema(): JsonObject = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                stringProperty("kana", "Lettura completa della frase in kana, con spazi tra le parole")
                stringProperty(
                    "romaji",
                    "Romanizzazione Hepburn modificata con macron per le vocali lunghe, con spazi tra le parole",
                )
                stringProperty(
                    "literalTranslation",
                    "Glossa in ordine giapponese; particelle come [tema], [oggetto], [verso]; NON italiano naturale",
                )
                stringProperty(
                    "naturalTranslation",
                    "Traduzione italiana naturale che preservi significato, registro e tono",
                )
            }
            put(
                "required",
                kotlinx.serialization.json.JsonArray(
                    listOf("kana", "romaji", "literalTranslation", "naturalTranslation")
                        .map { kotlinx.serialization.json.JsonPrimitive(it) },
                ),
            )
            put("additionalProperties", false)
        }

        private fun kotlinx.serialization.json.JsonObjectBuilder.stringProperty(name: String, description: String) {
            putJsonObject(name) {
                put("type", "string")
                put("description", description)
            }
        }
    }
}

@Serializable
private data class TranscriptionResponse(
    val text: String? = null,
)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("response_format") val responseFormat: ChatResponseFormat,
)

@Serializable
private data class ChatResponseFormat(
    val type: String,
    @SerialName("json_schema") val jsonSchema: JsonSchemaWrapper,
)

@Serializable
private data class JsonSchemaWrapper(
    val name: String,
    val strict: Boolean,
    val schema: JsonObject,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String? = null,
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage? = null,
)

@Serializable
private data class LinguisticAnalysisDto(
    val kana: String,
    val romaji: String,
    val literalTranslation: String,
    val naturalTranslation: String,
) {
    fun toModel() = LinguisticAnalysis(
        kana = kana.trim(),
        romaji = romaji.trim(),
        literalTranslation = literalTranslation.trim(),
        naturalTranslation = naturalTranslation.trim(),
    )
}

@Serializable
private data class ModelsListResponse(
    val data: List<ModelEntry> = emptyList(),
)

@Serializable
private data class ModelEntry(
    val id: String,
)
