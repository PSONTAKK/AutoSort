package com.autosort.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import com.autosort.MainActivity
import com.autosort.data.config.AppConfig
import com.autosort.data.repository.LogRepository
import com.autosort.data.repository.RuleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class AutoSortService : Service() {

    companion object {
        private const val TAG              = "AutoSortService"
        const val CHANNEL_ID               = "autosort_service"
        private const val NOTIFICATION_ID  = 1001
    }

    private val serviceJob   = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    @Inject lateinit var appConfig: AppConfig
    @Inject lateinit var fileSortEngine: FileSortEngine
    @Inject lateinit var logRepository: LogRepository
    @Inject lateinit var ruleRepository: RuleRepository

    private val observers = mutableListOf<FileObserver>()

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Observe source folder changes
        serviceScope.launch {
            appConfig.sourceFolderFlow().collectLatest {
                Log.i(TAG, "Global source folder changed or rules updated")
                startObservers()
            }
        }
        
        // Also observe rule changes so we watch new source folders automatically
        serviceScope.launch {
            ruleRepository.allRules.collectLatest {
                startObservers()
            }
        }

        // V-04: Prune old logs every time the service starts
        serviceScope.launch {
            try {
                logRepository.pruneOldLogs()
            } catch (_: Exception) { /* non-critical */ }
        }

        Log.i(TAG, "AutoSortService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_FORCE_SCAN") {
            Log.i(TAG, "Manual scan triggered by user via ACTION_FORCE_SCAN")
            serviceScope.launch {
                startObservers() // Restarts observers and triggers initial scan
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        observers.forEach { it.stopWatching() }
        observers.clear()
        serviceJob.cancel()
        Log.i(TAG, "AutoSortService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── FileObserver ──────────────────────────────────────────────────────

    private suspend fun startObservers() {
        // Stop previous observers
        observers.forEach { it.stopWatching() }
        observers.clear()

        val rules = ruleRepository.getActiveRules()
        val globalFolder = appConfig.sourceFolder
        val foldersToWatch = mutableSetOf(globalFolder)
        
        // Add all valid custom source folders from active rules
        rules.forEach { rule ->
            rule.sourceFolder?.let { customFolder ->
                if (customFolder.isNotBlank() && File(customFolder).exists()) {
                    foldersToWatch.add(customFolder)
                }
            }
        }

        val mask = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO

        for (folderPath in foldersToWatch) {
            val folderFile = File(folderPath)
            if (!folderFile.exists()) continue

            val observer = object : FileObserver(folderFile, mask) {
                override fun onEvent(event: Int, path: String?) {
                    if (path == null) return
                    val fullPath = "$folderPath/$path"
                    Log.d(TAG, "FileObserver event=$event path=$fullPath")
                    serviceScope.launch { fileSortEngine.process(fullPath) }
                }
            }
            observer.startWatching()
            observers.add(observer)
            Log.i(TAG, "FileObserver watching: $folderPath  mask=$mask")

            // ── Initial scan: process files already in the folder ────────────
            val existing = folderFile.listFiles()?.filter { it.isFile } ?: emptyList()
            Log.i(TAG, "Initial scan: found ${existing.size} existing files in $folderPath")
            for (file in existing) {
                fileSortEngine.process(file.absolutePath)
            }
        }
    }

    // ── Notification ──────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AutoSort Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors source folder and sorts files automatically"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoSort Active")
            .setContentText("Monitoring ${appConfig.sourceFolder}")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
