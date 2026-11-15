package com.example.compass

import android.content.Context
import android.hardware.GeomagneticField
import android.location.Location
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Handler
import android.os.Looper

/** Where a pin arrow sits on the (magnetic-north-up) dial. */
data class PinMarker(val angle: Float, val color: Int, val arrived: Boolean)

/** Combines saved pins with the current location into bearings/distances, and detects arrival. */
class PinNavigator(private val context: Context, private val onChange: () -> Unit) {

    class Status(
        val pin: Pin,
        val distance: Float,
        val trueBearing: Float,
        val magneticBearing: Float,
        val arrived: Boolean,
    )

    private val tracker = LocationTracker(context) { recompute(location) }
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            recompute(location) // notice when fixes go stale even if no new ones arrive
            handler.postDelayed(this, 5000)
        }
    }
    private val arrivedIds = mutableSetOf<Long>()

    // Pins we've seen from outside the arrival radius. A pin created where you stand
    // starts "arrived" but silent; only a genuine arrival after being away buzzes.
    private val seenAway = mutableSetOf<Long>()

    var statuses: List<Status> = emptyList()
        private set

    val location: Location? get() = tracker.last
    val isGps: Boolean get() = tracker.isGps
    fun freshLocation(): Location? = tracker.last

    val markers: List<PinMarker>
        get() = statuses.map { PinMarker(it.magneticBearing, it.pin.color, it.arrived) }

    fun start() {
        tracker.start()
        refresh()
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, 5000)
    }

    fun stop() {
        handler.removeCallbacks(tick)
        tracker.stop()
    }

    /** Re-evaluate after pins were added/edited/removed. */
    fun refresh() = recompute(location)

    private fun recompute(loc: Location?) {
        if (loc == null) {
            statuses = emptyList()
            onChange()
            return
        }
        val declination = GeomagneticField(
            loc.latitude.toFloat(), loc.longitude.toFloat(), loc.altitude.toFloat(), System.currentTimeMillis()
        ).declination
        val result = FloatArray(3)
        val pins = PinStore.load(context)
        val liveIds = pins.map { it.id }.toSet()
        arrivedIds.retainAll(liveIds)
        seenAway.retainAll(liveIds)

        statuses = pins.map { pin ->
            Location.distanceBetween(loc.latitude, loc.longitude, pin.lat, pin.lon, result)
            val distance = result[0]
            val trueBearing = (result[1] + 360f) % 360f
            // The dial is magnetic-north-up, so convert the true bearing to a magnetic one.
            val magnetic = ((trueBearing - declination) % 360f + 360f) % 360f
            val wasArrived = pin.id in arrivedIds
            // Fixed, predictable radii; a poor fix never counts as "arrived" (and can't hold it either).
            val arrived = if (wasArrived) loc.accuracy <= 40f && distance <= 25f
            else loc.accuracy <= 20f && distance <= 15f
            if (arrived && !wasArrived) {
                arrivedIds += pin.id
                if (pin.id in seenAway) buzz()
            } else if (!arrived && wasArrived) {
                arrivedIds -= pin.id
            }
            if (!arrived) seenAway += pin.id
            Status(pin, distance, trueBearing, magnetic, arrived)
        }
        onChange()
    }

    private fun buzz() {
        runCatching {
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                .vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
