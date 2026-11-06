package com.example.compass

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.RemoteViews
import kotlin.math.roundToInt

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

        private const val PREFS = "compass"
        private const val KEY_HEADING = "heading"

        fun saveHeading(context: Context, heading: Float) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY_HEADING, heading).apply()

        private fun lastHeading(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_HEADING, 0f)

        private fun widgetIds(context: Context, manager: AppWidgetManager) =
            manager.getAppWidgetIds(ComponentName(context, CompassWidgetProvider::class.java))

        private fun setHeading(views: RemoteViews, heading: Float) {
            views.setFloat(R.id.dial, "setRotation", -heading)
            views.setTextViewText(
                R.id.readout, "${heading.roundToInt() % 360}°\n${CompassRenderer.cardinal(heading)}"
            )
        }

        /** Full redraw (sends the dial images). Used when the widget is placed or switched on/off. */
        fun update(
            context: Context,
            manager: AppWidgetManager = AppWidgetManager.getInstance(context),
            ids: IntArray = widgetIds(context, manager),
            live: Boolean = CompassService.running,
        ) {
            if (ids.isEmpty()) return
            val size = BITMAP_SIZE.toFloat()
            val dial = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
            renderer.drawDial(Canvas(dial), size, size, 0f)
            val pointer = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
            renderer.drawPointer(Canvas(pointer), size, size)

            val tap = PendingIntent.getForegroundService(
                context, 0,
                Intent(context, CompassService::class.java).setAction(CompassService.ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.widget_compass).apply {
                setImageViewBitmap(R.id.dial, dial)
                setImageViewBitmap(R.id.pointer, pointer)
                setViewVisibility(R.id.hint, if (live) View.GONE else View.VISIBLE)
                setHeading(this, lastHeading(context))
                setOnClickPendingIntent(R.id.root, tap)
            }
            manager.updateAppWidget(ids, views)
        }

        /** Cheap live update: only a rotation angle and a short text string are sent. */
        fun pushHeading(context: Context, heading: Float) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = widgetIds(context, manager)
            if (ids.isEmpty()) return
            val views = RemoteViews(context.packageName, R.layout.widget_compass)
            setHeading(views, heading)
            manager.partiallyUpdateAppWidget(ids, views)
        }
    }
}
