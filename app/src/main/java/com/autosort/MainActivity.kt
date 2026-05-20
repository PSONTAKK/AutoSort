package com.autosort

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.autosort.data.config.AppConfig
import com.autosort.service.AutoSortService
import com.autosort.ui.navigation.NavGraph
import com.autosort.ui.navigation.Routes
import com.autosort.ui.screens.PermissionRationaleScreen
import com.autosort.ui.theme.AutoSortTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appConfig: AppConfig

    private var hasStoragePermission by mutableStateOf(false)

    // Re-check permission when returning from Settings
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasStoragePermission = Environment.isExternalStorageManager()
        if (hasStoragePermission) startSortingService()
    }

    // POST_NOTIFICATIONS (API 33+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Permission result handled silently; service still runs without it on API < 33
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasStoragePermission = Environment.isExternalStorageManager()
        if (hasStoragePermission) {
            startSortingService()
        }
        requestNotificationPermissionIfNeeded()

        setContent {
            AutoSortTheme {
                if (hasStoragePermission) {
                    val navController = rememberNavController()
                    val startDest = if (intent.getBooleanExtra("open_report", false)) Routes.MONTHLY_REPORT else Routes.DASHBOARD
                    NavGraph(navController = navController, startDestination = startDest)
                } else {
                    PermissionRationaleScreen(
                        onGrantClick = { openManageStorageSettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluate on every resume in case user granted from Settings manually
        val granted = Environment.isExternalStorageManager()
        if (granted && !hasStoragePermission) {
            hasStoragePermission = true
            startSortingService()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun openManageStorageSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        manageStorageLauncher.launch(intent)
    }

    private fun startSortingService() {
        if (appConfig.isPaused()) {
            return
        }

        val intent = Intent(this, AutoSortService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }
}
