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
    private var lastPushed = 0f
    private var hasPushed = false
    private var lastPushTime = 0L

    private lateinit var navigator: PinNavigator
    private var lastPinMarkers: List<PinMarker> = emptyList()
    private var lastPinPush = 0L

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        navigator = PinNavigator(this) { onPinsChanged() }
        sensor = HeadingSensor(this) { heading ->
            val now = SystemClock.elapsedRealtime()
            // Smallest angle between this reading and the last one we pushed (wraps at 360°).
            var diff = abs(heading - lastPushed) % 360f
            if (diff > 180f) diff = 360f - diff
            // Always push the first reading, then throttle: ≥1° change and ≤5 updates per second.
            if (!hasPushed || (diff >= 1f && now - lastPushTime >= 200)) {
                hasPushed = true
                lastPushed = heading
                lastPushTime = now
                CompassWidgetProvider.pushHeading(this, heading)
            }
        }
    }

    /** Pins need location while the app is in the background, which takes "Allow all the time". */
    private fun canLocate() = Permissions.hasFine(this) && Permissions.hasBackground(this)

    private fun onPinsChanged() {
        val markers = navigator.markers
        val shapeChanged = markers.map { it.copy(angle = 0f) } != lastPinMarkers.map { it.copy(angle = 0f) }
        val now = SystemClock.elapsedRealtime()
        if (!shapeChanged) {
            val moved = markers.indices.any { i ->
                var d = abs(markers[i].angle - lastPinMarkers[i].angle) % 360f
                if (d > 180f) d = 360f - d
                d >= 2f
            }
            if (!moved || now - lastPinPush < 1500) return
        }
        lastPinMarkers = markers
        lastPinPush = now
        CompassWidgetProvider.pushPins(this, markers)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground promptly after startForegroundService(), even if we stop right away.
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), Permissions.fgsType(canLocate()))
        } catch (e: RuntimeException) {
            // Location type refused (e.g. permission changed): carry on as a plain compass.
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), Permissions.fgsType(false))
        }

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
            if (canLocate()) navigator.start()
            registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
            handler.postDelayed({ stopSelf() }, TIMEOUT_MS)
            CompassWidgetProvider.update(this, live = true)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (hasPushed) CompassWidgetProvider.saveHeading(this, lastPushed)
        if (running) {
            running = false
            sensor.stop()
            navigator.stop()
            handler.removeCallbacksAndMessages(null)
            runCatching { unregisterReceiver(screenOff) }
        }
        CompassWidgetProvider.pinMarkers = emptyList()
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
