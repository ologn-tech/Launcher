package com.osamaalek.kiosklauncher.util

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.osamaalek.kiosklauncher.ui.MainActivity
import android.os.Looper
import com.osamaalek.kiosklauncher.FloatingButtonService

class BlockAppsService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sharedPreferences: SharedPreferences
    private val checkInterval: Long = 1000
    private val kioskPackageName = "com.osamaalek.kiosklauncher"

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
        handler.post(checkForegroundApp)
    }

    private val checkForegroundApp = object : Runnable {
        override fun run() {
            val allowedApps = sharedPreferences.getStringSet("selected_apps", emptySet())?.toMutableSet() ?: mutableSetOf()

            allowedApps.add(kioskPackageName)

            val foregroundApp = getForegroundApp()

            if (foregroundApp != null && !allowedApps.contains(foregroundApp)) {
                Log.d("BlockAppsService", "Unauthorized app detected: $foregroundApp")

                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            } else {
                Log.d("BlockAppsService", "Allowed app: $foregroundApp")
            }
            Log.d("FloatingButtonService", FloatingButtonService.isFloatingViewVisible.toString())

            handler.postDelayed(this, checkInterval)
        }
    }
    private fun startFloatingButtonService() {
        val serviceIntent = Intent(this, FloatingButtonService::class.java)
        startService(serviceIntent)

    }
    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - 10000,
            currentTime
        )

        if (!stats.isNullOrEmpty()) {
            val recentApp = stats.maxByOrNull { it.lastTimeUsed }
            return recentApp?.packageName
        }
        return null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkForegroundApp)
    }
}