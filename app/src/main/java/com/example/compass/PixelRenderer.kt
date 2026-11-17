package com.example.compass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A 64 x 64 pixel dial with a small fixed palette, scaled up without smoothing.
 * Each layer is plotted into a pixel buffer and drawn separately, like the other styles.
 */
class PixelRenderer(context: Context) : StyleRenderer(context) {

    private val n = 64
    private val buf = IntArray(n * n)
    private val sharp = Paint().apply { isFilterBitmap = false; isAntiAlias = false; isDither = false }
    private val dst = RectF()

    private val bg = hexc("#1A1C2C")
    private val ringL = hexc("#94B0C2")
    private val ringD = hexc("#566C86")
    private val white = hexc("#F4F4F4")
    private val orange = hexc("#EF7D57")
    private val yellow = hexc("#FFCD75")
    private val cyan = hexc("#73EFF7")

    private val letters = mapOf(
        'N' to arrayOf("10001", "11001", "10101", "10011", "10001"),
        'E' to arrayOf("11111", "10000", "11110", "10000", "11111"),
        'S' to arrayOf("01111", "10000", "01110", "00001", "11110"),
        'W' to arrayOf("10001", "10001", "10101", "11011", "10001"),
    )
    private val digits = mapOf(
        '0' to arrayOf("111", "101", "101", "101", "111"), '1' to arrayOf("010", "110", "010", "010", "111"),
        '2' to arrayOf("111", "001", "111", "100", "111"), '3' to arrayOf("111", "001", "111", "001", "111"),
        '4' to arrayOf("101", "101", "111", "001", "001"), '5' to arrayOf("111", "100", "111", "001", "111"),
        '6' to arrayOf("111", "100", "111", "101", "111"), '7' to arrayOf("111", "001", "010", "010", "010"),
        '8' to arrayOf("111", "101", "111", "101", "111"), '9' to arrayOf("111", "101", "111", "001", "111"),
        '°' to arrayOf("111", "101", "111"),
    )

    private fun put(x: Int, y: Int, color: Int) {
        if (x < 0 || y < 0 || x >= n || y >= n) return
        buf[y * n + x] = color
    }

    private fun blit(rows: Array<String>, x0: Int, y0: Int, color: Int) {
        for (y in rows.indices) for (x in rows[y].indices) if (rows[y][x] == '1') put(x0 + x, y0 + y, color)
    }

    private fun flush(c: Canvas, s: Float) {
        // A new bitmap per layer: a hardware canvas reads it later, so a shared one would show only the last layer.
        val bmp = Bitmap.createBitmap(buf, n, n, Bitmap.Config.ARGB_8888)
        val scale = max(1, floor(s / n).toInt())
        val size = (n * scale).toFloat()
        val o = (s - size) / 2f
        dst.set(o, o, o + size, o + size)
        c.drawBitmap(bmp, null, dst, sharp)
        buf.fill(0)
    }

    override fun drawFace(c: Canvas, s: Float) {
        buf.fill(0)
        for (y in 0 until n) for (x in 0 until n) {
            val dx = x + 0.5f - n / 2f
            val dy = y + 0.5f - n / 2f
            val r = sqrt(dx * dx + dy * dy)
            if (r > 31.5f) continue
            put(x, y, if (r >= 30.5f) ringL else if (r >= 29.5f) ringD else bg)
        }
        flush(c, s)
    }

    override fun drawContent(c: Canvas, s: Float, rotation: Float) {
        buf.fill(0)
        for (y in 0 until n) for (x in 0 until n) {
            val dx = x + 0.5f - n / 2f
            val dy = y + 0.5f - n / 2f
            val r = sqrt(dx * dx + dy * dy)
            if (r >= 29.5f) continue
            var theta = Math.toDegrees(atan2(dx, -dy).toDouble()).toFloat()
            if (theta < 0f) theta += 360f
            var al = (theta - rotation) % 360f
            if (al < 0f) al += 360f
            fun near(step: Float): Float {
                val mm = al % step
                return sin(Math.toRadians(min(mm, step - mm).toDouble())).toFloat() * r
            }
            if (near(90f) < 0.8f && r >= 24f) put(x, y, if (al < 4f || al > 356f) orange else white)
            else if (near(45f) < 0.7f && r >= 26f) put(x, y, white)
            else if (near(15f) < 0.65f && r >= 27.5f) put(x, y, ringD)
        }
        // Letters stay upright; only their position on the ring turns.
        for ((deg, ch, color) in listOf(Triple(0f, 'N', orange), Triple(90f, 'E', white), Triple(180f, 'S', white), Triple(270f, 'W', white))) {
            val t = Math.toRadians((deg + rotation).toDouble())
            val lx = n / 2f + sin(t).toFloat() * 18f
            val ly = n / 2f - Math.cos(t).toFloat() * 18f
            blit(letters.getValue(ch), (lx - 2.5f).roundToInt(), (ly - 2.5f).roundToInt(), color)
        }
        flush(c, s)
    }

    override fun drawPins(c: Canvas, s: Float, markers: List<PinMarker>, rotation: Float) {
        if (markers.isEmpty()) return
        buf.fill(0)
        var ring = 0
        for (mk in markers) {
            if (!mk.arrived) continue
            val target = 28.5f - ring * 1.5f
            for (y in 0 until n) for (x in 0 until n) {
                val dx = x + 0.5f - n / 2f
                val dy = y + 0.5f - n / 2f
                if (kotlin.math.abs(sqrt(dx * dx + dy * dy) - target) < 0.6f) put(x, y, mk.color)
            }
            ring++
        }
        for (mk in markers) {
            val t = Math.toRadians((mk.angle + rotation).toDouble())
            val px = (n / 2f + sin(t).toFloat() * 26f - 0.5f).roundToInt()
            val py = (n / 2f - Math.cos(t).toFloat() * 26f - 0.5f).roundToInt()
            for ((ox, oy) in listOf(2 to 0, -2 to 0, 0 to 2, 0 to -2)) put(px + ox, py + oy, bg)
            for ((ox, oy) in listOf(0 to 0, 1 to 0, -1 to 0, 0 to 1, 0 to -1)) put(px + ox, py + oy, mk.color)
        }
        flush(c, s)
    }

    override fun drawTop(c: Canvas, s: Float, compact: Boolean) {
        buf.fill(0)
        for ((x, y) in listOf(
            31 to 1, 32 to 1, 30 to 2, 31 to 2, 32 to 2, 33 to 2,
            29 to 3, 30 to 3, 31 to 3, 32 to 3, 33 to 3, 34 to 3
        )) put(x, y, yellow)
        flush(c, s)
    }

    override fun drawReadout(c: Canvas, s: Float, heading: Float) {
        buf.fill(0)
        val str = (Math.round(heading) % 360).toString()
        val w = str.length * 4 + 4
        val sx = (32 - w / 2f).roundToInt()
        for (i in str.indices) blit(digits.getValue(str[i]), sx + i * 4, 26, white)
        blit(digits.getValue('°'), sx + str.length * 4, 26, white)
        val cd = cardinalOf(heading)
        val cx0 = (32 - (cd.length * 6 - 1) / 2f).roundToInt()
        for (j in cd.indices) blit(letters.getValue(cd[j]), cx0 + j * 6, 34, cyan)
        flush(c, s)
    }
}
