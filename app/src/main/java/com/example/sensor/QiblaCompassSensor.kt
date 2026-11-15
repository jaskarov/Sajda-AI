package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

data class CompassReading(
    val magneticAzimuthDegrees: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasSensor: Boolean = true
)

class QiblaCompassSensor(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    fun readingsFlow(): Flow<CompassReading> = callbackFlow {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val hasSensor = hasMagneticSensors()

        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null
        var smoothedAzimuth = 0f
        var initialized = false
        var currentAccuracy = if (hasSensor) {
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
        } else {
            SensorManager.SENSOR_STATUS_UNRELIABLE
        }

        // Emit initial baseline immediately so upstream flows never stall
        trySend(
            CompassReading(
                magneticAzimuthDegrees = 0f,
                accuracy = currentAccuracy,
                hasSensor = hasSensor
            )
        )

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                var matrixReady = false

                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        matrixReady = true
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val prev = gravity
                        gravity = if (prev == null) {
                            event.values.clone()
                        } else {
                            FloatArray(3) { i -> prev[i] * 0.8f + event.values[i] * 0.2f }
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val prev = geomagnetic
                        geomagnetic = if (prev == null) {
                            event.values.clone()
                        } else {
                            FloatArray(3) { i -> prev[i] * 0.8f + event.values[i] * 0.2f }
                        }
                    }
                }

                if (!matrixReady && gravity != null && geomagnetic != null) {
                    matrixReady = SensorManager.getRotationMatrix(
                        rotationMatrix,
                        null,
                        gravity,
                        geomagnetic
                    )
                }

                if (!matrixReady) return

                // If the device is held upright, remap Z -> Y so heading remains stable
                val activeMatrix = if (abs(rotationMatrix[8]) < 0.45f) {
                    val remapped = SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedMatrix
                    )
                    if (remapped) remappedMatrix else rotationMatrix
                } else {
                    rotationMatrix
                }

                SensorManager.getOrientation(activeMatrix, orientation)
                var rawAzimuth = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()

                // Compensate for display rotation (landscape / reverse portrait)
                val displayOffset = when (currentDisplayRotation()) {
                    Surface.ROTATION_90 -> 90f
                    Surface.ROTATION_180 -> 180f
                    Surface.ROTATION_270 -> 270f
                    else -> 0f
                }
                rawAzimuth = (rawAzimuth + displayOffset) % 360f

                if (!initialized) {
                    smoothedAzimuth = rawAzimuth
                    initialized = true
                } else {
                    var delta = rawAzimuth - smoothedAzimuth
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    smoothedAzimuth = ((smoothedAzimuth + 0.2f * delta) % 360f + 360f) % 360f
                }

                trySend(
                    CompassReading(
                        magneticAzimuthDegrees = smoothedAzimuth,
                        accuracy = currentAccuracy,
                        hasSensor = hasSensor
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD ||
                    sensor?.type == Sensor.TYPE_ROTATION_VECTOR
                ) {
                    currentAccuracy = accuracy
                    trySend(
                        CompassReading(
                            magneticAzimuthDegrees = smoothedAzimuth,
                            accuracy = currentAccuracy,
                            hasSensor = hasSensor
                        )
                    )
                }
            }
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelSensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
            magnetSensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(): Int {
        return runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.display?.rotation ?: Surface.ROTATION_0
            } else {
                windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
            }
        }.getOrDefault(Surface.ROTATION_0)
    }

    fun hasMagneticSensors(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
            (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
    }
}
