package com.example.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.sqrt

/** Tactical pale-green display on dark olive-black: thin ticks, a faint radar grid, a heads-up readout. */
class OperationRenderer(context: Context) : StyleRenderer(context) {

    private val mono = font(R.font.share_tech_mono, Typeface.MONOSPACE)
    private val green = hexc("#A8E6B0")
    private val dim = hexc("#5E8F69")
    private val faint = hexc("#2E4A38")
    private val amber = hexc("#F2B84B")
    private val plate = RectF()

    override val pinOutline: Int = hexc("#06100A")

    override fun drawFace(c: Canvas, s: Float) {
        val m = s / 2f
        val r = m * 0.96f
        fillP(hexc("#1B2A22")); c.drawCircle(m, m, r, p)
        strokeP(hexc("#3C5446"), r * 0.012f); c.drawCircle(m, m, r, p)

        p.reset(); p.isAntiAlias = true
        p.shader = RadialGradient(m, m, r * 0.92f, intArrayOf(hexc("#10201A"), hexc("#070D0A")), null, Shader.TileMode.CLAMP)
        c.drawCircle(m, m, r * 0.92f, p)
        strokeP(dim, r * 0.008f); c.drawCircle(m, m, r * 0.92f, p)

        // Faint radar grid.
        strokeP(faint, r * 0.006f)
        c.drawCircle(m, m, r * 0.62f, p)
        c.drawCircle(m, m, r * 0.32f, p)
        c.drawLine(m - r * 0.92f, m, m + r * 0.92f, m, p)
        c.drawLine(m, m - r * 0.92f, m, m + r * 0.92f, p)

        // Very light scanlines.
        val edge = r * 0.92f
        val step = s / 90f
        var y = m - edge + step
        strokeP(Color.argb(14, 168, 230, 176), 1f)
        while (y < m + edge) {
            val dx = sqrt(edge * edge - (y - m) * (y - m))
            c.drawLine(m - dx, y, m + dx, y, p)
            y += step
        }
    }

    override fun drawContent(c: Canvas, s: Float, rotation: Float) {
        val m = s / 2f
        val r = m * 0.96f
        c.save()
        c.rotate(rotation, m, m)
        for (d in 0 until 360 step 5) {
            if (d % 90 == 0) continue
            val d30 = d % 30 == 0
            val d10 = d % 10 == 0
            strokeP(green, r * if (d30) 0.012f else 0.008f)
            p.alpha = if (d30) 255 else if (d10) 170 else 100
            val len = r * if (d30) 0.09f else if (d10) 0.06f else 0.035f
            c.save()
            c.rotate(d.toFloat(), m, m)
            c.drawLine(m, m - r * 0.9f, m, m - r * 0.9f + len, p)
            c.restore()
        }
        for ((d, t) in listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")) {
            val color = if (d == 0) amber else green
            c.save()
            c.rotate(d.toFloat(), m, m)
            path.reset()
            path.moveTo(m, m - r * 0.8f)
            path.lineTo(m - r * 0.04f, m - r * 0.9f)
            path.lineTo(m + r * 0.04f, m - r * 0.9f)
            path.close()
            fillP(color); c.drawPath(path, p)
            txt(c, t, m, m - r * 0.66f, r * 0.15f, color, mono)
            c.restore()
        }
        for (d in 30 until 360 step 30) {
            if (d % 90 == 0) continue
            c.save()
            c.rotate(d.toFloat(), m, m)
            txt(c, "%03d".format(d), m, m - r * 0.76f, r * 0.06f, dim, mono)
            c.restore()
        }
        c.restore()
    }

    /** Chevron markers, like contact blips on a tactical display. */
    override fun pinShape(c: Canvas, m: Float, r: Float, color: Int) {
        path.reset()
        path.moveTo(m, m - r * 0.995f)
        path.lineTo(m + r * 0.07f, m - r * 0.88f)
        path.lineTo(m, m - r * 0.915f)
        path.lineTo(m - r * 0.07f, m - r * 0.88f)
        path.close()
        fillP(color); c.drawPath(path, p)
        strokeP(pinOutline, r * 0.02f); c.drawPath(path, p)
    }

    override fun drawPins(c: Canvas, s: Float, markers: List<PinMarker>, rotation: Float) =
        pinLayer(c, s, markers, rotation)

    private fun brackets(c: Canvas, l: Float, t: Float, rt: Float, b: Float, len: Float, w: Float) {
        strokeP(green, w)
        c.drawLine(l, t + len, l, t, p); c.drawLine(l, t, l + len, t, p)
        c.drawLine(rt - len, t, rt, t, p); c.drawLine(rt, t, rt, t + len, p)
        c.drawLine(l, b - len, l, b, p); c.drawLine(l, b, l + len, b, p)
        c.drawLine(rt - len, b, rt, b, p); c.drawLine(rt, b, rt, b - len, p)
    }

    override fun drawTop(c: Canvas, s: Float, compact: Boolean) {
        val m = s / 2f
        val r = m * 0.96f
        // Heading pointer on the rim.
        path.reset()
        path.moveTo(m - r * 0.05f, m - r * 1.0f)
        path.lineTo(m + r * 0.05f, m - r * 1.0f)
        path.lineTo(m, m - r * 0.915f)
        path.close()
        fillP(green); c.drawPath(path, p)
        strokeP(hexc("#06100A"), r * 0.012f); c.drawPath(path, p)
        if (!compact) {
            // Readout plate with corner brackets.
            plate.set(m - r * 0.27f, m - r * 0.15f, m + r * 0.27f, m + r * 0.19f)
            fillP(Color.argb(225, 7, 13, 10)); c.drawRect(plate, p)
            brackets(c, plate.left, plate.top, plate.right, plate.bottom, r * 0.07f, r * 0.012f)
        }
    }

    override fun drawReadout(c: Canvas, s: Float, heading: Float) {
        val m = s / 2f
        val r = m * 0.96f
        txt(c, "${Math.round(heading) % 360}°", m, m + r * 0.04f, r * 0.17f, green, mono)
        txt(c, cardinalOf(heading), m, m + r * 0.15f, r * 0.08f, dim, mono)
    }
}
