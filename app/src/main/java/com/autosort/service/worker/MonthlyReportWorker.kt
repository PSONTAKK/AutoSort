package com.autosort.service.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autosort.data.config.ExternalConfig
import com.autosort.data.db.LogDao
import com.autosort.service.ai.AiAnalysisService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * V3: Monthly intelligence report — mostly on-device DB queries.
 * AI is used only for smart suggestions (1 credit max).
 */
@HiltWorker
class MonthlyReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val logDao: LogDao,
    private val aiService: AiAnalysisService
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "MonthlyReportWorker"
        const val CHANNEL_ID = "autosort_reports"
        private const val NOTIFICATION_ID = 2001
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting monthly intelligence report")

        try {
            // ── On-device stats (free — no AI) ────────────────────────────
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val recentLogs = logDao.getLogsSince(thirtyDaysAgo)

            val totalMoved = recentLogs.count { it.status.name == "SUCCESS" }
            val totalFailed = recentLogs.count { it.status.name == "FAIL_IO" }
            val totalSkipped = recentLogs.count { it.status.name == "SKIPPED" }
            val timeSavedMinutes = totalMoved * 2  // Rough estimate: 2 min per file

            // Find most active rule
            val mostActiveRule = recentLogs
                .filter { it.ruleName.isNotBlank() }
                .groupBy { it.ruleName }
                .maxByOrNull { it.value.size }

            // Calculate missing stats
            val totalProcessed = recentLogs.size
            val successRate = if (totalProcessed > 0) (totalMoved.toFloat() / totalProcessed * 100).toInt() else 0
            
            // Files per destination
            val destStats = recentLogs
                .filter { it.status.name == "SUCCESS" && it.target.isNotBlank() }
                .groupBy { 
                    // Extract just the folder name from the path for cleaner display
                    it.target.substringAfterLast("/") 
                }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
                .take(3)

            // Build the report
            val report = buildString {
                appendLine("📊 AutoSort Monthly Report")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLine("✅ Files sorted: $totalMoved (Success Rate: $successRate%)")
                appendLine("❌ Failed: $totalFailed")
                appendLine("⏭ Skipped: $totalSkipped")
                appendLine("⏱ Time saved: ~${timeSavedMinutes}min")
                
                if (mostActiveRule != null) {
                    appendLine("\n🏆 Top Rule: \"${mostActiveRule.key}\" (${mostActiveRule.value.size} files)")
                }
                
                if (destStats.isNotEmpty()) {
                    appendLine("\n📁 Top Destinations:")
                    destStats.forEach { (dest, count) ->
                        appendLine("  • $dest: $count files")
                    }
                }
            }

            // ── Optional AI suggestion (uses 1 credit if enabled) ─────────
            var aiSuggestion = ""
            if (ExternalConfig.isAiReady && totalSkipped > 3) {
                val skippedNames = recentLogs
                    .filter { it.status.name == "SKIPPED" }
                    .map { it.name }
                    .distinct()
                    .take(20)

                val result = aiService.analyzeFolderByNames(
                    folderName = "Unmatched Files",
                    fileNames = skippedNames
                )
                result.onSuccess { aiSuggestion = "\n💡 AI Suggestion:\n$it" }
            }

            // Send notification
            sendReportNotification(report + aiSuggestion)
            Log.i(TAG, "Monthly report complete: $totalMoved files sorted")

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Monthly report failed: ${e.message}", e)
            return Result.retry()
        }
    }

    private fun sendReportNotification(report: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "AutoSort Reports",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Monthly intelligence reports"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = android.content.Intent(applicationContext, com.autosort.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            // We can pass an extra to tell MainActivity to navigate to a specific screen later
            putExtra("open_report", true) 
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("📊 AutoSort Monthly Report")
            .setContentText("You sorted ${report.lines().find { it.startsWith("✅") }?.substringAfter(": ")?.substringBefore(" (") ?: "files"} this month!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(report))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
