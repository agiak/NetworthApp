package com.agcoding.networkapp.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NetWorthDetailWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NetWorthDetailWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                try {
                    GlanceAppWidgetManager(context)
                        .getGlanceIds(NetWorthDetailWidget::class.java)
                        .forEach { id -> glanceAppWidget.update(context, id) }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
