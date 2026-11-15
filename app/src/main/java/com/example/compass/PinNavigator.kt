package com.example.compass

import android.content.Context
import android.hardware.GeomagneticField
import android.location.Location
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.max
import kotlin.math.min

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

    private val tracker = LocationTracker(context) { recompute(it) }
    private val arrivedIds = mutableSetOf<Long>()

    // Pins we've seen from outside the arrival radius. A pin created where you stand
    // starts "arrived" but silent; only a genuine arrival after being away buzzes.
    private val seenAway = mutableSetOf<Long>()

    var statuses: List<Status> = emptyList()
        private set

    val location: Location? get() = tracker.last
    fun freshLocation(): Location? = tracker.freshLocation()

    val markers: List<PinMarker>
        get() = statuses.map { PinMarker(it.magneticBearing, it.pin.color, it.arrived) }

    fun start() {
        tracker.start()
        refresh()
    }

    fun stop() = tracker.stop()

    /** Re-evaluate after pins were added/edited/removed. */
    fun refresh() = recompute(tracker.last)

    private fun recompute(loc: Location?) {
        if (loc == null) {
            statuses = emptyList()
            onChange()
            return
        }
        val declination = GeomagneticField(
            loc.latitude.toFloat(), loc.longitude.toFloat(), loc.altitude.toFloat(), System.currentTimeMillis()
        ).declination
        // Poor fixes get a bit more slack, capped so "arrived" can't mean 100 m away.
        val arriveRadius = max(15f, min(loc.accuracy, 30f))
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
            val arrived = distance <= if (wasArrived) arriveRadius + 10f else arriveRadius
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
