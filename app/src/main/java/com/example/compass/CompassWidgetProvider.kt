package com.example.compass

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews

class CompassWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, manager, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        // Last widget removed: make sure the sensor service isn't left running.
        context.stopService(Intent(context, CompassService::class.java))
    }

    companion object {
        private const val BITMAP_SIZE = 360
        private val renderer = CompassRenderer()
        private var bitmap: Bitmap? = null

        private const val PREFS = "compass"
        private const val KEY_HEADING = "heading"

        fun saveHeading(context: Context, heading: Float) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY_HEADING, heading).apply()

        private fun lastHeading(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_HEADING, 0f)

        /** Redraws every placed widget. [heading] null => last known heading, shown as idle. */
        fun update(
            context: Context,
            manager: AppWidgetManager = AppWidgetManager.getInstance(context),
            ids: IntArray = manager.getAppWidgetIds(ComponentName(context, CompassWidgetProvider::class.java)),
            heading: Float? = null,
            live: Boolean = CompassService.running,
        ) {
            if (ids.isEmpty()) return
            val h = heading ?: lastHeading(context)

            // A fresh bitmap each time: RemoteViews won't redraw if handed the same object.
            val bmp = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
            renderer.draw(
                Canvas(bmp), BITMAP_SIZE.toFloat(), BITMAP_SIZE.toFloat(), h,
                if (live) null else context.getString(R.string.tap_to_start)
            )

            val tap = PendingIntent.getForegroundService(
                context, 0,
                Intent(context, CompassService::class.java).setAction(CompassService.ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.widget_compass).apply {
                setImageViewBitmap(R.id.compass_image, bmp)
                setOnClickPendingIntent(R.id.compass_image, tap)
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
