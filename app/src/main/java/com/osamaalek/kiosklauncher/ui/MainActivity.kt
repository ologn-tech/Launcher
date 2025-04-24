package com.osamaalek.kiosklauncher.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.osamaalek.kiosklauncher.FloatingButtonService
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.AppsUtil
import com.osamaalek.kiosklauncher.adapter.AppsAdapter

class MainActivity : AppCompatActivity() {
    final val TAG = "MainActivity";
    private var recyclerView: RecyclerView? = null
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    private var clickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Kiểm tra quyền usage stats
        if (!hasUsageStatsPermission(this)) {
            showUsagePermissionDialog()
        }
        recyclerView = findViewById(R.id.appListRecyclerView)

        setupOverlayPermissionLauncher()

        // Check và yêu cầu quyền overlay nếu cần
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
        } else {
            startFloatingButtonService()
        }

        setupRecyclerView()

    }

    private fun setupOverlayPermissionLauncher() {
        overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingButtonService()
            } else {
                Toast.makeText(this, "Permission required to display overlay", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUsagePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("To function properly, the app needs access to usage data.")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
            .setCancelable(false)
            .show()
    }

    private fun startFloatingButtonService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onResume() {
        super.onResume()
        if (!FloatingButtonService.isFloatingViewVisible) {
            startFloatingButtonService()
        }
    }

    private fun setupRecyclerView() {
        recyclerView!!.layoutManager = GridLayoutManager(this, 4)
        val selectedApps:MutableSet<String> = mutableSetOf("com.android.settings", "com.android.chrome", "com.android.camera")
        val apps = AppsUtil.getAllApps(this).filter { selectedApps.contains(it.packageName) }
        recyclerView!!.adapter = AppsAdapter(apps, this)
    }


    override fun onBackPressed() {}
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(AppOpsManager::class.java)
        val uid = context.applicationInfo.uid
        val mode = appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
