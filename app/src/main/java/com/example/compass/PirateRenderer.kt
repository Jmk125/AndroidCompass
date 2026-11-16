package com.example.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Riveted brass, rope trim, a weathered chart face and a fleur-de-lis north. */
class PirateRenderer(context: Context) : StyleRenderer(context) {

    private val face = font(R.font.pirata_one, Typeface.SERIF)
    private val ink = hexc("#3B2412")
    private val red = hexc("#9B1C1C")
    private val oval = RectF()

    override val pinOutline: Int = hexc("#2A1A08")

    override fun drawFace(c: Canvas, s: Float) {
        val m = s / 2f
        val r = m * 0.96f
        p.reset(); p.isAntiAlias = true
        p.shader = LinearGradient(
            m - r, m - r, m + r, m + r,
            intArrayOf(hexc("#F1D98A"), hexc("#B98A2F"), hexc("#6F4D14"), hexc("#D9B45A")),
            floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(m, m, r, p)
        strokeP(hexc("#4A3210"), r * 0.012f); c.drawCircle(m, m, r, p)

        // Rope ring.
        fillP(hexc("#5A3D17")); c.drawCircle(m, m, r * 0.9f, p)
        for (i in 0 until 96) {
            val a = i * 2.0 * Math.PI / 96
            val x = m + (cos(a) * r * 0.86).toFloat()
            val y = m + (sin(a) * r * 0.86).toFloat()
            val dx = (cos(a + 0.9) * r * 0.03).toFloat()
            val dy = (sin(a + 0.9) * r * 0.03).toFloat()
            strokeP(if (i % 2 == 1) hexc("#D1AD72") else hexc("#8A6A38"), r * 0.02f)
            c.drawLine(x - dx, y - dy, x + dx, y + dy, p)
        }
        // Rivets.
        for (k in 0 until 12) {
            val ra = k * 2.0 * Math.PI / 12
            val rx = m + (cos(ra) * r * 0.955).toFloat()
            val ry = m + (sin(ra) * r * 0.955).toFloat()
            p.reset(); p.isAntiAlias = true
            p.shader = RadialGradient(
                rx - r * 0.006f, ry - r * 0.006f, r * 0.024f,
                intArrayOf(hexc("#FBEAA8"), hexc("#6F4D14")), null, Shader.TileMode.CLAMP
            )
            c.drawCircle(rx, ry, r * 0.022f, p)
        }
        // Parchment.
        p.reset(); p.isAntiAlias = true
        p.shader = RadialGradient(m, m, r * 0.82f, intArrayOf(hexc("#F0DFAE"), hexc("#C6A266")), null, Shader.TileMode.CLAMP)
        c.drawCircle(m, m, r * 0.82f, p)
        val rnd = Random(7)
        for (b in 0 until 14) {
            val ang = rnd.nextDouble() * 2 * Math.PI
            val dist = r * 0.62f * Math.sqrt(rnd.nextDouble()).toFloat()
            fillP(android.graphics.Color.argb(12, 120, 70, 20))
            c.drawCircle(m + (cos(ang) * dist).toFloat(), m + (sin(ang) * dist).toFloat(), r * (0.05f + rnd.nextFloat() * 0.15f), p)
        }
    }

    private fun pblade(c: Canvas, m: Float, rot: Float, len: Float, wid: Float, lt: Int, dk: Int, r: Float) {
        c.save()
        c.rotate(rot, m, m)
        path.reset(); path.moveTo(m, m); path.lineTo(m - wid, m - len * 0.3f); path.lineTo(m, m - len); path.close()
        fillP(lt); c.drawPath(path, p)
        path.reset(); path.moveTo(m, m); path.lineTo(m + wid, m - len * 0.3f); path.lineTo(m, m - len); path.close()
        fillP(dk); c.drawPath(path, p)
        path.reset()
        path.moveTo(m - wid, m - len * 0.3f); path.lineTo(m, m - len); path.lineTo(m + wid, m - len * 0.3f)
        path.lineTo(m, m); path.close()
        strokeP(ink, r * 0.006f); c.drawPath(path, p)
        c.restore()
    }

    override fun drawContent(c: Canvas, s: Float, rotation: Float) {
        val m = s / 2f
        val r = m * 0.96f
        c.save()
        c.rotate(rotation, m, m)
        strokeP(ink, r * 0.008f); c.drawCircle(m, m, r * 0.745f, p)
        for (d in 0 until 360 step 5) {
            val big = d % 30 == 0
            strokeP(ink, r * if (big) 0.015f else 0.007f)
            c.save()
            c.rotate(d.toFloat(), m, m)
            c.drawLine(m, m - r * 0.745f, m, m - r * if (big) 0.69f else 0.715f, p)
            c.restore()
        }
        for (d in intArrayOf(45, 135, 225, 315)) pblade(c, m, d.toFloat(), r * 0.34f, r * 0.05f, hexc("#8A6A3A"), ink, r)
        for (d in intArrayOf(90, 180, 270)) pblade(c, m, d.toFloat(), r * 0.42f, r * 0.075f, hexc("#8A6A3A"), ink, r)
        pblade(c, m, 0f, r * 0.42f, r * 0.075f, red, hexc("#5E1010"), r)

        // Fleur-de-lis on north.
        for ((ang, len) in listOf(0f to 0.1f, -38f to 0.075f, 38f to 0.075f)) {
            c.save()
            c.translate(m, m - r * 0.42f)
            c.rotate(ang)
            val ry = r * len
            val cy = -ry * 0.75f
            oval.set(-r * 0.028f, cy - ry, r * 0.028f, cy + ry)
            fillP(red); c.drawOval(oval, p)
            strokeP(ink, r * 0.006f); c.drawOval(oval, p)
            c.restore()
        }
        for ((d, t) in listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")) {
            c.save()
            c.rotate(d.toFloat(), m, m)
            txt(c, t, m, m - r * 0.6f, r * 0.18f, if (d == 0) red else ink, face)
            c.restore()
        }
        c.restore()
    }

    override fun drawPins(c: Canvas, s: Float, markers: List<PinMarker>, rotation: Float) =
        pinLayer(c, s, markers, rotation)

    override fun drawTop(c: Canvas, s: Float, compact: Boolean) {
        val m = s / 2f
        val r = m * 0.96f
        fillP(hexc("#EAD6A2")); c.drawCircle(m, m, r * 0.25f, p)
        strokeP(ink, r * 0.012f); c.drawCircle(m, m, r * 0.25f, p)
        if (!compact) {
            // Skull and crossbones.
            strokeP(ink, r * 0.016f)
            c.drawLine(m - r * 0.075f, m - r * 0.15f, m + r * 0.075f, m - r * 0.05f, p)
            c.drawLine(m + r * 0.075f, m - r * 0.15f, m - r * 0.075f, m - r * 0.05f, p)
            fillP(ink)
            c.drawCircle(m, m - r * 0.115f, r * 0.045f, p)
            c.drawRect(m - r * 0.025f, m - r * 0.095f, m + r * 0.025f, m - r * 0.055f, p)
            fillP(hexc("#EAD6A2"))
            c.drawCircle(m - r * 0.018f, m - r * 0.12f, r * 0.011f, p)
            c.drawCircle(m + r * 0.018f, m - r * 0.12f, r * 0.011f, p)
        }
        // Pointer.
        path.reset()
        path.moveTo(m, m - r * 1.0f); path.lineTo(m - r * 0.05f, m - r * 0.88f); path.lineTo(m + r * 0.05f, m - r * 0.88f)
        path.close()
        fillP(hexc("#C0392B")); c.drawPath(path, p)
        strokeP(hexc("#4A3210"), r * 0.014f); c.drawPath(path, p)
    }

    override fun drawReadout(c: Canvas, s: Float, heading: Float) {
        val m = s / 2f
        val r = m * 0.96f
        txt(c, "${Math.round(heading) % 360}°", m, m + r * 0.09f, r * 0.15f, ink, face)
        txt(c, cardinalOf(heading), m, m + r * 0.19f, r * 0.09f, red, face)
    }
}
