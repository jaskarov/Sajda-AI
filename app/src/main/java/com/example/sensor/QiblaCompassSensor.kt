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
import kotlin.math.acos
import kotlin.math.sqrt

data class CompassReading(
    val magneticAzimuthDegrees: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasSensor: Boolean = true,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val tiltDegrees: Float = 0f,
    val isLevel: Boolean = true
)

class QiblaCompassSensor(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    fun readingsFlow(): Flow<CompassReading> = callbackFlow {
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val hasRotationVector = rotationSensor != null
        val hasAccelAndMagnet = accelSensor != null && magnetSensor != null
        val hasSensor = hasMagneticSensors()

        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null
        var smoothedAzimuth = 0f
        var initialized = false
        var tiltX = 0f
        var tiltY = 0f
        var tiltDegrees = 0f
        var isLevel = true
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
                    Sensor.TYPE_ACCELEROMETER -> {
                        val filtered = lowPassFilter(event.values, gravity, ALPHA_ACCEL_MAGNET)
                        gravity = filtered
                        val mag = sqrt(
                            filtered[0] * filtered[0] +
                                filtered[1] * filtered[1] +
                                filtered[2] * filtered[2]
                        )
                        if (mag > 0.5f) {
                            tiltX = (-filtered[0] / mag).coerceIn(-1f, 1f)
                            tiltY = (filtered[1] / mag).coerceIn(-1f, 1f)
                            val cosTilt = (filtered[2] / mag).coerceIn(-1f, 1f)
                            tiltDegrees = Math.toDegrees(acos(cosTilt.toDouble())).toFloat()
                            isLevel = tiltDegrees <= LEVEL_THRESHOLD_DEGREES
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic = lowPassFilter(event.values, geomagnetic, ALPHA_ACCEL_MAGNET)
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        matrixReady = true
                    }
                }

                // Only compute fallback rotation matrix from accel+magnet when TYPE_ROTATION_VECTOR
                // is unavailable so fused sensor heading is never overwritten by raw uncalibrated readings.
                if (!hasRotationVector) {
                    val g = gravity
                    val m = geomagnetic
                    if (g != null && m != null) {
                        matrixReady = SensorManager.getRotationMatrix(
                            rotationMatrix,
                            null,
                            g,
                            m
                        )
                    }
                }

                if (!matrixReady) {
                    if (initialized) {
                        trySend(
                            CompassReading(
                                magneticAzimuthDegrees = smoothedAzimuth,
                                accuracy = currentAccuracy,
                                hasSensor = hasSensor,
                                tiltX = tiltX,
                                tiltY = tiltY,
                                tiltDegrees = tiltDegrees,
                                isLevel = isLevel
                            )
                        )
                    }
                    return
                }

                val rawAzimuth = computeAzimuthFromRotationMatrix(
                    rotationMatrix = rotationMatrix,
                    displayRotation = currentDisplayRotation(),
                    remappedMatrix = remappedMatrix,
                    orientation = orientation
                )

                smoothedAzimuth = if (!initialized) {
                    initialized = true
                    rawAzimuth
                } else {
                    smoothAzimuthCircular(smoothedAzimuth, rawAzimuth, ALPHA_AZIMUTH_SMOOTHING)
                }

                trySend(
                    CompassReading(
                        magneticAzimuthDegrees = smoothedAzimuth,
                        accuracy = currentAccuracy,
                        hasSensor = hasSensor,
                        tiltX = tiltX,
                        tiltY = tiltY,
                        tiltDegrees = tiltDegrees,
                        isLevel = isLevel
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
                            hasSensor = hasSensor,
                            tiltX = tiltX,
                            tiltY = tiltY,
                            tiltDegrees = tiltDegrees,
                            isLevel = isLevel
                        )
                    )
                }
            }
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            if (accelSensor != null) {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetSensor != null) {
                sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_UI)
            }
        } else if (hasAccelAndMagnet) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_UI)
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
        return (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) ||
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }

    companion object {
        private const val ALPHA_ACCEL_MAGNET = 0.22f
        private const val ALPHA_AZIMUTH_SMOOTHING = 0.22f
        const val LEVEL_THRESHOLD_DEGREES = 12f

        /**
         * Computes azimuth in [0, 360) directly from the 3x3 world rotation matrix using
         * SensorManager.getOrientation without artificial upright AXIS_X/AXIS_Z axis swapping.
         */
        fun computeAzimuthFromRotationMatrix(
            rotationMatrix: FloatArray,
            displayRotation: Int = Surface.ROTATION_0,
            remappedMatrix: FloatArray = FloatArray(9),
            orientation: FloatArray = FloatArray(3)
        ): Float {
            val activeMatrix = when (displayRotation) {
                Surface.ROTATION_90 -> {
                    if (SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_Y,
                            SensorManager.AXIS_MINUS_X,
                            remappedMatrix
                        )
                    ) remappedMatrix else rotationMatrix
                }
                Surface.ROTATION_180 -> {
                    if (SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_MINUS_X,
                            SensorManager.AXIS_MINUS_Y,
                            remappedMatrix
                        )
                    ) remappedMatrix else rotationMatrix
                }
                Surface.ROTATION_270 -> {
                    if (SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_MINUS_Y,
                            SensorManager.AXIS_X,
                            remappedMatrix
                        )
                    ) remappedMatrix else rotationMatrix
                }
                else -> rotationMatrix
            }

            SensorManager.getOrientation(activeMatrix, orientation)
            return ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
        }

        fun lowPassFilter(
            input: FloatArray,
            previous: FloatArray?,
            alpha: Float = ALPHA_ACCEL_MAGNET
        ): FloatArray {
            if (previous == null) return input.clone()
            val size = minOf(input.size, previous.size)
            return FloatArray(size) { i ->
                previous[i] * (1f - alpha) + input[i] * alpha
            }
        }

        fun smoothAzimuthCircular(
            currentDeg: Float,
            targetDeg: Float,
            alpha: Float = ALPHA_AZIMUTH_SMOOTHING
        ): Float {
            var delta = targetDeg - currentDeg
            while (delta > 180f) delta -= 360f
            while (delta < -180f) delta += 360f
            return ((currentDeg + alpha * delta) % 360f + 360f) % 360f
        }
    }
}
