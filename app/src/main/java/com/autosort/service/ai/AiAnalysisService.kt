package com.autosort.service.ai

import android.util.Log
import com.autosort.data.config.ExternalConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V3: Communicates with Google Gemini API for folder analysis.
 * Uses raw HTTP — no additional SDK dependency needed.
 *
 * Two-tier analysis:
 *   1. [analyzeFolderByNames] — Lightweight folder summary using ONLY file names. Fast, cheap.
 *   2. [analyzeFileContents]  — Deep file content analysis. Heavier, requires user permission.
 *
 * All AI features are gated behind [ExternalConfig.isAiReady].
 */
@Singleton
class AiAnalysisService @Inject constructor() {

    companion object {
        private const val TAG = "AiAnalysisService"
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
    }

    // ── Tier 1: Folder Analysis (file names only) ──────────────────────────

    /**
     * Lightweight folder analysis using ONLY file names.
     * No file contents are read or sent. Fast and token-efficient.
     */
    suspend fun analyzeFolderByNames(
        folderName: String,
        fileNames: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!ExternalConfig.isAiReady) {
                throw IllegalStateException("AI is not configured. Set your API key in ExternalConfig.kt")
            }

            val prompt = buildFolderPrompt(folderName, fileNames)
            callGroqApi(prompt)
        }
    }

    private fun buildFolderPrompt(folderName: String, fileNames: List<String>): String {
        val fileListBlock = fileNames.take(100).joinToString("\n") { "- $it" }
        val extraNote = if (fileNames.size > 100) "\n... and ${fileNames.size - 100} more files" else ""

        return """
            You are AutoSort AI, a file organization assistant.
            Analyze this folder based ONLY on file names (no content available).

            FOLDER NAME: $folderName
            TOTAL FILES: ${fileNames.size}

            === FILE LIST ===
            $fileListBlock$extraNote

            Based on the file names, respond with:
            1. 📁 Folder Summary (2-3 lines — what type of folder is this, date range if dates appear in names)
            2. 📊 File categories with counts (e.g., PDFs: 12, Images: 5)
            3. 🔁 Possible duplicates (similar names)
            4. 🗑️ Junk/temp files that can be safely deleted
            5. 💡 Organization suggestions

            Keep it concise. Use emoji for clarity.
        """.trimIndent()
    }

    // ── Tier 2: Deep File Content Analysis ──────────────────────────────────

    /**
     * Deep analysis that reads and sends file contents to AI.
     * Requires user permission (privacy check must pass first).
     */
    suspend fun analyzeFileContents(
        folderName: String,
        fileNames: List<String>,
        fileContents: Map<String, String>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!ExternalConfig.isAiReady) {
                throw IllegalStateException("AI is not configured. Set your API key in ExternalConfig.kt")
            }

            val processedContents = mutableMapOf<String, String>()

            val binaryExtensions = setOf(
                ".mp4", ".mkv", ".avi", ".mov",
                ".zip", ".rar", ".7z", ".tar", ".gz",
                ".apk", ".exe", ".iso",
                ".jpg", ".jpeg", ".png", ".webp", ".gif",
                ".mp3", ".aac", ".wav", ".flac"
            )

            for ((fileName, content) in fileContents) {
                val lowerName = fileName.lowercase()
                if (binaryExtensions.any { lowerName.endsWith(it) }) continue

                val truncatedText = if (content.length > 1500) {
                    content.take(1500) + "\n... [TRUNCATED]"
                } else {
                    content
                }

                val redactedText = PiiRedactor.redact(truncatedText)
                processedContents[fileName] = redactedText
            }

            val prompt = buildDeepPrompt(folderName, fileNames, processedContents)
            callGroqApi(prompt)
        }
    }

    private fun buildDeepPrompt(
        folderName: String,
        fileNames: List<String>,
        fileContents: Map<String, String>
    ): String {
        val folderContext = when {
            folderName.contains("financ", ignoreCase = true) ||
            folderName.contains("bill", ignoreCase = true) ||
            folderName.contains("invoice", ignoreCase = true) ->
                "This is a FINANCE folder. Extract amounts, vendors, dates. Show a spending summary with vendor breakdown. Flag duplicates and spending anomalies."

            folderName.contains("work", ignoreCase = true) ||
            folderName.contains("project", ignoreCase = true) ->
                "This is a WORK folder. Identify document types (proposals, contracts, notes), project names, and whether files are drafts or finals."

            folderName.contains("medical", ignoreCase = true) ||
            folderName.contains("health", ignoreCase = true) ->
                "This is a MEDICAL folder. Summarize report types, dates, and doctor/hospital names. Do NOT quote medical diagnoses."

            folderName.contains("study", ignoreCase = true) ||
            folderName.contains("education", ignoreCase = true) ->
                "This is an EDUCATION folder. List subjects, assignment names, and identify incomplete work."

            else ->
                "Provide deep analysis of file contents: key topics, important data, and actionable insights."
        }

        val contentBlock = fileContents.entries.joinToString("\n---\n") { (name, content) ->
            "FILE: $name\n$content"
        }

        return """
            You are AutoSort AI, a file organization assistant.
            Perform DEEP analysis of the following file contents.

            FOLDER NAME: $folderName
            CONTEXT: $folderContext
            FILES ANALYZED: ${fileContents.size} of ${fileNames.size} total

            === FILE CONTENTS ===
            $contentBlock

            Respond with:
            1. 📄 Key findings from file contents
            2. 💰 Financial/data summaries (if applicable)
            3. ⚠️ Important items that need attention
            4. 💡 Actionable recommendations

            Keep it concise. Use emoji for clarity.
        """.trimIndent()
    }

    // ── Groq API Call ───────────────────────────────────────────────────────

    private fun callGroqApi(prompt: String, maxRetries: Int = 3): String {
        val model = ExternalConfig.AI_MODEL_NAME
        val apiKey = ExternalConfig.GROQ_API_KEY
        val url = URL(BASE_URL)

        val requestBody = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 60_000
            }

            try {
                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseBody)
                    val choices = json.getJSONArray("choices")
                    if (choices.length() == 0) {
                        throw RuntimeException("AI returned no results. The content may have been filtered by safety settings.")
                    }
                    val content = choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    Log.i(TAG, "Groq analysis complete (${content.length} chars)")
                    return content
                }

                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "Groq API error $responseCode (attempt $attempt/$maxRetries): $errorBody")

                if (responseCode == 429 && attempt < maxRetries) {
                    val delayMs = (attempt * 2000).toLong()
                    Log.i(TAG, "Rate limited (429). Retrying in ${delayMs}ms...")
                    Thread.sleep(delayMs)
                    lastException = RuntimeException("Rate limited by AI service")
                    continue
                }

                val userMessage = when (responseCode) {
                    429 -> "AI service is busy. Please wait a minute and try again."
                    400 -> "Invalid request to AI service. Please report this issue."
                    401, 403 -> "API key is invalid or expired. Please check your Groq API key."
                    500, 503 -> "AI service is temporarily down. Please try again later."
                    else -> "AI service returned error $responseCode"
                }
                throw RuntimeException(userMessage)

            } catch (e: RuntimeException) {
                lastException = e
                if (e.message?.contains("Rate limited") != true) throw e
            } finally {
                connection.disconnect()
            }
        }

        throw lastException ?: RuntimeException("AI analysis failed after $maxRetries attempts")
    }
}
