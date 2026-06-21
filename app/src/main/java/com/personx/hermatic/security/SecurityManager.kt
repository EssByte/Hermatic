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

    fun saveBaseUrl(url: String) {
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        sharedPreferences.edit().putString("base_url", formattedUrl).apply()
    }

    fun getBaseUrl(): String {
        return sharedPreferences.getString("base_url", "https://hermes-agent.nousresearch.com/") ?: "https://hermes-agent.nousresearch.com/"
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

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean("biometric_enabled", true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun getSystemPrompt(): String {
        return sharedPreferences.getString("system_prompt", "You are Hermes, a helpful AI assistant.") ?: "You are Hermes, a helpful AI assistant."
    }

    fun saveSystemPrompt(prompt: String) {
        sharedPreferences.edit().putString("system_prompt", prompt).apply()
    }

    fun getTemperature(): Float {
        return sharedPreferences.getFloat("temperature", 0.7f)
    }

    fun saveTemperature(temp: Float) {
        sharedPreferences.edit().putFloat("temperature", temp).apply()
    }

    fun getMaxTokens(): Int {
        return sharedPreferences.getInt("max_tokens", 2048)
    }

    fun saveMaxTokens(tokens: Int) {
        sharedPreferences.edit().putInt("max_tokens", tokens).apply()
    }

    fun getSelectedModel(): String {
        return sharedPreferences.getString("selected_model", "hermes-agent") ?: "hermes-agent"
    }

    fun saveSelectedModel(model: String) {
        sharedPreferences.edit().putString("selected_model", model).apply()
    }

    fun getPrimaryColor(): String {
        // Default to Monochrome (White/Greenish-White)
        return sharedPreferences.getString("theme_primary_color", "#FFFFFF") ?: "#FFFFFF"
    }

    fun savePrimaryColor(hex: String) {
        sharedPreferences.edit().putString("theme_primary_color", hex).apply()
    }

    fun getAccentColor(): String {
        return sharedPreferences.getString("theme_accent_color", "#00FF00") ?: "#00FF00"
    }

    fun saveAccentColor(hex: String) {
        sharedPreferences.edit().putString("theme_accent_color", hex).apply()
    }

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean("theme_dark_mode", true)
    }

    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("theme_dark_mode", enabled).apply()
    }
}
