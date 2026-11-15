package com.senin.paketin // Kendi paket adınla değiştir

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.ColorUtils

class BatteryService : Service() {

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val percentage = (level * 100 / scale.toFloat()).toInt()

            updateWidget(context, percentage, isCharging)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "BATTERY_WIDGET_CHANNEL")
            .setContentTitle("Pil Göstergesi Aktif")
            .setContentText("Widget pil durumunu dinliyor.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging) // Sistemin varsayılan ikonu
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(1, notification)

        // Pil değişimlerini dinlemeye başla
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateWidget(context: Context, percentage: Int, isCharging: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, BatteryWidgetProvider::class.java)
        val views = RemoteViews(context.packageName, R.layout.widget_battery)

        val color = getBatteryColor(percentage, isCharging)

        views.setTextViewText(R.id.text_percentage, "%$percentage")
        views.setInt(R.id.bg_image, "setColorFilter", color)
        views.setViewVisibility(R.id.icon_charging, if (isCharging) View.VISIBLE else View.GONE)

        appWidgetManager.updateAppWidget(componentName, views)
    }

    private fun getBatteryColor(percentage: Int, isCharging: Boolean): Int {
        if (isCharging) return Color.parseColor("#2196F3") // Şarjda mavi

        return if (percentage >= 50) {
            val fraction = (percentage - 50) / 50.0f
            ColorUtils.blendARGB(Color.parseColor("#FFC107"), Color.parseColor("#4CAF50"), fraction) // Sarı -> Yeşil
        } else {
            val fraction = percentage / 50.0f
            ColorUtils.blendARGB(Color.parseColor("#F44336"), Color.parseColor("#FFC107"), fraction) // Kırmızı -> Sarı
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("BATTERY_WIDGET_CHANNEL", "Pil Widget Servisi", NotificationManager.IMPORTANCE_MIN)
            channel.description = "Widget için arka plan pil takibi"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
