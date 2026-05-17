package com.autosort.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.autosort.service.AutoSortService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val appConfig = com.autosort.data.config.AppConfig(context)
        
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED" &&
            action != "com.autosort.ACTION_RESUME_SERVICE"
        ) return

        if (appConfig.isPaused()) {
            Log.i(TAG, "Service is still paused until ${appConfig.pauseUntil}. Skipping start.")
            return
        }

        Log.i(TAG, "Starting AutoSortService (Action: $action)")

        val serviceIntent = Intent(context, AutoSortService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
