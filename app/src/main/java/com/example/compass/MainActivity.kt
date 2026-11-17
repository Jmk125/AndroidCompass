package com.example.compass

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
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

    private lateinit var t: UiTheme
    private lateinit var appliedStyle: CompassStyle
    private lateinit var compassView: CompassView
    private lateinit var radarView: RadarView
    private lateinit var compassTab: Button
    private lateinit var radarTab: Button
    private lateinit var sensor: HeadingSensor
    private lateinit var navigator: PinNavigator
    private lateinit var floatingButton: Button
    private lateinit var statusText: TextView
    private lateinit var pinsContainer: LinearLayout

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        appliedStyle = StylePrefs.get(this)
        t = UiTheme.of(this, appliedStyle)
        t.applyWindow(this)

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

        statusText = TextView(this).apply { t.styleBody(this, 14f, muted = true); gravity = Gravity.CENTER }
        val pinButton = t.newButton(this, getString(R.string.pin_here)) { pinHere() }
        pinsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        floatingButton = t.newButton(this, "") { toggleFloating() }
        val settingsButton = t.newButton(this, getString(R.string.settings)) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        compassTab = t.newButton(this, getString(R.string.tab_compass)) { showView(radar = false) }
        radarTab = t.newButton(this, getString(R.string.tab_radar)) { showView(radar = true) }

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), dp(6))
            addView(compassTab, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = dp(6) })
            addView(radarTab, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(6) })
        }
        val viewFrame = FrameLayout(this).apply {
            addView(compassView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(radarView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(8))
            addView(statusText, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(6) })
            addView(pinButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(pinsContainer, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(6), dp(16), 0)
            addView(floatingButton, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = dp(6) })
            addView(settingsButton, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(6) })
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(t.bg)
            addView(tabs, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(viewFrame, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(ScrollView(this@MainActivity).apply { addView(content) }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(bottomRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        applySystemInsets(root, extraTop = dp(8), extraBottom = dp(12), extraSide = 0)
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
        if (StylePrefs.get(this) != appliedStyle) {
            recreate() // style was changed in Settings: rebuild the screen in the new look
            return
        }
        sensor.start()
        navigator.start()
        updateFloatingButton()
        updateStatus()
        refreshPins()
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
        t.styleButton(this, radarTab, selected = radar)
        t.styleButton(this, compassTab, selected = !radar)
        getSharedPreferences("ui", MODE_PRIVATE).edit().putBoolean("radar", radar).apply()
    }

    // ---------- pins ----------

    private fun updateStatus() {
        val loc = navigator.location
        statusText.text = when {
            !sensor.isAvailable -> getString(R.string.no_sensor)
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
            ThemedDialog(this, t).show(
                title = getString(R.string.weak_title),
                message = getString(R.string.weak_message, loc.accuracy.roundToInt()),
                positive = getString(R.string.pin_anyway),
                onPositive = { savePin() },
                negative = getString(android.R.string.cancel),
            )
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
                setText(R.string.no_pins); t.styleBody(this, 15f, muted = true)
                setPadding(0, dp(10), 0, 0)
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
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(pin.color); setStroke(dp(1), t.border)
                }
            }
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@MainActivity).apply { text = pin.name; t.styleTitle(this, 18f) })
                addView(TextView(this@MainActivity).apply {
                    text = subtitle; t.styleBody(this, 14f, muted = true)
                    if (status?.arrived == true) setTextColor(t.accent)
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
        ThemedDialog(this, t).show(
            title = pin.name,
            items = listOf(getString(R.string.rename), getString(R.string.change_color), getString(R.string.delete)),
            onItem = { which ->
                when (which) {
                    0 -> promptName(pin.name) { PinStore.update(this, pin.copy(name = it)); pinsChanged() }
                    1 -> pickColor(pin)
                    2 -> ThemedDialog(this, t).show(
                        message = getString(R.string.delete_confirm, pin.name),
                        positive = getString(R.string.delete),
                        onPositive = { PinStore.delete(this, pin.id); pinsChanged() },
                        negative = getString(android.R.string.cancel),
                    )
                }
            },
        )
    }

    private fun pickColor(pin: Pin) {
        val items: List<CharSequence> = PinStore.PALETTE.map { (name, color) ->
            SpannableString("●  $name").apply { setSpan(ForegroundColorSpan(color), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        }
        ThemedDialog(this, t).show(
            title = getString(R.string.change_color),
            items = items,
            onItem = { i ->
                PinStore.update(this, pin.copy(color = PinStore.PALETTE[i].second))
                pinsChanged()
            },
        )
    }

    private fun promptName(initial: String, onOk: (String) -> Unit) {
        val edit = EditText(this).apply {
            setText(initial); setSelectAllOnFocus(true); setSingleLine()
            typeface = t.body; setTextColor(t.text); textSize = 18f
            backgroundTintList = ColorStateList.valueOf(t.accent)
        }
        ThemedDialog(this, t).show(
            title = getString(R.string.name_pin),
            body = edit,
            positive = getString(android.R.string.ok),
            onPositive = { onOk(edit.text.toString().trim().ifEmpty { initial }) },
            negative = getString(android.R.string.cancel),
        )
    }

    // ---------- floating compass ----------

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

    companion object {
        private const val REQ_NOTIFICATIONS = 1
        private const val REQ_LOCATION = 2
    }
}
