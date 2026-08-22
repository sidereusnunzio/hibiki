package com.hibiki.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hibiki.data.crypto.SecureApiKeyStore
import com.hibiki.domain.model.ApiProviderId
import com.hibiki.domain.model.ApiSettings
import com.hibiki.domain.model.AppPreferences
import com.hibiki.domain.model.AudioFormat
import com.hibiki.domain.model.AudioMatchConfig
import com.hibiki.domain.model.AudioSettings
import com.hibiki.domain.model.BuiltInIds
import com.hibiki.domain.model.OverlayDisplayPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hibiki_settings",
)

class SettingsRepository(
    context: Context,
    private val apiKeyStore: SecureApiKeyStore,
) {
    private val dataStore = context.applicationContext.settingsDataStore
    private val json = Json { ignoreUnknownKeys = true }

    val preferences: Flow<AppPreferences> = dataStore.data.map { prefs ->
        AppPreferences(
            lastContextId = prefs[Keys.LAST_CONTEXT] ?: BuiltInIds.GENERAL,
            lastSubjectIds = decodeSubjectMap(prefs[Keys.LAST_SUBJECTS]),
            overlayX = prefs[Keys.OVERLAY_X] ?: 24,
            overlayY = prefs[Keys.OVERLAY_Y] ?: 180,
            overlayCollapsed = prefs[Keys.OVERLAY_COLLAPSED] ?: false,
            overlayDisplay = OverlayDisplayPrefs(
                showJapanese = prefs[Keys.SHOW_JAPANESE] ?: true,
                showKana = prefs[Keys.SHOW_KANA] ?: true,
                showRomaji = prefs[Keys.SHOW_ROMAJI] ?: false,
                showLiteral = prefs[Keys.SHOW_LITERAL] ?: false,
                showNatural = prefs[Keys.SHOW_NATURAL] ?: true,
            ),
            audio = AudioSettings(
                maxDurationSeconds = prefs[Keys.MAX_DURATION] ?: 10,
                saveAudio = prefs[Keys.SAVE_AUDIO] ?: true,
                audioFormat = AudioFormat.AAC,
                fingerprintThreshold = prefs[Keys.FINGERPRINT_THRESHOLD]
                    ?: AudioMatchConfig.DEFAULT_SIMILARITY_THRESHOLD,
                trimSilence = prefs[Keys.TRIM_SILENCE] ?: true,
            ),
            api = ApiSettings(
                providerId = prefs[Keys.API_PROVIDER] ?: ApiProviderId.OPENAI,
                transcriptionModel = prefs[Keys.TRANSCRIPTION_MODEL] ?: "gpt-transcribe",
                languageAnalysisModel = prefs[Keys.ANALYSIS_MODEL] ?: "gpt-4o-mini",
                hasApiKey = prefs[Keys.HAS_API_KEY] ?: !apiKeyStore.getApiKey().isNullOrBlank(),
            ),
        )
    }

    fun getApiKey(): String? = apiKeyStore.getApiKey()

    fun getAdminApiKey(): String? = apiKeyStore.getAdminApiKey()

    suspend fun setApiKey(value: String) {
        apiKeyStore.setApiKey(value)
        dataStore.edit { it[Keys.HAS_API_KEY] = value.isNotBlank() }
    }

    suspend fun clearApiKey() {
        apiKeyStore.clearApiKey()
        dataStore.edit { it[Keys.HAS_API_KEY] = false }
    }

    suspend fun setAdminApiKey(value: String) {
        apiKeyStore.setAdminApiKey(value)
        dataStore.edit { it[Keys.HAS_ADMIN_API_KEY] = value.isNotBlank() }
    }

    suspend fun clearAdminApiKey() {
        apiKeyStore.clearAdminApiKey()
        dataStore.edit { it[Keys.HAS_ADMIN_API_KEY] = false }
    }

    suspend fun setLastContext(contextId: String) {
        dataStore.edit { it[Keys.LAST_CONTEXT] = contextId }
    }

    suspend fun setLastSubject(contextId: String, subjectId: String?) {
        dataStore.edit { prefs ->
            val current = decodeSubjectMap(prefs[Keys.LAST_SUBJECTS]).toMutableMap()
            if (subjectId.isNullOrBlank()) {
                current.remove(contextId)
            } else {
                current[contextId] = subjectId
            }
            prefs[Keys.LAST_SUBJECTS] = json.encodeToString(current)
        }
    }

    suspend fun setOverlayPosition(x: Int, y: Int) {
        dataStore.edit {
            it[Keys.OVERLAY_X] = x
            it[Keys.OVERLAY_Y] = y
        }
    }

    suspend fun setOverlayCollapsed(collapsed: Boolean) {
        dataStore.edit { it[Keys.OVERLAY_COLLAPSED] = collapsed }
    }

    suspend fun setOverlayDisplay(prefs: OverlayDisplayPrefs) {
        dataStore.edit {
            it[Keys.SHOW_JAPANESE] = prefs.showJapanese
            it[Keys.SHOW_KANA] = prefs.showKana
            it[Keys.SHOW_ROMAJI] = prefs.showRomaji
            it[Keys.SHOW_LITERAL] = prefs.showLiteral
            it[Keys.SHOW_NATURAL] = prefs.showNatural
        }
    }

    suspend fun setAudioSettings(settings: AudioSettings) {
        dataStore.edit {
            it[Keys.MAX_DURATION] = settings.maxDurationSeconds
            it[Keys.SAVE_AUDIO] = settings.saveAudio
            it[Keys.FINGERPRINT_THRESHOLD] = settings.fingerprintThreshold
            it[Keys.TRIM_SILENCE] = settings.trimSilence
        }
    }

    suspend fun setApiSettings(settings: ApiSettings) {
        dataStore.edit {
            it[Keys.API_PROVIDER] = settings.providerId
            it[Keys.TRANSCRIPTION_MODEL] = settings.transcriptionModel
            it[Keys.ANALYSIS_MODEL] = settings.languageAnalysisModel
        }
    }

    private fun decodeSubjectMap(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
    }

    private object Keys {
        val LAST_CONTEXT = stringPreferencesKey("last_context")
        val LAST_SUBJECTS = stringPreferencesKey("last_subjects")
        val OVERLAY_X = intPreferencesKey("overlay_x")
        val OVERLAY_Y = intPreferencesKey("overlay_y")
        val OVERLAY_COLLAPSED = booleanPreferencesKey("overlay_collapsed")
        val SHOW_JAPANESE = booleanPreferencesKey("show_japanese")
        val SHOW_KANA = booleanPreferencesKey("show_kana")
        val SHOW_ROMAJI = booleanPreferencesKey("show_romaji")
        val SHOW_LITERAL = booleanPreferencesKey("show_literal")
        val SHOW_NATURAL = booleanPreferencesKey("show_natural")
        val MAX_DURATION = intPreferencesKey("max_duration")
        val SAVE_AUDIO = booleanPreferencesKey("save_audio")
        val FINGERPRINT_THRESHOLD = floatPreferencesKey("fingerprint_threshold")
        val TRIM_SILENCE = booleanPreferencesKey("trim_silence")
        val API_PROVIDER = stringPreferencesKey("api_provider")
        val TRANSCRIPTION_MODEL = stringPreferencesKey("transcription_model")
        val ANALYSIS_MODEL = stringPreferencesKey("analysis_model")
        val HAS_API_KEY = booleanPreferencesKey("has_api_key")
        val HAS_ADMIN_API_KEY = booleanPreferencesKey("has_admin_api_key")
    }
}
