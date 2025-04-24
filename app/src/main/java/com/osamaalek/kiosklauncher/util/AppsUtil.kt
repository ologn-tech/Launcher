package com.osamaalek.kiosklauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.osamaalek.kiosklauncher.model.AppInfo

// AppsUtil.kt
class AppsUtil {
    companion object {
        fun getAllApps(context: Context, onlySelected: Boolean = false): List<AppInfo> {
            val packageManager: PackageManager = context.packageManager
            val appsList = ArrayList<AppInfo>()
            val i = Intent(Intent.ACTION_MAIN, null)
            i.addCategory(Intent.CATEGORY_LAUNCHER)

            val prefs = context.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
            val selectedApps = prefs.getStringSet("selected_apps", null)

            val allApps = packageManager.queryIntentActivities(i, 0)
            for (ri in allApps) {
                val packageName = ri.activityInfo.packageName
//                Log.d("AppUtil", "asfsdfsd "+ packageName );
                if (!onlySelected || selectedApps == null || selectedApps.contains(packageName)) {
                    val app = AppInfo(
                        ri.loadLabel(packageManager),
                        packageName,
                        ri.activityInfo.loadIcon(packageManager)
                    )
                    appsList.add(app)
                }
            }
            return appsList
        }
    }
}