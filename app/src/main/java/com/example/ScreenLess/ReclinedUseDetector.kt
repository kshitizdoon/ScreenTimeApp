package com.example.ScreenLess

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt


class ReclinedUseDetector(
    context: Context
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager


    private val gravitySensor =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_GRAVITY
        )


    private val accelerometer =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER
        )


    private val lightSensor =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_LIGHT
        )


    private var gx = 0f
    private var gy = 0f
    private var gz = 0f

    private var ambientLight = 100f

    private var motionScore = 0f

    private var lastAx = 0f
    private var lastAy = 0f
    private var lastAz = 0f


    fun start() {

        gravitySensor?.let {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }


        accelerometer?.let {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }


        lightSensor?.let {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }


    fun stop() {

        sensorManager.unregisterListener(
            this
        )
    }


    override fun onSensorChanged(
        event: SensorEvent
    ) {

        when (event.sensor.type) {

            Sensor.TYPE_GRAVITY -> {

                gx = event.values[0]
                gy = event.values[1]
                gz = event.values[2]
            }


            Sensor.TYPE_LIGHT -> {

                ambientLight =
                    event.values[0]
            }


            Sensor.TYPE_ACCELEROMETER -> {

                val ax =
                    event.values[0]

                val ay =
                    event.values[1]

                val az =
                    event.values[2]


                val movement =
                    abs(ax - lastAx) +
                            abs(ay - lastAy) +
                            abs(az - lastAz)


                // Simple smoothing
                motionScore =
                    0.9f * motionScore +
                            0.1f * movement


                lastAx = ax
                lastAy = ay
                lastAz = az
            }
        }
    }


    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // Nothing needed
    }


    fun reclinedScore(): Double {

        /*
         * Estimate physical orientation relative
         * to gravity.
         */

        val pitch =
            Math.toDegrees(
                atan2(
                    -gx.toDouble(),
                    sqrt(
                        gy.toDouble() *
                                gy.toDouble() +
                                gz.toDouble() *
                                gz.toDouble()
                    )
                )
            )


        val roll =
            Math.toDegrees(
                atan2(
                    gy.toDouble(),
                    gz.toDouble()
                )
            )


        /*
         * These are intentionally broad prototype
         * heuristics. We'll calibrate them from
         * measurements on your actual phone.
         */

        val orientationSignal =
            if (
                abs(pitch) > 45 ||
                abs(roll) > 45
            ) {
                1.0
            } else {
                0.0
            }


        val lowMotionSignal =
            if (motionScore < 0.8f) {
                1.0
            } else {
                0.0
            }


        val lowLightSignal =
            if (ambientLight < 30f) {
                1.0
            } else {
                0.0
            }


        return (
                0.60 * orientationSignal +
                        0.30 * lowMotionSignal +
                        0.10 * lowLightSignal
                )
    }


    fun isLikelyReclined(): Boolean {

        return reclinedScore() >= 0.70
    }
}