package com.personx.hermatic.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString("api_key", apiKey).apply()
    }

    fun getApiKey(): String? {
        return sharedPreferences.getString("api_key", null)
    }

    fun getDatabasePassphrase(): ByteArray {
        val key = sharedPreferences.getString("db_passphrase", null)
        return if (key != null) {
            key.toByteArray()
        } else {
            val newKey = java.util.UUID.randomUUID().toString()
            sharedPreferences.edit().putString("db_passphrase", newKey).apply()
            newKey.toByteArray()
        }
    }

    fun setSelfDestructPeriod(periodMs: Long) {
        sharedPreferences.edit().putLong("self_destruct_period", periodMs).apply()
    }

    fun getSelfDestructPeriod(): Long {
        // Default to 24 hours (24 * 60 * 60 * 1000)
        // 0 means disabled
        return sharedPreferences.getLong("self_destruct_period", 0L)
    }
}
