package com.osamaalek.kiosklauncher.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
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
import com.osamaalek.kiosklauncher.adapter.AppsAdapter
import com.osamaalek.kiosklauncher.util.AppsUtil
import com.osamaalek.kiosklauncher.util.BlockAppsService

class MainActivity : AppCompatActivity() {
    private var recyclerView: RecyclerView? = null
    private var sharedPreferences: SharedPreferences? = null
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    private var clickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Kiểm tra quyền usage stats
        if (!hasUsageStatsPermission(this)) {
            showUsagePermissionDialog()
        }

        sharedPreferences = getSharedPreferences("kiosk_prefs", MODE_PRIVATE)
        recyclerView = findViewById(R.id.appListRecyclerView)

        setupOverlayPermissionLauncher()
        setupSettingsLauncher()

        // Check và yêu cầu quyền overlay nếu cần
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
        } else {
            startFloatingButtonService()
        }

        setupRecyclerView()
        setupClickListener()

        // Khởi động dịch vụ chặn app
        startService(Intent(this, BlockAppsService::class.java))
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

    private fun setupSettingsLauncher() {
        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                setupRecyclerView()
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
        startService(Intent(this, BlockAppsService::class.java))
        if (!FloatingButtonService.isFloatingViewVisible) {
            startFloatingButtonService()
        }
    }

    private fun setupRecyclerView() {
        recyclerView!!.layoutManager = GridLayoutManager(this, 4)
        val selectedApps = sharedPreferences?.getStringSet("selected_apps", emptySet()) ?: emptySet()
        val apps = AppsUtil.getAllApps(this).filter { selectedApps.contains(it.packageName) }
        recyclerView!!.adapter = AppsAdapter(apps, this)
    }

    private fun setupClickListener() {
        findViewById<View>(R.id.mainLayout).setOnClickListener {
            clickCount++
            if (clickCount >= 5) {
                showPinDialog()
                clickCount = 0
            }
        }
    }

    private fun showPinDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf<InputFilter>(LengthFilter(8))
        }

        AlertDialog.Builder(this)
            .setTitle("Enter PIN")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                val savedPin = sharedPreferences!!.getString("kiosk_pin", "")
                if (input.text.toString() == savedPin) {
                    settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
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
