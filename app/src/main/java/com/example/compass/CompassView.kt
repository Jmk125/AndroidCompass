package com.example.compass

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.view.View

/** The full compass in the chosen style. Used by the app and the floating bubble. */
class CompassView(context: Context, private var fixedStyle: CompassStyle? = null) : View(context) {

    private var renderer: StyleRenderer = (fixedStyle ?: StylePrefs.get(context)).createRenderer(context)

    private val prefs = context.getSharedPreferences(StylePrefs.PREFS, Context.MODE_PRIVATE)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == StylePrefs.KEY && fixedStyle == null) {
            renderer = StylePrefs.get(context).createRenderer(context)
            invalidate()
        }
    }

    var heading: Float = 0f
        set(value) { field = value; invalidate() }

    var markers: List<PinMarker> = emptyList()
        set(value) { field = value; invalidate() }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (fixedStyle == null) {
            renderer = StylePrefs.get(context).createRenderer(context)
            prefs.registerOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun onDetachedFromWindow() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        renderer.drawFull(canvas, width.toFloat(), height.toFloat(), heading, markers)
    }
}
