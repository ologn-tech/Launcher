package com.osamaalek.kiosklauncher.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.model.AppInfo

class AppsAdapter(private val appsList: List<AppInfo>, private val context: Context) :
    RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.holder_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appsList[position]
        holder.appName.text = app.label
        holder.appIcon.setImageDrawable(app.icon)

        holder.itemView.setOnClickListener {
            val launchIntent: Intent? = context.packageManager.getLaunchIntentForPackage(app.packageName.toString())
            launchIntent?.let { context.startActivity(it) }
        }
    }

    override fun getItemCount(): Int = appsList.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        val appName: TextView = itemView.findViewById(R.id.app_name)
    }
}
