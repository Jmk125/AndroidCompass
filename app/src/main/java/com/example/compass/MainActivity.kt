package com.example.compass

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var compassView: CompassView
    private lateinit var radarView: RadarView
    private lateinit var compassTab: Button
    private lateinit var radarTab: Button
    private lateinit var sensor: HeadingSensor
    private lateinit var navigator: PinNavigator
    private lateinit var floatingButton: Button
    private lateinit var backgroundButton: Button
    private lateinit var statusText: TextView
    private lateinit var pinsContainer: LinearLayout

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        compassView = CompassView(this)
        radarView = RadarView(this)
        sensor = HeadingSensor(this) {
            compassView.heading = it
            radarView.heading = it
        }
        navigator = PinNavigator(this) {
            compassView.markers = navigator.markers
            radarView.statuses = navigator.statuses
            radarView.hasLocation = navigator.location != null
            updateStatus()
            refreshPins()
        }

        val gray = 0xFF8A93A0.toInt()
        statusText = TextView(this).apply { setTextColor(gray); gravity = Gravity.CENTER }
        val pinButton = Button(this).apply {
            setText(R.string.pin_here)
            setOnClickListener { pinHere() }
        }
        val pinsTitle = TextView(this).apply {
            setText(R.string.pins_title)
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 20f
            setPadding(0, dp(16), 0, dp(4))
        }
        pinsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        backgroundButton = Button(this).apply {
            setText(R.string.pins_on_widget)
            setOnClickListener {
                Toast.makeText(this@MainActivity, R.string.pins_on_widget_hint, Toast.LENGTH_LONG).show()
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                )
            }
        }
        floatingButton = Button(this).apply { setOnClickListener { toggleFloating() } }
        val addWidget = Button(this).apply {
            setText(R.string.add_widget)
            setOnClickListener { requestPinWidget() }
        }
        val note = TextView(this).apply {
            text = if (sensor.isAvailable) getString(R.string.calibrate_hint) else getString(R.string.no_sensor)
            setTextColor(gray)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        val settingsButton = Button(this).apply {
            setText(R.string.settings)
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(64))
            addView(statusText, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(pinButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(pinsTitle, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(pinsContainer, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(backgroundButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(floatingButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(addWidget, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(note, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(settingsButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        compassTab = Button(this).apply {
            setText(R.string.tab_compass)
            setOnClickListener { showView(radar = false) }
        }
        radarTab = Button(this).apply {
            setText(R.string.tab_radar)
            setOnClickListener { showView(radar = true) }
        }
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), 0)
            addView(compassTab, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(radarTab, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        }
        val viewFrame = FrameLayout(this).apply {
            addView(compassView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(radarView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(32), 0, 0)
            addView(tabs, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(viewFrame, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(ScrollView(this@MainActivity).apply { addView(content) }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }
        setContentView(root)
        showView(radar = getSharedPreferences("ui", MODE_PRIVATE).getBoolean("radar", false))

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        sensor.start()
        navigator.start()
        updateFloatingButton()
        updateStatus()
        refreshPins()
        backgroundButton.visibility =
            if (Permissions.hasFine(this) && !Permissions.hasBackground(this)) View.VISIBLE else View.GONE
    }

    override fun onPause() {
        super.onPause()
        sensor.stop()
        navigator.stop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION && Permissions.hasFine(this)) {
            navigator.start()
            onResume()
        }
    }

    private fun showView(radar: Boolean) {
        radarView.visibility = if (radar) View.VISIBLE else View.GONE
        compassView.visibility = if (radar) View.GONE else View.VISIBLE
        radarTab.alpha = if (radar) 1f else 0.5f
        compassTab.alpha = if (radar) 0.5f else 1f
        getSharedPreferences("ui", MODE_PRIVATE).edit().putBoolean("radar", radar).apply()
    }

    // ---------- pins ----------

    private fun updateStatus() {
        val loc = navigator.location
        statusText.text = when {
            !Permissions.hasFine(this) -> getString(R.string.loc_off)
            loc == null -> getString(R.string.loc_waiting)
            navigator.isGps -> getString(R.string.loc_accuracy, loc.accuracy.roundToInt())
            else -> getString(R.string.loc_weak, loc.accuracy.roundToInt())
        }
    }

    private fun pinHere() {
        if (!Permissions.hasFine(this)) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        val loc = navigator.freshLocation()
        if (loc == null) {
            Toast.makeText(this, R.string.waiting_gps, Toast.LENGTH_LONG).show()
            return
        }
        val savePin = {
            val initial = getString(R.string.default_pin_name, PinStore.load(this).size + 1)
            promptName(initial) { name ->
                PinStore.add(this, name, loc.latitude, loc.longitude)
                pinsChanged()
            }
        }
        if (navigator.isGps && loc.accuracy <= 30f) {
            savePin()
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.weak_title)
                .setMessage(getString(R.string.weak_message, loc.accuracy.roundToInt()))
                .setPositiveButton(R.string.pin_anyway) { _, _ -> savePin() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun pinsChanged() {
        navigator.refresh()
        refreshPins()
    }

    private fun refreshPins() {
        pinsContainer.removeAllViews()
        val pins = PinStore.load(this)
        if (pins.isEmpty()) {
            pinsContainer.addView(TextView(this).apply {
                setText(R.string.no_pins)
                setTextColor(0xFF8A93A0.toInt())
            })
            return
        }
        for (pin in pins) {
            val status = navigator.statuses.firstOrNull { it.pin.id == pin.id }
            val subtitle = when {
                status == null -> getString(R.string.pin_waiting)
                status.arrived -> "${getString(R.string.pin_here_now)} · ${formatDistance(status.distance)}"
                else -> "${formatDistance(status.distance)} · ${cardinalOf(status.trueBearing)}"
            }
            val swatch = View(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(pin.color) }
            }
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@MainActivity).apply {
                    text = pin.name; textSize = 17f; setTextColor(0xFFFFFFFF.toInt())
                })
                addView(TextView(this@MainActivity).apply {
                    text = subtitle; textSize = 14f
                    setTextColor(if (status?.arrived == true) pin.color else 0xFF8A93A0.toInt())
                })
            }
            pinsContainer.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener { showPinOptions(pin) }
                addView(swatch, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(14) })
                addView(texts, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            })
        }
    }

    private fun formatDistance(meters: Float) =
        if (meters < 1000f) "${meters.roundToInt()} m" else String.format("%.1f km", meters / 1000f)

    private fun showPinOptions(pin: Pin) {
        AlertDialog.Builder(this)
            .setTitle(pin.name)
            .setItems(
                arrayOf(getString(R.string.rename), getString(R.string.change_color), getString(R.string.delete))
            ) { _, which ->
                when (which) {
                    0 -> promptName(pin.name) { PinStore.update(this, pin.copy(name = it)); pinsChanged() }
                    1 -> pickColor(pin)
                    2 -> AlertDialog.Builder(this)
                        .setMessage(getString(R.string.delete_confirm, pin.name))
                        .setPositiveButton(R.string.delete) { _, _ -> PinStore.delete(this, pin.id); pinsChanged() }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            }
            .show()
    }

    private fun colorItems(): Array<CharSequence> = PinStore.PALETTE.map { (name, color) ->
        SpannableString("●  $name").apply { setSpan(ForegroundColorSpan(color), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    }.toTypedArray()

    private fun pickColor(pin: Pin) {
        AlertDialog.Builder(this)
            .setTitle(R.string.change_color)
            .setItems(colorItems()) { _, i ->
                PinStore.update(this, pin.copy(color = PinStore.PALETTE[i].second))
                pinsChanged()
            }
            .show()
    }

    private fun promptName(initial: String, onOk: (String) -> Unit) {
        val edit = EditText(this).apply { setText(initial); setSelectAllOnFocus(true); setSingleLine() }
        val frame = FrameLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(edit, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.name_pin)
            .setView(frame)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onOk(edit.text.toString().trim().ifEmpty { initial })
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ---------- floating compass & widget ----------

    private fun updateFloatingButton() {
        floatingButton.setText(
            if (FloatingCompassService.running) R.string.floating_stop else R.string.floating_start
        )
    }

    private fun toggleFloating() {
        if (FloatingCompassService.running) {
            startService(Intent(this, FloatingCompassService::class.java).setAction(FloatingCompassService.ACTION_STOP))
            floatingButton.setText(R.string.floating_start)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.floating_permission, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startForegroundService(Intent(this, FloatingCompassService::class.java))
        floatingButton.setText(R.string.floating_stop)
        moveTaskToBack(true) // drop to the previous app so the bubble is visible right away
    }

    private fun requestPinWidget() {
        val mgr = getSystemService(AppWidgetManager::class.java)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, CompassWidgetProvider::class.java), null, null)
        } else {
            Toast.makeText(this, R.string.add_widget_manual, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val REQ_NOTIFICATIONS = 1
        private const val REQ_LOCATION = 2
    }
}
