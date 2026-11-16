package com.example.compass

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.content.Context

/** The original look: dark dial, red north, thin ticks. */
class ModernRenderer(context: Context) : StyleRenderer(context) {

    private val red = hexc("#FF5A4D")
    private val gray = hexc("#8A93A0")
    private val bold = Typeface.DEFAULT_BOLD

    override fun drawFace(c: Canvas, s: Float) {
        val m = s / 2f
        val r = m * 0.96f
        fillP(hexc("#14171C")); c.drawCircle(m, m, r, p)
        strokeP(hexc("#3A414C"), r * 0.015f); c.drawCircle(m, m, r, p)
    }

    override fun drawContent(c: Canvas, s: Float, rotation: Float) {
        val m = s / 2f
        val r = m * 0.96f
        c.save()
        c.rotate(rotation, m, m)
        for (d in 0 until 360 step 5) {
            val major = d % 90 == 0
            val medium = d % 30 == 0
            strokeP(if (d == 0) red else Color.WHITE, r * if (major) 0.03f else if (medium) 0.02f else 0.01f)
            p.alpha = if (major || medium) 255 else 110
            val len = r * if (major) 0.12f else if (medium) 0.09f else 0.05f
            c.save()
            c.rotate(d.toFloat(), m, m)
            c.drawLine(m, m - r * 0.97f, m, m - r * 0.97f + len, p)
            c.restore()
        }
        for ((d, t) in listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")) {
            c.save()
            c.rotate(d.toFloat(), m, m)
            txt(c, t, m, m - r * 0.97f + r * 0.36f, r * 0.2f, if (d == 0) red else Color.WHITE, bold)
            c.restore()
        }
        for (d in 30 until 360 step 30) {
            if (d % 90 == 0) continue
            c.save()
            c.rotate(d.toFloat(), m, m)
            txt(c, d.toString(), m, m - r * 0.97f + r * 0.27f, r * 0.075f, gray, bold)
            c.restore()
        }
        c.restore()
    }

    override fun drawPins(c: Canvas, s: Float, markers: List<PinMarker>, rotation: Float) =
        pinLayer(c, s, markers, rotation)

    override fun drawTop(c: Canvas, s: Float, compact: Boolean) {
        val m = s / 2f
        val r = m * 0.96f
        path.reset()
        path.moveTo(m, m - r * 0.98f)
        path.lineTo(m - r * 0.05f, m - r * 0.84f)
        path.lineTo(m + r * 0.05f, m - r * 0.84f)
        path.close()
        fillP(red); c.drawPath(path, p)
    }

    override fun drawReadout(c: Canvas, s: Float, heading: Float) {
        val m = s / 2f
        val r = m * 0.96f
        txt(c, "${Math.round(heading) % 360}°", m, m + r * 0.06f, r * 0.3f, Color.WHITE, bold)
        txt(c, cardinalOf(heading), m, m + r * 0.26f, r * 0.15f, Color.WHITE, bold)
    }
}
