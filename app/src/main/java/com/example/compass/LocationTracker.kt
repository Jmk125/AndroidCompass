package com.example.compass

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock

/** Plain LocationManager tracker (GPS + network); no Play Services needed. */
class LocationTracker(private val context: Context, private val onFix: (Location) -> Unit) : LocationListener {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var last: Location? = null
        private set

    /** The latest fix, only if it's recent enough to pin from. */
    fun freshLocation(): Location? = last?.takeIf {
        SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos < 30_000_000_000L
    }

    @SuppressLint("MissingPermission")
    fun start() {
        stop()
        if (!Permissions.hasFine(context)) return
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            runCatching {
                if (manager.isProviderEnabled(provider)) {
                    manager.requestLocationUpdates(provider, 1000L, 0f, this, Looper.getMainLooper())
                }
            }
        }
    }

    fun stop() {
        runCatching { manager.removeUpdates(this) }
    }

    override fun onLocationChanged(location: Location) {
        val cur = last
        // Take it if it's from the same source, more accurate, or the current fix has gone stale.
        if (cur == null || location.provider == cur.provider || location.accuracy <= cur.accuracy ||
            location.time - cur.time > 10_000
        ) {
            last = location
            onFix(location)
        }
    }

    // Implemented explicitly: on Android 8–10 the system still calls these.
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
