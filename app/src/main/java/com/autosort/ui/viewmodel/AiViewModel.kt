package com.autosort.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autosort.data.config.AppConfig
import com.autosort.data.db.AiInsightDao
import com.autosort.data.model.AiInsight
import com.autosort.service.ai.AiAnalysisService
import com.autosort.service.ai.PrivacyScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File

enum class AiStep {
    IDLE,
    SCANNING,           // Scanning file names locally
    FOLDER_ANALYZING,   // Tier 1: Sending file names to AI
    FOLDER_RESULTS,     // Tier 1: Showing folder summary (with "Deep Analyze" button)
    PRIVACY_PROMPT,     // Tier 2: User reviews sensitive files
    DEEP_ANALYZING,     // Tier 2: Sending file contents to AI
    RESULTS,            // Tier 2: Showing deep file analysis
    ERROR
}

@HiltViewModel
class AiViewModel @Inject constructor(
    private val privacyScanner: PrivacyScanner,
    private val aiService: AiAnalysisService,
    private val insightDao: AiInsightDao,
    private val appConfig: AppConfig
) : ViewModel() {

    private val _currentStep = MutableStateFlow(AiStep.IDLE)
    val currentStep: StateFlow<AiStep> = _currentStep.asStateFlow()

    private val _sensitiveFiles = MutableStateFlow<List<String>>(emptyList())
    val sensitiveFiles: StateFlow<List<String>> = _sensitiveFiles.asStateFlow()

    private val _safeFiles = MutableStateFlow<List<String>>(emptyList())

    private val _excludedFiles = MutableStateFlow<Set<String>>(emptySet())
    val excludedFiles: StateFlow<Set<String>> = _excludedFiles.asStateFlow()

    // Tier 1: Folder summary (file names only)
    private val _folderInsights = MutableStateFlow<String>("")
    val folderInsights: StateFlow<String> = _folderInsights.asStateFlow()

    // Tier 2: Deep file analysis
    private val _deepInsights = MutableStateFlow<String>("")
    val deepInsights: StateFlow<String> = _deepInsights.asStateFlow()

    private val _errorMessage = MutableStateFlow<String>("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private var currentFolderPath: String = ""
    private var currentFolderName: String = ""

    // ── Tier 1: Folder Analysis (file names only, fast) ─────────────────

    fun startAnalysis(folderPath: String, folderName: String) {
        currentFolderPath = folderPath
        currentFolderName = folderName
        _errorMessage.value = ""

        if (!appConfig.hasAiCredits()) {
            _errorMessage.value = "You have used all 10 free AI scans for this month. Upgrade to Intelligence tier for unlimited scans."
            _currentStep.value = AiStep.ERROR
            return
        }

        _currentStep.value = AiStep.SCANNING

        viewModelScope.launch {
            // Check cache first
            val cached = insightDao.getInsight(folderPath)
            if (cached != null) {
                _folderInsights.value = cached.summary
                _currentStep.value = AiStep.FOLDER_RESULTS
                return@launch
            }

            // Scan folder for file names (local, instant)
            val folder = File(folderPath)
            val allFileNames = if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.filter { it.isFile }?.map { it.name } ?: emptyList()
            } else {
                emptyList()
            }

            if (allFileNames.isEmpty()) {
                _errorMessage.value = "No files found in folder."
                _currentStep.value = AiStep.ERROR
                return@launch
            }

            // Also run privacy scan for later use (Tier 2)
            val privacyResult = privacyScanner.scan(allFileNames)
            _safeFiles.value = privacyResult.safeFiles
            _sensitiveFiles.value = privacyResult.sensitiveFiles
            _excludedFiles.value = emptySet()

            // Tier 1: Send ONLY file names to AI (fast, lightweight)
            _currentStep.value = AiStep.FOLDER_ANALYZING
            val result = aiService.analyzeFolderByNames(folderName, allFileNames)
            result.fold(
                onSuccess = { summary ->
                    _folderInsights.value = summary
                    _currentStep.value = AiStep.FOLDER_RESULTS
                    // Cache the folder summary
                    insightDao.insert(
                        AiInsight(
                            folderPath = currentFolderPath,
                            timestamp = System.currentTimeMillis(),
                            summary = summary,
                            fileCountAtScan = allFileNames.size
                        )
                    )
                    appConfig.consumeAiCredit()
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Folder analysis failed."
                    _currentStep.value = AiStep.ERROR
                }
            )
        }
    }

    // ── Tier 2: Deep File Analysis (reads content, needs permission) ────

    fun startDeepAnalysis() {
        if (_sensitiveFiles.value.isNotEmpty()) {
            _currentStep.value = AiStep.PRIVACY_PROMPT
        } else {
            continueDeepAnalysis()
        }
    }

    fun toggleExclusion(fileName: String) {
        val current = _excludedFiles.value
        _excludedFiles.value = if (current.contains(fileName)) {
            current - fileName
        } else {
            current + fileName
        }
    }

    fun continueDeepAnalysis() {
        if (!appConfig.hasAiCredits()) {
            _errorMessage.value = "No AI credits remaining for deep analysis."
            _currentStep.value = AiStep.ERROR
            return
        }

        _currentStep.value = AiStep.DEEP_ANALYZING
        viewModelScope.launch {
            val allowedSensitive = _sensitiveFiles.value.filter { it !in _excludedFiles.value }
            val filesToRead = _safeFiles.value + allowedSensitive
            val allFileNames = _safeFiles.value + _sensitiveFiles.value

            // Read contents of text files only (max 5 files, 1500 chars each)
            val textExtensions = setOf(".txt", ".csv", ".json", ".xml", ".log", ".md", ".html", ".htm")
            val fileContents = mutableMapOf<String, String>()
            val textFilesToRead = filesToRead
                .filter { name -> textExtensions.any { name.lowercase().endsWith(it) } }
                .take(5)
            for (fileName in textFilesToRead) {
                val file = File(currentFolderPath, fileName)
                if (file.exists() && file.isFile && file.length() < 500_000) {
                    try {
                        fileContents[fileName] = file.readText().take(1500)
                    } catch (e: Exception) {
                        // Ignore read errors
                    }
                }
            }

            if (fileContents.isEmpty()) {
                _errorMessage.value = "No readable text files found for deep analysis."
                _currentStep.value = AiStep.ERROR
                return@launch
            }

            val result = aiService.analyzeFileContents(currentFolderName, allFileNames, fileContents)
            result.fold(
                onSuccess = { summary ->
                    _deepInsights.value = summary
                    _currentStep.value = AiStep.RESULTS
                    appConfig.consumeAiCredit()
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Deep analysis failed."
                    _currentStep.value = AiStep.ERROR
                }
            )
        }
    }

    fun reset() {
        _currentStep.value = AiStep.IDLE
        _sensitiveFiles.value = emptyList()
        _safeFiles.value = emptyList()
        _excludedFiles.value = emptySet()
        _folderInsights.value = ""
        _deepInsights.value = ""
        _errorMessage.value = ""
        currentFolderPath = ""
        currentFolderName = ""
    }
}
