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
        private const val BITMAP_SIZE = 384 // a multiple of 64 so the pixel style scales cleanly

        /** Latest pin arrows, set by the live service; empty when the widget is idle. */
        @Volatile var pinMarkers: List<PinMarker> = emptyList()

        private const val PREFS = "compass"
        private const val KEY_HEADING = "heading"

        private var cachedStyle: CompassStyle? = null
        private var cachedRenderer: StyleRenderer? = null

        private fun rendererFor(context: Context, style: CompassStyle): StyleRenderer {
            val existing = cachedRenderer
            if (existing != null && cachedStyle == style) return existing
            val fresh = style.createRenderer(context.applicationContext)
            cachedStyle = style
            cachedRenderer = fresh
            return fresh
        }

        fun saveHeading(context: Context, heading: Float) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY_HEADING, heading).apply()

        private fun lastHeading(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_HEADING, 0f)

        private fun widgetIds(context: Context, manager: AppWidgetManager) =
            manager.getAppWidgetIds(ComponentName(context, CompassWidgetProvider::class.java))

        private fun setHeading(views: RemoteViews, heading: Float) {
            views.setFloat(R.id.dial, "setRotation", -heading)
            views.setFloat(R.id.pins, "setRotation", -heading)
            views.setTextViewText(R.id.readout, "${heading.roundToInt() % 360}°\n${cardinalOf(heading)}")
        }

        private fun layer(draw: (Canvas, Float) -> Unit): Bitmap {
            val bmp = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
            draw(Canvas(bmp), BITMAP_SIZE.toFloat())
            return bmp
        }

        /** Full redraw (sends the dial images). Used when the widget is placed, switched on/off or restyled. */
        fun update(
            context: Context,
            manager: AppWidgetManager = AppWidgetManager.getInstance(context),
            ids: IntArray = widgetIds(context, manager),
            live: Boolean = CompassService.running,
        ) {
            if (ids.isEmpty()) return
            val style = StylePrefs.get(context)
            val renderer = rendererFor(context, style)

            val dial = layer { cv, s -> renderer.drawFace(cv, s); renderer.drawContent(cv, s, 0f) }
            val pins = layer { cv, s -> renderer.drawPins(cv, s, pinMarkers, 0f) }
            val top = layer { cv, s -> renderer.drawTop(cv, s, true) }

            val tap = PendingIntent.getForegroundService(
                context, 0,
                Intent(context, CompassService::class.java).setAction(CompassService.ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, style.widgetLayout()).apply {
                setImageViewBitmap(R.id.dial, dial)
                setImageViewBitmap(R.id.pins, pins)
                setImageViewBitmap(R.id.pointer, top)
                setViewVisibility(R.id.hint, if (live) View.GONE else View.VISIBLE)
                setHeading(this, lastHeading(context))
                setOnClickPendingIntent(R.id.root, tap)
            }
            manager.updateAppWidget(ids, views)
        }

        /** Redraws only the pin arrows layer (called when bearings change noticeably). */
        fun pushPins(context: Context, markers: List<PinMarker>) {
            pinMarkers = markers
            val manager = AppWidgetManager.getInstance(context)
            val ids = widgetIds(context, manager)
            if (ids.isEmpty()) return
            val style = StylePrefs.get(context)
            val renderer = rendererFor(context, style)
            val bmp = layer { cv, s -> renderer.drawPins(cv, s, markers, 0f) }
            val views = RemoteViews(context.packageName, style.widgetLayout())
            views.setImageViewBitmap(R.id.pins, bmp)
            manager.partiallyUpdateAppWidget(ids, views)
        }

        /** Cheap live update: only a rotation angle and a short text string are sent. */
        fun pushHeading(context: Context, heading: Float) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = widgetIds(context, manager)
            if (ids.isEmpty()) return
            val views = RemoteViews(context.packageName, StylePrefs.get(context).widgetLayout())
            setHeading(views, heading)
            manager.partiallyUpdateAppWidget(ids, views)
        }
    }
}
