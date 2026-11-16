package com.senin.paketin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.ColorUtils

class BatteryService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 60000L // 60 saniye

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateBatteryStatus()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "BATTERY_WIDGET_CHANNEL")
            .setContentTitle("Battery Widget Aktif")
            .setContentText("Pil seviyesi takip ediliyor.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        
        // Android 14+ (API 34) için zorunlu Servis Tipi beyanı
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY 
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateBatteryStatus() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            this.registerReceiver(null, ifilter)
        }

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        
        val percentage = if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }

        if (percentage != -1) {
            updateWidgetUI(this, percentage, isCharging)
        }
    }

    private fun updateWidgetUI(context: Context, percentage: Int, isCharging: Boolean) {
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
        if (isCharging) return Color.parseColor("#2196F3")

        return if (percentage >= 50) {
            val fraction = (percentage - 50) / 50.0f
            ColorUtils.blendARGB(Color.parseColor("#FFC107"), Color.parseColor("#4CAF50"), fraction)
        } else {
            val fraction = percentage / 50.0f
            ColorUtils.blendARGB(Color.parseColor("#F44336"), Color.parseColor("#FFC107"), fraction)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("BATTERY_WIDGET_CHANNEL", "Pil Widget Servisi", NotificationManager.IMPORTANCE_MIN)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
