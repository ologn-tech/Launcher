package com.osamaalek.kiosklauncher.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.model.AppInfo

class SettingsAppsAdapter(
    private val appsList: List<AppInfo>,
    private val context: Context,
    private val selectedApps: MutableSet<String>
) : RecyclerView.Adapter<SettingsAppsAdapter.AppViewHolder>() {

    init {
        val sharedPreferences = context.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
        val selectedAppsSet = sharedPreferences.getStringSet("selected_apps", emptySet<String>()) ?: emptySet<String>()
        selectedApps.addAll(selectedAppsSet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.holder_app_settings, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appsList[position]
        holder.appName.text = app.label
        holder.appIcon.setImageDrawable(app.icon)

        holder.checkBox.isChecked = selectedApps.contains(app.packageName)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedApps.add(app.packageName.toString())
            } else {
                selectedApps.remove(app.packageName)
            }
        }
    }

    override fun getItemCount(): Int = appsList.size

    fun getSelectedApps(): Set<String> {
        return selectedApps
    }

    fun saveSelectedApps() {
        val sharedPreferences = context.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("selected_apps", selectedApps).apply()
    }

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        val appName: TextView = itemView.findViewById(R.id.app_name)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_app)
    }
}
