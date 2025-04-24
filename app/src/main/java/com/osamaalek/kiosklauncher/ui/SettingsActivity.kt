package com.osamaalek.kiosklauncher.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.AppsUtil
import com.osamaalek.kiosklauncher.adapter.SettingsAppsAdapter

class SettingsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var pinEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("kiosk_prefs", MODE_PRIVATE)

        setupViews()
        loadApps()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView_settings)
        pinEditText = findViewById(R.id.editText_pin)
        saveButton = findViewById(R.id.button_save)

        pinEditText.filters = arrayOf(InputFilter.LengthFilter(8))
        pinEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadApps() {
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        val apps = AppsUtil.getAllApps(this)

        val selectedApps = sharedPreferences.getStringSet("selected_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
        recyclerView.adapter = SettingsAppsAdapter(apps, this, selectedApps)

    }


    private fun saveSettings() {
        val pin = pinEditText.text.toString()
        if (pin.isNotEmpty()) {
            sharedPreferences.edit().putString("kiosk_pin", pin).apply()
        }

        val adapter = recyclerView.adapter as SettingsAppsAdapter
        adapter.saveSelectedApps()

        setResult(RESULT_OK)
        finish()
    }

    private fun getSelectedApps(): Set<String> {
        val adapter = recyclerView.adapter as SettingsAppsAdapter
        return adapter.getSelectedApps()
    }
}