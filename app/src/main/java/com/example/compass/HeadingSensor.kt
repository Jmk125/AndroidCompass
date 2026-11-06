package com.example.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Delivers a smoothed compass heading (degrees clockwise from magnetic north, 0..360)
 * using the fused rotation-vector sensor. Works with the phone flat or held upright.
 */
class HeadingSensor(context: Context, private val onHeading: (Float) -> Unit) : SensorEventListener {

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotation = FloatArray(9)
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)

    // Smooth on the unit circle so the 359° -> 1° wrap doesn't glitch.
    private var sinAvg = 0f
    private var cosAvg = 0f
    private var primed = false

    val isAvailable: Boolean get() = sensor != null

    fun start() {
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        primed = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotation, event.values)

        // Phone held upright (camera pointing at the horizon): use the back of the phone as "forward".
        val upright = abs(rotation[7]) > 0.7f // phone's long axis points mostly up/down
        val matrix = if (upright) {
            SensorManager.remapCoordinateSystem(
                rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped
            )
            remapped
        } else {
            rotation
        }
        SensorManager.getOrientation(matrix, orientation)
        val azimuth = orientation[0] // radians, -π..π

        val alpha = 0.15f
        if (!primed) {
            sinAvg = sin(azimuth); cosAvg = cos(azimuth); primed = true
        } else {
            sinAvg += alpha * (sin(azimuth) - sinAvg)
            cosAvg += alpha * (cos(azimuth) - cosAvg)
        }
        var deg = Math.toDegrees(atan2(sinAvg, cosAvg).toDouble()).toFloat()
        if (deg < 0) deg += 360f
        onHeading(deg)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
