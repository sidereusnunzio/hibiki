package com.hibiki.data.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureApiKeyStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getApiKey(): String? = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }

    fun setApiKey(value: String) {
        prefs.edit().putString(KEY_API, value.trim()).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API).apply()
    }

    fun getAdminApiKey(): String? = prefs.getString(KEY_ADMIN, null)?.takeIf { it.isNotBlank() }

    fun setAdminApiKey(value: String) {
        prefs.edit().putString(KEY_ADMIN, value.trim()).apply()
    }

    fun clearAdminApiKey() {
        prefs.edit().remove(KEY_ADMIN).apply()
    }

    companion object {
        private const val FILE_NAME = "hibiki_secure_prefs"
        private const val KEY_API = "openai_api_key"
        private const val KEY_ADMIN = "openai_admin_api_key"
    }
}
