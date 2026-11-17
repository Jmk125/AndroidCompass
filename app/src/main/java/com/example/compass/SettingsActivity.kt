package com.example.compass

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
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
    private lateinit var backgroundButton: Button
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

        column.addView(TextView(this).apply {
            setText(R.string.widget_section)
            setTextColor(0xFFFFFFFF.toInt()); textSize = 18f
            setPadding(0, dp(20), 0, dp(2))
        })
        column.addView(TextView(this).apply {
            setText(R.string.widget_hint)
            setTextColor(gray)
            setPadding(0, 0, 0, dp(8))
        })
        column.addView(Button(this).apply {
            setText(R.string.add_widget)
            setOnClickListener { requestPinWidget() }
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        backgroundButton = Button(this).apply {
            setText(R.string.pins_on_widget)
            setOnClickListener {
                Toast.makeText(this@SettingsActivity, R.string.pins_on_widget_hint, Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }
        column.addView(backgroundButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        column.addView(TextView(this).apply {
            setText(R.string.calibrate_hint)
            setTextColor(gray)
            setPadding(0, dp(20), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(column) })
        refresh()
    }

    override fun onResume() {
        super.onResume()
        // Only offer this once location is allowed but not yet "all the time".
        backgroundButton.visibility =
            if (Permissions.hasFine(this) && !Permissions.hasBackground(this)) View.VISIBLE else View.GONE
    }

    private fun requestPinWidget() {
        val mgr = getSystemService(AppWidgetManager::class.java)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, CompassWidgetProvider::class.java), null, null)
        } else {
            Toast.makeText(this, R.string.add_widget_manual, Toast.LENGTH_LONG).show()
        }
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
