package com.example.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Offline "radar": you in the middle, pins as coloured dots at their real distance and direction.
 * Tap to change zoom, long-press to switch between heading-up and north-up.
 * Everything is in the same magnetic frame as the compass dial.
 */
class RadarView(context: Context) : View(context) {

    var heading: Float = 0f
        set(value) { field = value; invalidate() }

    var statuses: List<PinNavigator.Status> = emptyList()
        set(value) { field = value; invalidate() }

    var hasLocation: Boolean = false
        set(value) { field = value; invalidate() }

    private var headingUp = true
    private var zoomIndex = -1 // -1 = auto-fit to the farthest pin

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#14171C") }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A414C"); style = Paint.Style.STROKE
    }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A303A"); style = Paint.Style.STROKE
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val textOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#14171C"); style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND
    }
    private val gray = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8A93A0") }
    private val north = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5A4D"); textAlign = Paint.Align.CENTER
    }
    private val you = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val youOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#14171C"); style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    init {
        setOnClickListener {
            zoomIndex = if (zoomIndex + 1 >= ZOOMS.size) -1 else zoomIndex + 1
            invalidate()
        }
        setOnLongClickListener {
            headingUp = !headingUp
            invalidate()
            true
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f * 0.96f
        val plotR = r * 0.85f

        canvas.drawCircle(cx, cy, r, bg)
        ring.strokeWidth = r * 0.015f
        canvas.drawCircle(cx, cy, r, ring)

        val maxDist = statuses.maxOfOrNull { it.distance } ?: 0f
        val range = if (zoomIndex >= 0) ZOOMS[zoomIndex] else niceRange(maxDist * 1.15f)
        val scale = plotR / range

        // Distance rings at half range and full range, labelled.
        grid.strokeWidth = r * 0.008f
        gray.textSize = r * 0.06f
        gray.textAlign = Paint.Align.CENTER
        for (frac in floatArrayOf(0.5f, 1f)) {
            canvas.drawCircle(cx, cy, plotR * frac, grid)
            canvas.drawText(formatRange(range * frac), cx, cy - plotR * frac - r * 0.012f, gray)
        }
        grid.strokeWidth = r * 0.006f
        canvas.drawLine(cx - plotR, cy, cx + plotR, cy, grid)
        canvas.drawLine(cx, cy - plotR, cx, cy + plotR, grid)

        // Magnetic north marker on the rim (top when north-up, rotates with you when heading-up).
        val rotation = if (headingUp) heading else 0f
        north.textSize = r * 0.09f
        val nAngle = Math.toRadians((-rotation).toDouble())
        canvas.drawText(
            "N",
            cx + (sin(nAngle) * r * 0.92).toFloat(),
            cy - (cos(nAngle) * r * 0.92).toFloat() + north.textSize * 0.35f,
            north
        )

        // Pins.
        val dotR = r * 0.035f
        text.textSize = r * 0.07f
        textOutline.textSize = text.textSize
        textOutline.strokeWidth = r * 0.02f
        for (s in statuses) {
            val a = Math.toRadians((s.magneticBearing - rotation).toDouble())
            var d = s.distance * scale
            val beyond = d > plotR * 1.05f
            if (beyond) d = plotR * 1.05f
            val x = cx + (sin(a) * d).toFloat()
            val y = cy - (cos(a) * d).toFloat()

            if (s.arrived) {
                dotRing.color = s.pin.color
                dotRing.strokeWidth = r * 0.012f
                canvas.drawCircle(x, y, dotR * 2.4f, dotRing)
            }
            if (beyond) {
                // Off the edge at this zoom: hollow dot on the rim.
                dotRing.color = s.pin.color
                dotRing.strokeWidth = r * 0.012f
                canvas.drawCircle(x, y, dotR, dotRing)
            } else {
                dot.color = s.pin.color
                canvas.drawCircle(x, y, dotR, dot)
            }
            val label = s.pin.name
            val labelW = text.measureText(label)
            val lx = if (x + dotR * 1.8f + labelW > w - 4f) x - dotR * 1.8f - labelW else x + dotR * 1.8f
            val ly = y + text.textSize * 0.35f
            text.textAlign = Paint.Align.LEFT
            textOutline.textAlign = Paint.Align.LEFT
            canvas.drawText(label, lx, ly, textOutline)
            canvas.drawText(label, lx, ly, text)
        }

        // You, in the middle: points where you're facing.
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(if (headingUp) 0f else heading)
        val u = r * 0.05f
        path.reset()
        path.moveTo(0f, -u * 1.4f)
        path.lineTo(-u, u)
        path.lineTo(0f, u * 0.4f)
        path.lineTo(u, u)
        path.close()
        youOutline.strokeWidth = r * 0.012f
        canvas.drawPath(path, youOutline)
        canvas.drawPath(path, you)
        canvas.restore()

        // Status lines.
        gray.textAlign = Paint.Align.CENTER
        gray.textSize = r * 0.065f
        val modeName = context.getString(if (headingUp) R.string.radar_heading_up else R.string.radar_north_up)
        val zoomName = if (zoomIndex < 0) context.getString(R.string.radar_auto) else formatRange(range)
        canvas.drawText("$modeName · $zoomName", cx, cy + r * 0.62f, gray)
        gray.textSize = r * 0.055f
        canvas.drawText(context.getString(R.string.radar_hint), cx, cy + r * 0.72f, gray)

        val message = when {
            !hasLocation -> R.string.radar_waiting
            statuses.isEmpty() -> R.string.radar_no_pins
            else -> null
        }
        if (message != null) {
            gray.textSize = r * 0.07f
            canvas.drawText(context.getString(message), cx, cy - r * 0.25f, gray)
        }
    }

    companion object {
        private val ZOOMS = floatArrayOf(50f, 100f, 250f, 500f, 1000f, 5000f)
        private val NICE = floatArrayOf(50f, 100f, 200f, 250f, 500f, 1000f, 2000f, 5000f, 10000f, 20000f, 50000f)

        private fun niceRange(needed: Float): Float = NICE.firstOrNull { it >= needed } ?: NICE.last()

        private fun formatRange(m: Float): String = when {
            m < 1000f -> "${m.roundToInt()} m"
            m % 1000f == 0f -> "${(m / 1000f).roundToInt()} km"
            else -> String.format("%.1f km", m / 1000f)
        }
    }
}
