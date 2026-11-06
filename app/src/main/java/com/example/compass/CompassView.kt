package com.example.compass

import android.content.Context
import android.graphics.Canvas
import android.view.View

class CompassView(context: Context) : View(context) {
    private val renderer = CompassRenderer()

    var heading: Float = 0f
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        renderer.draw(canvas, width.toFloat(), height.toFloat(), heading)
    }
}
