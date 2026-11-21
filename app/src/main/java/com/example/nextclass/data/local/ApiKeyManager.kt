package com.example.nextclass.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores the user's own Gemini API key, encrypted at rest. */
class ApiKeyManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getKey(): String = prefs.getString(KEY_API, "").orEmpty()

    fun hasKey(): Boolean = getKey().isNotBlank()

    fun saveKey(value: String) {
        prefs.edit().putString(KEY_API, value.trim()).apply()
    }

    fun clearKey() {
        prefs.edit().remove(KEY_API).apply()
    }

    private companion object {
        const val FILE = "nextclass_secure_prefs"
        const val KEY_API = "gemini_api_key"
    }
}
