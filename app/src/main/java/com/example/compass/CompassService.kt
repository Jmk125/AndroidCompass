package com.example.compass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlin.math.abs

/**
 * Keeps the compass sensor running while the home screen widget is "live".
 * Tap the widget to start; tap again, lock the screen, or wait for the timeout to stop.
 */
class CompassService : Service() {

    private lateinit var sensor: HeadingSensor
    private val handler = Handler(Looper.getMainLooper())
    private var lastPushed = -1000f
    private var lastPushTime = 0L

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        sensor = HeadingSensor(this) { heading ->
            val now = SystemClock.elapsedRealtime()
            var diff = abs(heading - lastPushed)
            if (diff > 180f) diff = 360f - diff
            // Throttle widget redraws: ≥1° change and ≤5 updates per second.
            if (diff >= 1f && now - lastPushTime >= 200) {
                lastPushed = heading
                lastPushTime = now
                CompassWidgetProvider.saveHeading(this, heading)
                CompassWidgetProvider.update(this, heading = heading, live = true)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground promptly after startForegroundService(), even if we stop right away.
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )

        if (intent?.action == ACTION_STOP || (intent?.action == ACTION_TOGGLE && running)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!sensor.isAvailable) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running) {
            running = true
            sensor.start()
            registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
            handler.postDelayed({ stopSelf() }, TIMEOUT_MS)
            CompassWidgetProvider.update(this, live = true)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (running) {
            running = false
            sensor.stop()
            handler.removeCallbacksAndMessages(null)
            runCatching { unregisterReceiver(screenOff) }
        }
        CompassWidgetProvider.update(this, live = false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, CompassService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .addAction(0, getString(R.string.stop), stop)
            .build()
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.compass.TOGGLE"
        const val ACTION_STOP = "com.example.compass.STOP"
        private const val CHANNEL_ID = "compass_live"
        private const val NOTIFICATION_ID = 1
        private const val TIMEOUT_MS = 5 * 60 * 1000L

        @Volatile var running = false
            private set
    }
}
