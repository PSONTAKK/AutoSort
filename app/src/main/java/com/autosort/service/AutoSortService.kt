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
import com.autosort.data.auth.GoogleAuthManager
import com.autosort.data.config.AppConfig
import com.autosort.data.db.AppDatabase
import com.autosort.data.model.DestinationType
import com.autosort.data.repository.LogRepository
import com.autosort.data.repository.RuleRepository
import com.autosort.service.destination.FileDestination
import com.autosort.service.destination.GoogleDriveDestination
import com.autosort.service.destination.LocalFileDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class AutoSortService : Service() {

    companion object {
        private const val TAG              = "AutoSortService"
        const val CHANNEL_ID               = "autosort_service"
        private const val NOTIFICATION_ID  = 1001
    }

    private val serviceJob   = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var appConfig: AppConfig
    private lateinit var fileSortEngine: FileSortEngine
    private var fileObserver: FileObserver? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        appConfig = AppConfig(this)

        val db             = AppDatabase.getInstance(this)
        val ruleRepository = RuleRepository(db.ruleDao())
        val logRepository  = LogRepository(db.logDao())

        // ── Destination Registry ──────────────────────────────────────────
        // Add new FileDestination implementations here as you build them.
        val googleAuthManager = GoogleAuthManager.getInstance(this)
        val destinations: Map<DestinationType, FileDestination> = mapOf(
            DestinationType.LOCAL        to LocalFileDestination(),
            DestinationType.CLOUD_GDRIVE to GoogleDriveDestination(googleAuthManager)
            // DestinationType.CLOUD_DROPBOX to DropboxDestination(context),
            // DestinationType.CLOUD_S3      to S3Destination(context),
        )

        fileSortEngine = FileSortEngine(ruleRepository, logRepository, destinations)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Observe source folder — restarts FileObserver whenever it changes
        serviceScope.launch {
            appConfig.sourceFolderFlow().collectLatest { folder ->
                Log.i(TAG, "Source folder changed to: $folder")
                startObserver(folder)
            }
        }

        Log.i(TAG, "AutoSortService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fileObserver?.stopWatching()
        serviceJob.cancel()
        Log.i(TAG, "AutoSortService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── FileObserver ──────────────────────────────────────────────────────

    private fun startObserver(folderPath: String) {
        // Stop previous observer if running
        fileObserver?.stopWatching()

        val mask = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO

        fileObserver = object : FileObserver(File(folderPath), mask) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null) return

                val fullPath = "$folderPath/$path"
                Log.d(TAG, "FileObserver event=$event path=$fullPath")

                serviceScope.launch {
                    fileSortEngine.process(fullPath)
                }
            }
        }

        fileObserver?.startWatching()
        Log.i(TAG, "FileObserver watching: $folderPath  mask=$mask")

        // ── Initial scan: process files already in the folder ────────────
        serviceScope.launch {
            val folder = File(folderPath)
            if (folder.exists() && folder.isDirectory) {
                val existing = folder.listFiles()?.filter { it.isFile } ?: emptyList()
                Log.i(TAG, "Initial scan: found ${existing.size} existing files in $folderPath")
                for (file in existing) {
                    Log.d(TAG, "Initial scan processing: ${file.absolutePath}")
                    fileSortEngine.process(file.absolutePath)
                }
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
