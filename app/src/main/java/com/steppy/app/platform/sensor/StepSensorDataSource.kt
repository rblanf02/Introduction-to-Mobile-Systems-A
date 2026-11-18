package com.steppy.app.platform.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Reads the hardware step counter (TYPE_STEP_COUNTER).
 *
 * The sensor returns the total steps since the phone was last rebooted.
 */

class StepSensorDataSource(context: Context) : StepCounterSource{

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepCounter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override fun isAvailable(): Boolean = stepCounter != null

    override fun stepSinceReboot(): Flow<Int> {

        val sensor = stepCounter ?: return  emptyFlow()

        return callbackFlow {
            val listener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent) {
                // values[0]: Number of steps taken by the user since the last reboot while the sensor is activated
                trySend(event.values[0].toInt())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

}