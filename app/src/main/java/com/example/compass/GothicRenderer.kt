package com.example.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface

/** Black stone and pewter, pointed arches, a blood-red north blade, blackletter lettering. */
class GothicRenderer(context: Context) : StyleRenderer(context) {

    private val face = font(R.font.unifraktur_cook, Typeface.SERIF)
    private val bone = hexc("#D8D0BF")
    private val boneD = hexc("#8F887A")
    private val blood = hexc("#B3222D")
    private val bloodD = hexc("#6E1620")

    override val pinOutline: Int = hexc("#050407")

    override fun drawFace(c: Canvas, s: Float) {
        val m = s / 2f
        val r = m * 0.96f
        p.reset(); p.isAntiAlias = true
        p.shader = LinearGradient(
            m - r, m - r, m + r, m + r,
            intArrayOf(hexc("#A29FAE"), hexc("#47434F"), hexc("#8B8898")), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(m, m, r, p)
        p.reset(); p.isAntiAlias = true
        p.shader = RadialGradient(m, m, r * 0.93f, intArrayOf(hexc("#271C2E"), hexc("#0A080D")), null, Shader.TileMode.CLAMP)
        c.drawCircle(m, m, r * 0.93f, p)
        strokeP(hexc("#C9C6D4"), r * 0.012f); c.drawCircle(m, m, r * 0.93f, p)
        strokeP(hexc("#6D6978"), r * 0.007f); c.drawCircle(m, m, r * 0.77f, p)
    }

    private fun blade(c: Canvas, m: Float, rot: Float, len: Float, wid: Float, lt: Int, dk: Int) {
        c.save()
        c.rotate(rot, m, m)
        path.reset(); path.moveTo(m, m); path.lineTo(m - wid, m - len * 0.28f); path.lineTo(m, m - len); path.close()
        fillP(lt); c.drawPath(path, p)
        path.reset(); path.moveTo(m, m); path.lineTo(m + wid, m - len * 0.28f); path.lineTo(m, m - len); path.close()
        fillP(dk); c.drawPath(path, p)
        c.restore()
    }

    override fun drawContent(c: Canvas, s: Float, rotation: Float) {
        val m = s / 2f
        val r = m * 0.96f
        // Pointed arches around the rim.
        for (i in 0 until 24) {
            if (i % 6 == 0) continue
            c.save()
            c.rotate(rotation + i * 15f, m, m)
            val w = r * 0.045f
            val base = m - r * 0.775f
            val tip = m - r * 0.915f
            path.reset()
            path.moveTo(m - w, base)
            path.quadTo(m - w, m - r * 0.86f, m, tip)
            path.quadTo(m + w, m - r * 0.86f, m + w, base)
            path.close()
            fillP(hexc("#17111C")); c.drawPath(path, p)
            strokeP(boneD, r * 0.011f); c.drawPath(path, p)
            c.restore()
        }
        // Letter medallions.
        for ((d, t) in listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")) {
            c.save()
            c.rotate(rotation + d, m, m)
            fillP(hexc("#120D16")); c.drawCircle(m, m - r * 0.85f, r * 0.095f, p)
            strokeP(if (d == 0) blood else bone, r * 0.018f); c.drawCircle(m, m - r * 0.85f, r * 0.095f, p)
            txt(c, t, m, m - r * 0.85f + r * 0.055f, r * 0.17f, if (d == 0) hexc("#E0505A") else bone, face)
            c.restore()
        }
        // Eight-point rose of faceted blades.
        for (d in intArrayOf(45, 135, 225, 315)) blade(c, m, rotation + d, r * 0.5f, r * 0.07f, boneD, hexc("#5D574D"))
        for (d in intArrayOf(90, 180, 270)) blade(c, m, rotation + d, r * 0.7f, r * 0.1f, bone, boneD)
        blade(c, m, rotation, r * 0.7f, r * 0.1f, blood, bloodD)
    }

    override fun pinShape(c: Canvas, m: Float, r: Float, color: Int) {
        path.reset()
        path.moveTo(m, m - r * 0.995f)
        path.lineTo(m + r * 0.055f, m - r * 0.9f)
        path.lineTo(m, m - r * 0.81f)
        path.lineTo(m - r * 0.055f, m - r * 0.9f)
        path.close()
        fillP(color); c.drawPath(path, p)
        strokeP(pinOutline, r * 0.02f); c.drawPath(path, p)
    }

    override fun drawPins(c: Canvas, s: Float, markers: List<PinMarker>, rotation: Float) =
        pinLayer(c, s, markers, rotation)

    override fun drawTop(c: Canvas, s: Float, compact: Boolean) {
        val m = s / 2f
        val r = m * 0.96f
        fillP(hexc("#0E0B11")); c.drawCircle(m, m, r * 0.3f, p)
        strokeP(bone, r * 0.015f); c.drawCircle(m, m, r * 0.3f, p)
        strokeP(hexc("#5D574D"), r * 0.006f); c.drawCircle(m, m, r * 0.265f, p)
        // Dagger pointer.
        path.reset()
        path.moveTo(m, m - r * 1.0f); path.lineTo(m - r * 0.04f, m - r * 0.86f); path.lineTo(m + r * 0.04f, m - r * 0.86f)
        path.close()
        fillP(hexc("#E8E6EE")); c.drawPath(path, p)
        strokeP(hexc("#0A080D"), r * 0.012f); c.drawPath(path, p)
        fillP(blood); c.drawCircle(m, m - r * 0.875f, r * 0.018f, p)
    }

    override fun drawReadout(c: Canvas, s: Float, heading: Float) {
        val m = s / 2f
        val r = m * 0.96f
        txt(c, "${Math.round(heading) % 360}°", m, m + r * 0.04f, r * 0.19f, bone, face)
        txt(c, cardinalOf(heading), m, m + r * 0.17f, r * 0.12f, hexc("#B9B2A2"), face)
    }
}
