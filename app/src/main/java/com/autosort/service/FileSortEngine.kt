package com.autosort.service

import android.util.Log
import com.autosort.data.model.DestinationType
import com.autosort.data.model.LogStatus
import com.autosort.data.model.RuleType
import com.autosort.data.model.SortLog
import com.autosort.data.repository.LogRepository
import com.autosort.data.repository.RuleRepository
import com.autosort.service.destination.FileDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "FileSortEngine"

// Filter 1: incomplete download extensions
private val INCOMPLETE_SUFFIXES = listOf(
    ".crdownload", ".download", ".tmp", ".pending"
)

// Filter 2: system-protected path segments
private val SYSTEM_PATH_SEGMENTS = listOf(
    "com.android.vending",
    "/Android/data/",
    "/Android/obb/",
    "/Android/media/"
)

/**
 * Core sorting pipeline. Loosely coupled — receives a registry of
 * [FileDestination] implementations keyed by [DestinationType].
 * Adding a new destination type requires zero changes here.
 */
class FileSortEngine(
    private val ruleRepository: RuleRepository,
    private val logRepository: LogRepository,
    private val destinations: Map<DestinationType, FileDestination>
) {

    suspend fun process(filePath: String) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val fileName = file.name

        // ── Filter 1: Incomplete download ──────────────────────────────────
        if (INCOMPLETE_SUFFIXES.any { fileName.endsWith(it, ignoreCase = true) }) {
            Log.d(TAG, "Skipped (incomplete): $fileName")
            return@withContext
        }

        // ── Filter 2: System-protected path ───────────────────────────────
        if (SYSTEM_PATH_SEGMENTS.any { filePath.contains(it) }) {
            Log.d(TAG, "Skipped (system protected): $filePath")
            return@withContext
        }

        // ── Filter 3: Rule match ───────────────────────────────────────────
        val activeRules = ruleRepository.getActiveRules()
        val matchedRule = activeRules.firstOrNull { rule ->
            try {
                val pattern = when (rule.type) {
                    RuleType.CONTAINS -> ".*${Regex.escape(rule.value)}.*"
                    RuleType.ENDS     -> ".*${Regex.escape(rule.value)}$"
                    RuleType.STARTS   -> "^${Regex.escape(rule.value)}.*"
                    RuleType.REGEX    -> rule.value  // Fix 4: wrapped in try-catch below
                }
                // Fix 4: Timeout guard — withTimeout cancels coroutine if regex hangs
                kotlinx.coroutines.withTimeout(2000L) {
                    fileName.matches(Regex(pattern, RegexOption.IGNORE_CASE))
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "Regex timed out for rule '${rule.name}': ${rule.value}")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Invalid regex in rule '${rule.name}': ${e.message}")
                false
            }
        }

        if (matchedRule == null) {
            Log.d(TAG, "No rule matched: $fileName")
            logRepository.insert(
                SortLog(
                    name   = fileName,
                    target = "",
                    status = LogStatus.SKIPPED
                )
            )
            return@withContext
        }

        // ── Route to the correct FileDestination ──────────────────────────
        val destination = destinations[matchedRule.destinationType]
        if (destination == null) {
            Log.e(TAG, "No destination registered for type: ${matchedRule.destinationType}")
            logRepository.insert(
                SortLog(
                    name   = fileName,
                    target = matchedRule.target,
                    status = LogStatus.FAIL_IO
                )
            )
            return@withContext
        }

        val result = destination.send(file, matchedRule.target, fileName)

        result.fold(
            onSuccess = { finalPath ->
                Log.i(TAG, "Sorted '$fileName' -> '$finalPath' via ${matchedRule.destinationType}")
                logRepository.insert(
                    SortLog(
                        name   = fileName,
                        target = finalPath,
                        status = LogStatus.SUCCESS
                    )
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to sort '$fileName': ${error.message}", error)
                logRepository.insert(
                    SortLog(
                        name   = fileName,
                        target = matchedRule.target,
                        status = LogStatus.FAIL_IO
                    )
                )
            }
        )
    }
}
