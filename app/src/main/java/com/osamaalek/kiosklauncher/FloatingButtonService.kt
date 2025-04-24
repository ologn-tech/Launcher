package com.osamaalek.kiosklauncher

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.osamaalek.kiosklauncher.ui.MainActivity
import java.io.BufferedReader
import java.io.InputStreamReader


class FloatingButtonService : Service() {

    private var pinDialogView: View? = null
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: Button
    private var clickCount = 0
    private val resetClickHandler = Handler()
    private val resetClickRunnable = Runnable { clickCount = 0 }
    private var currentApp =""

    companion object {
        var isFloatingViewVisible = false
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "floating_service_channel",
                "Floating Button Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "floating_service_channel")
            .setContentTitle("Floating Button Running")
            .setContentText("Touch the button 5 times to unlock.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            stopSelf()
//            Log.d("FloatingButtonService", "stop self")
            isFloatingViewVisible = false
            return
        }


        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val buttonWidth = 200
        val buttonHeight = 150

        val params = WindowManager.LayoutParams(
            buttonWidth,
            buttonHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.LEFT
        params.x = 0
        params.y = 0

        floatingButton = Button(this).apply {
            text = ""
            setBackgroundColor(Color.TRANSPARENT)
            alpha = 1f
            isClickable = true
        }

        windowManager.addView(floatingButton, params)

        floatingButton.setOnClickListener {
            clickCount++
            resetClickHandler.removeCallbacks(resetClickRunnable)
            resetClickHandler.postDelayed(resetClickRunnable, 2000)

            if (clickCount >= 5) {
                currentApp = getCurrentApp().toString();
                if (!currentApp.contains("kiosklauncher"))
                        showPinOverlayDialog()
            }
        }

        isFloatingViewVisible = true
    }

    private fun showPinOverlayDialog() {
        if (pinDialogView != null) return // Prevent duplicates

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_pin, null)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.CENTER

        val pinDisplay = view.findViewById<TextView>(R.id.pinDisplay)
        val button0 = view.findViewById<Button>(R.id.button0)
        val button1 = view.findViewById<Button>(R.id.button1)
        val button2 = view.findViewById<Button>(R.id.button2)
        val button3 = view.findViewById<Button>(R.id.button3)
        val button4 = view.findViewById<Button>(R.id.button4)
        val button5 = view.findViewById<Button>(R.id.button5)
        val button6 = view.findViewById<Button>(R.id.button6)
        val button7 = view.findViewById<Button>(R.id.button7)
        val button8 = view.findViewById<Button>(R.id.button8)
        val button9 = view.findViewById<Button>(R.id.button9)

        // Variable to store entered PIN
        var enteredPin = ""

        // Set click listeners for each number button
        val buttons = listOf(button0,button1, button2, button3, button4, button5, button6, button7, button8, button9)
        buttons.forEach { button ->
            button.setOnClickListener {
                enteredPin += button.text.toString()  // Append clicked number
                pinDisplay.text = enteredPin  // Update displayed PIN
            }
        }

        // Set up cancel and OK buttons
        val btnOk = view.findViewById<Button>(R.id.okButton)
        val btnCancel = view.findViewById<Button>(R.id.cancelButton)

        btnOk.setOnClickListener {
            if (enteredPin == getKioskPassword()) {
                killApp(currentApp)
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                try {
                    pinDialogView?.let {
                        windowManager.removeView(it)
                        pinDialogView = null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            try {
                pinDialogView?.let {
                    windowManager.removeView(it)
                    pinDialogView = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Add the view and show the PIN dialog
        try {
            windowManager.addView(view, layoutParams)
            pinDialogView = view
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCurrentApp(): String? {
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

    fun getKioskPassword(): String? {
        try {
            val process = Runtime.getRuntime()
                .exec(arrayOf("su", "-c", "getprop persist.vendor.mikiosk.password"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val password = reader.readLine()
            reader.close()
            return password?.trim { it <= ' ' }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun killApp(packageName: String) {
//        Log.d("gfgdgdfg","gdfgdfg "+currentApp);
        try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
//            Toast.makeText(this, "Force stopped: $packageName", Toast.LENGTH_SHORT).show()
        } catch (e: java.lang.Exception) {
//            Toast.makeText(this, "Failed to force stop: $packageName", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::windowManager.isInitialized && ::floatingButton.isInitialized) {
                windowManager.removeView(floatingButton)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isFloatingViewVisible = false
    }
}
