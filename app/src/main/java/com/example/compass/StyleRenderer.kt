package com.example.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import kotlin.math.cos
import kotlin.math.sin

private val CARDINALS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

fun cardinalOf(heading: Float): String =
    CARDINALS[(((heading % 360f) + 360f + 22.5f) / 45f).toInt() % 8]

fun hexc(s: String): Int = Color.parseColor(s)

/**
 * One compass look. The compass is built from layers so the home screen widget can send
 * the dial image once and then only rotate it:
 *   face (static) + content (ticks, letters, rose; rotates) + pins (rotate) + top (static) + readout (text).
 * `rotation` is how far the dial is turned, in degrees clockwise (the app passes -heading).
 */
abstract class StyleRenderer(protected val context: Context) {

    abstract fun drawFace(c: Canvas, s: Float)
    abstract fun drawContent(c: Canvas, s: Float, rotation: Float)
    abstract fun drawPins(c: Canvas, s: Float, markers: List<PinMarker>, rotation: Float)

    /** Static centre piece and pointer. [compact] leaves out extras that need room (the widget). */
    abstract fun drawTop(c: Canvas, s: Float, compact: Boolean)
    abstract fun drawReadout(c: Canvas, s: Float, heading: Float)

    /** The whole compass, centred in a w x h area. */
    fun drawFull(c: Canvas, w: Float, h: Float, heading: Float, markers: List<PinMarker>) {
        val s = minOf(w, h)
        c.save()
        c.translate((w - s) / 2f, (h - s) / 2f)
        drawFace(c, s)
        drawContent(c, s, -heading)
        drawPins(c, s, markers, -heading)
        drawTop(c, s, false)
        drawReadout(c, s, heading)
        c.restore()
    }

    // ---------- helpers for the styles ----------

    protected val p = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val path = Path()

    protected fun font(resId: Int, fallback: Typeface): Typeface =
        runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() ?: fallback

    protected fun fillP(color: Int) {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL; p.color = color
    }

    protected fun strokeP(color: Int, width: Float) {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.color = color
        p.strokeWidth = width; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
    }

    protected fun txt(c: Canvas, str: String, x: Float, y: Float, size: Float, color: Int, tf: Typeface) {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        p.color = color; p.textSize = size; p.textAlign = Paint.Align.CENTER; p.typeface = tf
        c.drawText(str, x, y, p)
    }

    protected fun starPath(x: Float, y: Float, outer: Float, inner: Float, points: Int) {
        path.reset()
        for (i in 0 until points * 2) {
            val rad = if (i % 2 == 0) outer else inner
            val a = -Math.PI / 2 + i * Math.PI / points
            val px = x + (cos(a) * rad).toFloat()
            val py = y + (sin(a) * rad).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
    }

    protected open val pinOutline: Int = hexc("#14171C")

    /** One pin arrow, pointing outward from the top of the dial. */
    protected open fun pinShape(c: Canvas, m: Float, r: Float, color: Int) {
        path.reset()
        path.moveTo(m, m - r * 0.995f)
        path.lineTo(m - r * 0.075f, m - r * 0.86f)
        path.lineTo(m + r * 0.075f, m - r * 0.86f)
        path.close()
        fillP(color); c.drawPath(path, p)
        strokeP(pinOutline, r * 0.02f); c.drawPath(path, p)
    }

    /** Arrows for every pin, plus a coloured ring around the edge for pins you're standing at. */
    protected fun pinLayer(c: Canvas, s: Float, markers: List<PinMarker>, rotation: Float) {
        if (markers.isEmpty()) return
        val m = s / 2f
        val r = m * 0.96f
        var ring = 0
        for (mk in markers) {
            if (!mk.arrived) continue
            strokeP(mk.color, r * 0.045f)
            c.drawCircle(m, m, r * (0.975f - ring * 0.05f), p)
            ring++
        }
        for (mk in markers) {
            c.save()
            c.rotate(rotation + mk.angle, m, m)
            pinShape(c, m, r, mk.color)
            c.restore()
        }
    }
}
