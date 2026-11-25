package org.balise.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WidgetSaveService : Service(), LocationListener {

    companion object {
        const val CHANNEL_ID = "balise_widget_channel"
        const val NOTIFICATION_ID = 2001
        const val LOCATION_TIMEOUT_MS = 15000L
    }

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var locationManager: LocationManager? = null
    private var hasReceivedLocation = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        widgetId = intent?.getIntExtra(
            BaliseWidget.EXTRA_WIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        showNotification("📍 Enregistrement du lieu…", ongoing = true)

        if (!hasLocationPermission(this)) {
            showNotification("❌ Permission localisation requise. Ouvrez l'application Balise.", ongoing = false, openApp = true)
            stopSelf()
            return START_NOT_STICKY
        }

        val lastKnown = getLastKnownLocation()
        if (lastKnown != null && isRecent(lastKnown)) {
            savePlace(lastKnown)
            return START_NOT_STICKY
        }

        requestFreshLocation()
        return START_STICKY
    }

    
    }

    private fun getLastKnownLocation(): Location? {
        val lm = locationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var best: Location? = null
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null && (best == null || loc.accuracy < best.accuracy)) {
                    best = loc
                }
            } catch (_: SecurityException) {}
        }
        return best
    }

    private fun isRecent(location: Location): Boolean {
        val age = System.currentTimeMillis() - location.time
        return age < 60_000L // Moins d'une minute
    }

    private fun requestFreshLocation() {
        val lm = locationManager ?: return
        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                if (lm.isProviderEnabled(provider)) {
                    lm.requestLocationUpdates(provider, 0L, 0f, this, Looper.getMainLooper())
                }
            }
        } catch (_: SecurityException) {
            showNotification("❌ Erreur de localisation", ongoing = false)
            stopSelf()
            return
        }

        android.os.Handler(Looper.getMainLooper()).postDelayed({
            if (!hasReceivedLocation) {
                val fallback = getLastKnownLocation()
                if (fallback != null) {
                    savePlace(fallback)
                } else {
                    showNotification("❌ Position introuvable. Sortez à l'air libre.", ongoing = false)
                }
                cleanupAndStop()
            }
        }, LOCATION_TIMEOUT_MS)
    }

    override fun onLocationChanged(location: Location) {
        if (hasReceivedLocation) return
        hasReceivedLocation = true
        savePlace(location)
        cleanupAndStop()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun savePlace(location: Location) {
        val now = System.currentTimeMillis()
        val defaultName = "Lieu du " + SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(now))

        // Création de l'objet Place
        val place = Place(
            id = now,
            name = defaultName,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            savedAt = now
        )

        // Sauvegarde via PlaceStore
        val store = PlaceStore(this)
        val current = store.load().toMutableList()
        current.add(0, place)
        store.save(current)

        // Mise à jour du widget
        val views = RemoteViews(packageName, R.layout.widget_balise)
        views.setTextViewText(R.id.widget_title, "✓ Enregistré")
        views.setTextViewText(R.id.widget_status, defaultName)
        views.setViewVisibility(R.id.widget_status, android.view.View.VISIBLE)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        val coords = "${location.latitude.toString().take(8)}, ${location.longitude.toString().take(8)}"
        val text = "📍 $defaultName\n$coords (±${location.accuracy.toInt()}m)"
        showNotification(text, ongoing = false)
    }

    private fun cleanupAndStop() {
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun showNotification(text: String, ongoing: Boolean, openApp: Boolean = false) {
        val contentIntent = if (openApp) {
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                PendingIntent.getActivity(
                    this, 0, it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        } else null

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Balise")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply { contentIntent?.let { setContentIntent(it) } }
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ongoing) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else if (ongoing) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Widget Balise",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications du widget d'enregistrement rapide"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
