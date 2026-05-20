package com.autosort.service.ai

import com.autosort.data.config.ExternalConfig
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V3: Scans file names for sensitive keywords BEFORE sending to AI.
 * Runs entirely on-device — no network calls, no AI credits used.
 */
@Singleton
class PrivacyScanner @Inject constructor() {

    data class ScanResult(
        val safeFiles: List<String>,
        val sensitiveFiles: List<String>
    )

    /**
     * Scans a list of file names and separates them into safe vs sensitive.
     */
    fun scan(fileNames: List<String>): ScanResult {
        val sensitive = mutableListOf<String>()
        val safe = mutableListOf<String>()

        for (fileName in fileNames) {
            val lowerName = fileName.lowercase()
            if (ExternalConfig.SENSITIVE_KEYWORDS.any { lowerName.contains(it) }) {
                sensitive.add(fileName)
            } else {
                safe.add(fileName)
            }
        }

        return ScanResult(safeFiles = safe, sensitiveFiles = sensitive)
    }

    /**
     * Scans files in a directory and returns the scan result.
     */
    fun scanFolder(folderPath: String): ScanResult {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            return ScanResult(emptyList(), emptyList())
        }
        val fileNames = folder.listFiles()
            ?.filter { it.isFile }
            ?.map { it.name }
            ?: emptyList()

        return scan(fileNames)
    }
}
