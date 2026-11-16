package com.example.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Stitched leather, aged paper, a sheriff-star north and saloon lettering. */
class WesternRenderer(context: Context) : StyleRenderer(context) {

    private val face = font(R.font.rye, Typeface.SERIF)
    private val ink = hexc("#3A2414")
    private val brass = hexc("#C79A3B")
    private val red = hexc("#A3241B")
    private val cream = hexc("#F2E4C2")
    private val arc = RectF()

    override val pinOutline: Int = hexc("#2A170A")

    override fun drawFace(c: Canvas, s: Float) {
        val m = s / 2f
        val r = m * 0.96f
        p.reset(); p.isAntiAlias = true
        p.shader = LinearGradient(
            m - r, m - r, m + r, m + r, intArrayOf(hexc("#74461F"), hexc("#341E0E")), null, Shader.TileMode.CLAMP
        )
        c.drawCircle(m, m, r, p)
        val rnd = Random(3)
        for (i in 0 until 260) {
            val a = rnd.nextDouble() * 2 * Math.PI
            val d = r * (0.85f + rnd.nextFloat() * 0.15f)
            val dot = r * 0.006f + rnd.nextFloat() * r * 0.006f
            fillP(if (rnd.nextBoolean()) Color.argb(64, 0, 0, 0) else Color.argb(30, 255, 220, 170))
            c.drawCircle(m + (cos(a) * d).toFloat(), m + (sin(a) * d).toFloat(), dot, p)
        }
        // Stitching.
        val rs = r * 0.92f
        arc.set(m - rs, m - rs, m + rs, m + rs)
        strokeP(hexc("#E9D6A6"), r * 0.013f)
        for (t in 0 until 72) c.drawArc(arc, t * 5f - 1.15f, 2.3f, false, p)
        strokeP(brass, r * 0.028f); c.drawCircle(m, m, r * 0.835f, p)

        // Paper.
        p.reset(); p.isAntiAlias = true
        p.shader = RadialGradient(m, m, r * 0.82f, intArrayOf(hexc("#F2E4C2"), hexc("#D6BD8A")), null, Shader.TileMode.CLAMP)
        c.drawCircle(m, m, r * 0.82f, p)
        for (i in 0 until 90) {
            val a = rnd.nextDouble() * 2 * Math.PI
            val d = r * 0.8f * Math.sqrt(rnd.nextDouble()).toFloat()
            fillP(Color.argb(46, 90, 55, 20))
            c.drawCircle(m + (cos(a) * d).toFloat(), m + (sin(a) * d).toFloat(), r * 0.004f + rnd.nextFloat() * r * 0.006f, p)
        }
        strokeP(ink, r * 0.007f); c.drawCircle(m, m, r * 0.78f, p)
    }

    override fun drawContent(c: Canvas, s: Float, rotation: Float) {
        val m = s / 2f
        val r = m * 0.96f
        c.save()
        c.rotate(rotation, m, m)
        for (d in 0 until 360 step 5) {
            val big = d % 30 == 0
            strokeP(ink, r * if (big) 0.016f else 0.008f)
            c.save()
            c.rotate(d.toFloat(), m, m)
            c.drawLine(m, m - r * 0.78f, m, m - r * if (big) 0.71f else 0.745f, p)
            c.restore()
        }
        for (d in 30 until 360 step 30) {
            if (d % 90 == 0) continue
            c.save()
            c.rotate(d.toFloat(), m, m)
            txt(c, d.toString(), m, m - r * 0.655f, r * 0.06f, Color.argb(204, 58, 36, 20), face)
            c.restore()
        }
        for (d in intArrayOf(45, 135, 225, 315)) {
            c.save()
            c.rotate(d.toFloat(), m, m)
            starPath(m, m - r * 0.5f, r * 0.065f, r * 0.028f, 5)
            fillP(brass); c.drawPath(path, p)
            strokeP(ink, r * 0.008f); c.drawPath(path, p)
            c.restore()
        }
        for ((d, t) in listOf(90 to "E", 180 to "S", 270 to "W")) {
            c.save()
            c.rotate(d.toFloat(), m, m)
            txt(c, t, m, m - r * 0.5f + r * 0.06f, r * 0.17f, ink, face)
            c.restore()
        }
        // Sheriff-star north.
        val ny = m - r * 0.5f
        starPath(m, ny, r * 0.19f, r * 0.085f, 6)
        fillP(red); c.drawPath(path, p)
        strokeP(ink, r * 0.01f); c.drawPath(path, p)
        for (i in 0 until 6) {
            val a = -Math.PI / 2 + i * Math.PI / 3
            val tx = m + (cos(a) * r * 0.19).toFloat()
            val ty = ny + (sin(a) * r * 0.19).toFloat()
            fillP(brass); c.drawCircle(tx, ty, r * 0.024f, p)
            strokeP(ink, r * 0.01f); c.drawCircle(tx, ty, r * 0.024f, p)
        }
        txt(c, "N", m, ny + r * 0.05f, r * 0.14f, cream, face)
        c.restore()
    }

    override fun drawPins(c: Canvas, s: Float, markers: List<PinMarker>, rotation: Float) =
        pinLayer(c, s, markers, rotation)

    override fun drawTop(c: Canvas, s: Float, compact: Boolean) {
        val m = s / 2f
        val r = m * 0.96f
        path.reset()
        path.moveTo(m, m - r * 1.0f); path.lineTo(m - r * 0.05f, m - r * 0.88f); path.lineTo(m + r * 0.05f, m - r * 0.88f)
        path.close()
        fillP(brass); c.drawPath(path, p)
        strokeP(hexc("#2A170A"), r * 0.014f); c.drawPath(path, p)
    }

    override fun drawReadout(c: Canvas, s: Float, heading: Float) {
        val m = s / 2f
        val r = m * 0.96f
        txt(c, "${Math.round(heading) % 360}°", m, m + r * 0.05f, r * 0.2f, ink, face)
        txt(c, cardinalOf(heading), m, m + r * 0.19f, r * 0.1f, red, face)
    }
}
