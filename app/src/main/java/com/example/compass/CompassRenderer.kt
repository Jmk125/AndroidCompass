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
    private val path = Path()

    /** Background, ring, ticks and labels, with north at the top rotated by [rotation] degrees. */
    fun drawDial(canvas: Canvas, width: Float, height: Float, rotation: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f * 0.96f

        canvas.drawCircle(cx, cy, r, bg)
        ring.strokeWidth = r * 0.015f
        canvas.drawCircle(cx, cy, r, ring)

        canvas.save()
        canvas.rotate(rotation, cx, cy)
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
    }

    /** The fixed red pointer at the top (the direction you're facing). */
    fun drawPointer(canvas: Canvas, width: Float, height: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f * 0.96f
        path.reset()
        path.moveTo(cx, cy - r * 1.0f + r * 0.02f)
        path.lineTo(cx - r * 0.05f, cy - r * 0.84f)
        path.lineTo(cx + r * 0.05f, cy - r * 0.84f)
        path.close()
        canvas.drawPath(path, pointer)
    }

    /** Full compass for the in-app view. */
    fun draw(canvas: Canvas, width: Float, height: Float, heading: Float) {
        drawDial(canvas, width, height, -heading)
        drawPointer(canvas, width, height)

        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f * 0.96f
        readout.textSize = r * 0.3f
        canvas.drawText("${heading.roundToInt() % 360}°", cx, cy + r * 0.06f, readout)
        readout.textSize = r * 0.15f
        canvas.drawText(cardinal(heading), cx, cy + r * 0.26f, readout)
    }

    companion object {
        private val NAMES = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        fun cardinal(heading: Float): String = NAMES[((heading + 22.5f) / 45f).toInt() % 8]
    }
}
