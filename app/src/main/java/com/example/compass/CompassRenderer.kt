package com.example.compass

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.roundToInt

/** Draws the compass face. Shared by the in-app view and the home screen widget bitmap. */
class CompassRenderer {

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#14171C") }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A414C"); style = Paint.Style.STROKE
    }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val pointer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF5A4D") }
    private val readout = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A93A0"); textAlign = Paint.Align.CENTER
    }
    private val path = Path()

    /**
     * @param heading degrees clockwise from north
     * @param hintText small caption under the readout (e.g. "Tap to start"), or null
     */
    fun draw(canvas: Canvas, width: Float, height: Float, heading: Float, hintText: String?) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f * 0.96f

        canvas.drawCircle(cx, cy, r, bg)
        ring.strokeWidth = r * 0.015f
        canvas.drawCircle(cx, cy, r, ring)

        // Rotating dial: north moves opposite to the heading.
        canvas.save()
        canvas.rotate(-heading, cx, cy)
        for (deg in 0 until 360 step 5) {
            val major = deg % 90 == 0
            val medium = deg % 30 == 0
            tick.color = if (deg == 0) Color.parseColor("#FF5A4D") else Color.WHITE
            tick.alpha = if (major || medium) 255 else 110
            tick.strokeWidth = r * if (major) 0.03f else if (medium) 0.02f else 0.01f
            val len = r * if (major) 0.12f else if (medium) 0.09f else 0.05f
            canvas.save()
            canvas.rotate(deg.toFloat(), cx, cy)
            canvas.drawLine(cx, cy - r * 0.97f, cx, cy - r * 0.97f + len, tick)
            canvas.restore()
        }
        for ((deg, text) in listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")) {
            label.color = if (deg == 0) Color.parseColor("#FF5A4D") else Color.WHITE
            label.textSize = r * 0.2f
            canvas.save()
            canvas.rotate(deg.toFloat(), cx, cy)
            canvas.drawText(text, cx, cy - r * 0.97f + r * 0.36f, label)
            canvas.restore()
        }
        label.color = Color.parseColor("#8A93A0")
        label.textSize = r * 0.075f
        for (deg in 30 until 360 step 30) {
            if (deg % 90 == 0) continue
            canvas.save()
            canvas.rotate(deg.toFloat(), cx, cy)
            canvas.drawText(deg.toString(), cx, cy - r * 0.97f + r * 0.27f, label)
            canvas.restore()
        }
        canvas.restore()

        // Fixed pointer at the top (the direction you're facing).
        path.reset()
        path.moveTo(cx, cy - r * 1.0f + r * 0.02f)
        path.lineTo(cx - r * 0.05f, cy - r * 0.84f)
        path.lineTo(cx + r * 0.05f, cy - r * 0.84f)
        path.close()
        canvas.drawPath(path, pointer)

        // Centre readout.
        val d = heading.roundToInt() % 360
        readout.textSize = r * 0.3f
        canvas.drawText("$d°", cx, cy + r * 0.06f, readout)
        readout.textSize = r * 0.15f
        canvas.drawText(cardinal(heading), cx, cy + r * 0.26f, readout)
        if (hintText != null) {
            hint.textSize = r * 0.08f
            canvas.drawText(hintText, cx, cy + r * 0.42f, hint)
        }
    }

    companion object {
        private val NAMES = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        fun cardinal(heading: Float): String = NAMES[((heading + 22.5f) / 45f).toInt() % 8]
    }
}
