package com.example.compass

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock

/**
 * Plain LocationManager tracker; no Play Services needed.
 * GPS is preferred. Wi-Fi/cell ("network") fixes are only a fallback because they can be
 * tens or hundreds of metres off and barely move as you walk.
 */
class LocationTracker(private val context: Context, private val onFix: () -> Unit) : LocationListener {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var gps: Location? = null
    private var network: Location? = null

    private fun ageMs(l: Location) = (SystemClock.elapsedRealtimeNanos() - l.elapsedRealtimeNanos) / 1_000_000L

    /** A recent GPS fix, else a recent network fix that isn't hopeless, else null. */
    val last: Location?
        get() {
            gps?.let { if (ageMs(it) < 15_000L) return it }
            network?.let { if (ageMs(it) < 30_000L && it.accuracy <= 100f) return it }
            return null
        }

    val isGps: Boolean get() = last?.provider == LocationManager.GPS_PROVIDER

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
        when (location.provider) {
            LocationManager.GPS_PROVIDER -> gps = location
            LocationManager.NETWORK_PROVIDER -> network = location
            else -> return
        }
        onFix()
    }

    // Implemented explicitly: on Android 8-10 the system still calls these.
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
