package com.example.compass

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var compassView: CompassView
    private lateinit var sensor: HeadingSensor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        compassView = CompassView(this)
        sensor = HeadingSensor(this) { compassView.heading = it }

        val dp = resources.displayMetrics.density
        val addWidget = Button(this).apply {
            text = getString(R.string.add_widget)
            setOnClickListener { requestPinWidget() }
        }
        val note = TextView(this).apply {
            text = if (sensor.isAvailable) getString(R.string.calibrate_hint) else getString(R.string.no_sensor)
            setTextColor(0xFF8A93A0.toInt())
            gravity = Gravity.CENTER
            setPadding((24 * dp).toInt(), 0, (24 * dp).toInt(), (8 * dp).toInt())
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, (24 * dp).toInt(), 0, (24 * dp).toInt())
            addView(compassView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(note, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(addWidget, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
        setContentView(FrameLayout(this).apply { addView(column, MATCH_PARENT, MATCH_PARENT) })

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() { super.onResume(); sensor.start() }
    override fun onPause() { super.onPause(); sensor.stop() }

    private fun requestPinWidget() {
        val mgr = getSystemService(AppWidgetManager::class.java)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, CompassWidgetProvider::class.java), null, null)
        } else {
            Toast.makeText(this, R.string.add_widget_manual, Toast.LENGTH_LONG).show()
        }
    }
}
