package com.autosort.data.config

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * SharedPreferences-backed app configuration.
 * Loosely coupled — any component can read/observe settings
 * without knowing where they're stored.
 */
class AppConfig(context: Context) {

    companion object {
        private const val PREFS_NAME          = "autosort_config"
        private const val KEY_SOURCE_FOLDER   = "source_folder"

        private val DEFAULT_SOURCE_FOLDER: String =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ).absolutePath
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
