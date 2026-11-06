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
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlin.math.abs

/**
 * A draggable compass bubble drawn over other apps.
 * Drag to move, tap to cycle size, long-press (or the notification's Stop button) to close.
 */
class FloatingCompassService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var compassView: CompassView
    private lateinit var sensor: HeadingSensor
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var added = false
    private var sizeIndex = 1

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Don't burn battery on the sensor while the screen is off.
            if (intent.action == Intent.ACTION_SCREEN_OFF) sensor.stop() else sensor.start()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        compassView = CompassView(this)
        sensor = HeadingSensor(this) { compassView.heading = it }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        if (intent?.action == ACTION_STOP || !android.provider.Settings.canDrawOverlays(this) || !sensor.isAvailable) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!added) {
            addBubble()
            sensor.start()
            registerReceiver(screenReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            })
            added = true
            running = true
        }
        return START_NOT_STICKY
    }

    private fun addBubble() {
        val dp = resources.displayMetrics.density
        val size = (SIZES_DP[sizeIndex] * dp).toInt()
        params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * dp).toInt()
            y = (120 * dp).toInt()
        }

        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val longPress = Runnable { stopSelf() }
        var downRawX = 0f; var downRawY = 0f
        var startX = 0; var startY = 0
        var moved = false

        compassView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = e.rawX; downRawY = e.rawY
                    startX = params.x; startY = params.y
                    moved = false
                    handler.postDelayed(longPress, 800)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downRawX
                    val dy = e.rawY - downRawY
                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) {
                        moved = true
                        handler.removeCallbacks(longPress)
                    }
                    if (moved) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        windowManager.updateViewLayout(compassView, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPress)
                    if (!moved) cycleSize()
                }
                MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(longPress)
            }
            true
        }
        windowManager.addView(compassView, params)
    }

    private fun cycleSize() {
        sizeIndex = (sizeIndex + 1) % SIZES_DP.size
        val px = (SIZES_DP[sizeIndex] * resources.displayMetrics.density).toInt()
        params.width = px
        params.height = px
        windowManager.updateViewLayout(compassView, params)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        sensor.stop()
        if (added) {
            runCatching { windowManager.removeView(compassView) }
            runCatching { unregisterReceiver(screenReceiver) }
            added = false
        }
        running = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.floating_channel), NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this, 2, Intent(this, FloatingCompassService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val open = PendingIntent.getActivity(
            this, 3, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.floating_notification_title))
            .setContentText(getString(R.string.floating_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, getString(R.string.stop), stop)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.example.compass.FLOATING_STOP"
        private const val CHANNEL_ID = "compass_floating"
        private const val NOTIFICATION_ID = 2
        private val SIZES_DP = intArrayOf(110, 160, 230)

        @Volatile var running = false
            private set
    }
}
