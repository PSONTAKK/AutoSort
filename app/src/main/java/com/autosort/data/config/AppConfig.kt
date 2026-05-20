package com.autosort.data.config

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * SharedPreferences-backed app configuration.
 * Fix 6: Uses EncryptedSharedPreferences so config data is encrypted at rest.
 * Loosely coupled — any component can read/observe settings
 * without knowing where they're stored.
 */
class AppConfig(context: Context) {

    companion object {
        private const val TAG                 = "AppConfig"
        private const val PREFS_NAME          = "autosort_config_encrypted"
        private const val LEGACY_PREFS_NAME   = "autosort_config"
        private const val KEY_SOURCE_FOLDER   = "source_folder"
        private const val KEY_PAUSE_UNTIL     = "pause_until"

        // V3 Keys
        private const val KEY_SUB_TIER = "sub_tier"
        private const val KEY_AI_CREDITS = "ai_credits"
        private const val KEY_AI_RESET_DATE = "ai_reset_date"
        
        // V2 Keys
        private const val KEY_GOOGLE_ACCOUNTS = "google_accounts"

        private val DEFAULT_SOURCE_FOLDER: String =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ).absolutePath
    }

    private val prefs: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also { encrypted ->
            // Migrate from legacy unencrypted prefs if they exist
            migrateLegacyPrefs(context, encrypted)
        }
    } catch (e: Exception) {
        // Fallback to regular prefs if encrypted fails (should not happen on API 30+)
        Log.e(TAG, "EncryptedSharedPreferences failed, using fallback: ${e.message}")
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * One-time migration: copies values from the old unencrypted prefs
     * to the new encrypted store, then clears the old file.
     */
    private fun migrateLegacyPrefs(context: Context, encrypted: SharedPreferences) {
        val legacy = context.applicationContext
            .getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) return  // nothing to migrate

        val editor = encrypted.edit()
        legacy.getString(KEY_SOURCE_FOLDER, null)?.let {
            editor.putString(KEY_SOURCE_FOLDER, it)
        }
        editor.apply()
        legacy.edit().clear().apply()
        Log.i(TAG, "Migrated legacy prefs to encrypted store")
    }

    // ── Pause state ───────────────────────────────────────────────────────

    var pauseUntil: Long
        get() = prefs.getLong(KEY_PAUSE_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_PAUSE_UNTIL, value).apply()

    // ── V2: Multi-Account Drive ───────────────────────────────────────

    var connectedGoogleAccounts: Set<String>
        get() = prefs.getStringSet(KEY_GOOGLE_ACCOUNTS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_GOOGLE_ACCOUNTS, value).apply()

    fun addGoogleAccount(email: String) {
        val current = connectedGoogleAccounts.toMutableSet()
        current.add(email)
        connectedGoogleAccounts = current
    }

    fun removeGoogleAccount(email: String) {
        val current = connectedGoogleAccounts.toMutableSet()
        current.remove(email)
        connectedGoogleAccounts = current
    }

    // ── V3: Monetization & Credits ────────────────────────────────────

    var subscriptionTier: String
        get() = prefs.getString(KEY_SUB_TIER, "FREE") ?: "FREE"
        set(value) = prefs.edit().putString(KEY_SUB_TIER, value).apply()

    var aiCreditsUsed: Int
        get() = prefs.getInt(KEY_AI_CREDITS, 0)
        set(value) = prefs.edit().putInt(KEY_AI_CREDITS, value).apply()

    var aiCreditsResetDate: Long
        get() = prefs.getLong(KEY_AI_RESET_DATE, 0L)
        set(value) = prefs.edit().putLong(KEY_AI_RESET_DATE, value).apply()

    /**
     * Checks if user has remaining AI credits (resets monthly).
     */
    fun hasAiCredits(): Boolean {
        if (subscriptionTier == "INTELLIGENCE") return true // Unlimited or higher limit?
        
        val now = System.currentTimeMillis()
        if (now > aiCreditsResetDate) {
            // Reset for the new month
            aiCreditsUsed = 0
            // Set next reset to 30 days from now
            aiCreditsResetDate = now + (30L * 24 * 60 * 60 * 1000)
        }
        
        return aiCreditsUsed < 10 // 10 free scans per month
    }

    /**
     * Consumes one AI credit.
     */
    fun consumeAiCredit() {
        if (subscriptionTier != "INTELLIGENCE") {
            aiCreditsUsed += 1
        }
    }

    fun isPaused(): Boolean {
        return System.currentTimeMillis() < pauseUntil
    }

    // ── Source folder ──────────────────────────────────────────────────────

    var sourceFolder: String
        get()  = prefs.getString(KEY_SOURCE_FOLDER, DEFAULT_SOURCE_FOLDER) ?: DEFAULT_SOURCE_FOLDER
        set(value) = prefs.edit().putString(KEY_SOURCE_FOLDER, value).apply()

    /**
     * Emits the current source folder and then every subsequent change.
     * Consumers (e.g. AutoSortService) can collect this to restart FileObserver.
     */
    fun sourceFolderFlow(): Flow<String> = callbackFlow {
        // Emit current value immediately
        trySend(sourceFolder)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SOURCE_FOLDER) {
                trySend(sourceFolder)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
