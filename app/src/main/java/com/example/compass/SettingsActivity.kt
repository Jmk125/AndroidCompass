package com.example.compass

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Pick the compass style (which also styles the whole app), and set up the widget. */
class SettingsActivity : Activity() {

    private lateinit var t: UiTheme
    private lateinit var backgroundButton: Button

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        t = UiTheme.of(this)
        t.applyWindow(this)
        val current = StylePrefs.get(this)

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }
        fun heading(textRes: Int, topDp: Int) = TextView(this).apply {
            setText(textRes); t.styleTitle(this, 20f); setPadding(0, dp(topDp), 0, dp(2))
        }
        fun muted(textRes: Int, bottomDp: Int) = TextView(this).apply {
            setText(textRes); t.styleBody(this, 14f, muted = true); setPadding(0, 0, 0, dp(bottomDp))
        }

        column.addView(TextView(this).apply { setText(R.string.settings); t.styleTitle(this, 30f) })
        column.addView(heading(R.string.compass_style, 18))
        column.addView(muted(R.string.compass_style_hint, 12))

        for (style in CompassStyle.values()) {
            val selected = style == current
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@SettingsActivity).apply { text = style.label; t.styleTitle(this, 21f) })
                addView(TextView(this@SettingsActivity).apply { text = style.blurb; t.styleBody(this, 14f, muted = true) })
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = t.box(this@SettingsActivity, t.surface, if (selected) t.accent else t.border, if (selected) 3 else 1)
                setOnClickListener { choose(style) }
                addView(StylePreview(this@SettingsActivity, style), LinearLayout.LayoutParams(dp(104), dp(104)).apply { rightMargin = dp(14) })
                addView(texts, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            }
            column.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(10) })
        }

        column.addView(heading(R.string.widget_section, 16))
        column.addView(muted(R.string.widget_hint, 8))
        column.addView(t.newButton(this, getString(R.string.add_widget)) { requestPinWidget() },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        backgroundButton = t.newButton(this, getString(R.string.pins_on_widget)) {
            Toast.makeText(this, R.string.pins_on_widget_hint, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        column.addView(backgroundButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) })
        column.addView(TextView(this).apply {
            setText(R.string.calibrate_hint); t.styleBody(this, 14f, muted = true); setPadding(0, dp(20), 0, 0)
        })

        val scroll = ScrollView(this).apply {
            setBackgroundColor(t.bg)
            clipToPadding = false
            addView(column)
        }
        applySystemInsets(scroll, extraTop = dp(8), extraBottom = dp(8), extraSide = 0)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        // Only offer this once location is allowed but not yet "all the time".
        backgroundButton.visibility =
            if (Permissions.hasFine(this) && !Permissions.hasBackground(this)) View.VISIBLE else View.GONE
    }

    private fun choose(style: CompassStyle) {
        if (style == StylePrefs.get(this)) return
        StylePrefs.set(this, style)
        CompassWidgetProvider.update(this) // restyle any widget already on the home screen
        recreate() // rebuild this screen in the new look
    }

    private fun requestPinWidget() {
        val mgr = getSystemService(AppWidgetManager::class.java)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, CompassWidgetProvider::class.java), null, null)
        } else {
            Toast.makeText(this, R.string.add_widget_manual, Toast.LENGTH_LONG).show()
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
