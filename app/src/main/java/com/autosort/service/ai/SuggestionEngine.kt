package com.autosort.service.ai

import com.autosort.data.model.RuleType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V3: On-device pattern matching engine for rule suggestions.
 * Does NOT use AI — runs entirely locally with zero cost.
 */
@Singleton
class SuggestionEngine @Inject constructor() {

    data class RuleSuggestion(
        val name: String,
        val matchType: RuleType,
        val matchValue: String,
        val suggestedFolder: String,
        val fileCount: Int,
        val emoji: String
    )

    /**
     * Scans a source folder and suggests rules based on file patterns.
     */
    fun generateSuggestions(sourceFolder: String): List<RuleSuggestion> {
        val folder = File(sourceFolder)
        if (!folder.exists() || !folder.isDirectory) return emptyList()

        val files = folder.listFiles()?.filter { it.isFile } ?: return emptyList()
        val fileNames = files.map { it.name.lowercase() }

        val suggestions = mutableListOf<RuleSuggestion>()

        // Pattern: PDF invoices/bills/receipts → Finance
        val pdfFinance = fileNames.count { name ->
            name.endsWith(".pdf") && (name.contains("invoice") || name.contains("bill") || name.contains("receipt"))
        }
        if (pdfFinance >= 2) {
            suggestions.add(
                RuleSuggestion(
                    name = "Finance PDFs",
                    matchType = RuleType.REGEX,
                    matchValue = ".*(?:invoice|bill|receipt).*\\.pdf$",
                    suggestedFolder = "Finance",
                    fileCount = pdfFinance,
                    emoji = "📄"
                )
            )
        }

        // Pattern: Images → Images folder
        val images = fileNames.count { name ->
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
        }
        if (images >= 3) {
            suggestions.add(
                RuleSuggestion(
                    name = "Image Files",
                    matchType = RuleType.REGEX,
                    matchValue = ".*\\.(jpg|jpeg|png|webp)$",
                    suggestedFolder = "Images",
                    fileCount = images,
                    emoji = "🖼️"
                )
            )
        }

        // Pattern: APK files → Apps folder
        val apks = fileNames.count { it.endsWith(".apk") }
        if (apks >= 2) {
            suggestions.add(
                RuleSuggestion(
                    name = "App Installers",
                    matchType = RuleType.ENDS,
                    matchValue = ".apk",
                    suggestedFolder = "Apps",
                    fileCount = apks,
                    emoji = "📱"
                )
            )
        }

        // Pattern: Archives → Archives folder
        val archives = fileNames.count { name ->
            name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") || name.endsWith(".tar.gz")
        }
        if (archives >= 2) {
            suggestions.add(
                RuleSuggestion(
                    name = "Archives",
                    matchType = RuleType.REGEX,
                    matchValue = ".*\\.(zip|rar|7z|tar\\.gz)$",
                    suggestedFolder = "Archives",
                    fileCount = archives,
                    emoji = "📦"
                )
            )
        }

        // Pattern: Documents → Documents folder
        val docs = fileNames.count { name ->
            name.endsWith(".docx") || name.endsWith(".doc") || name.endsWith(".txt") || name.endsWith(".xlsx") || name.endsWith(".csv")
        }
        if (docs >= 3) {
            suggestions.add(
                RuleSuggestion(
                    name = "Documents",
                    matchType = RuleType.REGEX,
                    matchValue = ".*\\.(docx|doc|txt|xlsx|csv)$",
                    suggestedFolder = "Documents",
                    fileCount = docs,
                    emoji = "📝"
                )
            )
        }

        // Pattern: Videos → Videos folder
        val videos = fileNames.count { name ->
            name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov")
        }
        if (videos >= 2) {
            suggestions.add(
                RuleSuggestion(
                    name = "Video Files",
                    matchType = RuleType.REGEX,
                    matchValue = ".*\\.(mp4|mkv|avi|mov)$",
                    suggestedFolder = "Videos",
                    fileCount = videos,
                    emoji = "🎬"
                )
            )
        }

        // Pattern: Study materials
        val study = fileNames.count { name ->
            name.contains("assignment") || name.contains("notes") || name.contains("chapter") || name.contains("lecture")
        }
        if (study >= 2) {
            suggestions.add(
                RuleSuggestion(
                    name = "Study Materials",
                    matchType = RuleType.REGEX,
                    matchValue = ".*(assignment|notes|chapter|lecture).*",
                    suggestedFolder = "Study",
                    fileCount = study,
                    emoji = "📚"
                )
            )
        }

        return suggestions
    }
}
