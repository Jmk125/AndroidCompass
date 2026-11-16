package com.example.compass

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Pick the compass style. Applies to the app, the floating compass and the widget. */
class SettingsActivity : Activity() {

    private lateinit var rows: Map<CompassStyle, LinearLayout>
    private val accent = 0xFFFF5A4D.toInt()
    private val gray = 0xFF8A93A0.toInt()

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(48), dp(16), dp(48))
        }
        column.addView(TextView(this).apply {
            setText(R.string.settings)
            setTextColor(0xFFFFFFFF.toInt()); textSize = 28f
        })
        column.addView(TextView(this).apply {
            setText(R.string.compass_style)
            setTextColor(0xFFFFFFFF.toInt()); textSize = 18f
            setPadding(0, dp(20), 0, dp(2))
        })
        column.addView(TextView(this).apply {
            setText(R.string.compass_style_hint)
            setTextColor(gray)
            setPadding(0, 0, 0, dp(12))
        })

        val built = LinkedHashMap<CompassStyle, LinearLayout>()
        for (style in CompassStyle.values()) {
            val preview = StylePreview(this, style)
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@SettingsActivity).apply {
                    text = style.label; textSize = 20f; setTextColor(0xFFFFFFFF.toInt())
                })
                addView(TextView(this@SettingsActivity).apply {
                    text = style.blurb; textSize = 14f; setTextColor(gray)
                })
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                setOnClickListener { choose(style) }
                addView(preview, LinearLayout.LayoutParams(dp(104), dp(104)).apply { rightMargin = dp(14) })
                addView(texts, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            }
            column.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(10) })
            built[style] = row
        }
        rows = built
        setContentView(ScrollView(this).apply { addView(column) })
        refresh()
    }

    private fun choose(style: CompassStyle) {
        StylePrefs.set(this, style)
        CompassWidgetProvider.update(this) // restyle any widget already on the home screen
        refresh()
    }

    private fun refresh() {
        val current = StylePrefs.get(this)
        for ((style, row) in rows) {
            row.background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(0xFF14171C.toInt())
                setStroke(dp(if (style == current) 3 else 1), if (style == current) accent else 0xFF2A303A.toInt())
            }
        }
    }

    /** A small static preview of one style, with two sample pins. */
    private class StylePreview(context: Context, style: CompassStyle) : View(context) {
        private val renderer = style.createRenderer(context)
        private val markers = listOf(
            PinMarker(40f, 0xFFFFD60A.toInt(), false),
            PinMarker(205f, 0xFF32D4E6.toInt(), false)
        )
        override fun onDraw(canvas: Canvas) {
            renderer.drawFull(canvas, width.toFloat(), height.toFloat(), 127f, markers)
        }
    }
}
