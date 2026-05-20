package com.autosort

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.autosort.service.worker.MonthlyReportWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.google.android.gms.ads.MobileAds
import com.autosort.data.config.ExternalConfig

/**
 * Application entry point for Hilt dependency injection.
 * Also schedules periodic background workers.
 */
@HiltAndroidApp
class AutoSortApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (ExternalConfig.AdConfig.GLOBAL_ADS_ENABLED) {
            Thread { MobileAds.initialize(this) {} }.start()
        }
        scheduleMonthlyReport()
    }

    /**
     * Schedules the V3 monthly intelligence report.
     * Runs every 30 days. Uses KEEP policy to avoid rescheduling.
     */
    private fun scheduleMonthlyReport() {
        val monthlyWork = PeriodicWorkRequestBuilder<MonthlyReportWorker>(
            30, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "autosort_monthly_report",
            ExistingPeriodicWorkPolicy.KEEP,
            monthlyWork
        )
    }
}
