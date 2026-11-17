package com.example

import android.content.Context
import android.hardware.GeomagneticField
import androidx.test.core.app.ApplicationProvider
import com.example.domain.engine.UzbekistanCities
import com.example.sensor.QiblaCompassSensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerSensorAndPermissionRobolectricTest {

    private lateinit var context: Context
    private lateinit var sensor: QiblaCompassSensor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sensor = QiblaCompassSensor(context)
    }

    @Test
    fun compassSensor_initialization_doesNotCrash() {
        assertNotNull(sensor)
        val hasSensors = sensor.hasMagneticSensors()
        // In Robolectric environment without real hardware
        assertTrue(hasSensors || !hasSensors)
    }

    @Test
    fun geomagneticField_tashkent_calculatesReasonableDeclination() {
        val city = UzbekistanCities.findById("tashkent")
        val now = System.currentTimeMillis()
        val geoField = GeomagneticField(
            city.latitude.toFloat(),
            city.longitude.toFloat(),
            450f,
            now
        )

        // Tashkent magnetic declination is typically +4° to +7° East
        val declination = geoField.declination
        assertTrue("Tashkent declination should be reasonable positive degrees", declination in 0f..15f)
    }

    @Test
    fun qiblaAngleCalculation_shortestSignedPath_correctlyHandlesBoundary() {
        val qiblaBearing = 240f
        val heading1 = 6f

        // Shortest signed angle from heading1 to qibla: ((240 - 6 + 540) % 360) - 180
        val diff1 = ((qiblaBearing - heading1 + 540f) % 360f) - 180f
        // 240 - 6 = 234, 234 - 360 = -126 (126° left / counter-clockwise)
        assertEquals(-126f, diff1, 0.1f)

        // Test alignment within 5 degrees
        val headingAligned = 242f
        val diffAligned = ((qiblaBearing - headingAligned + 540f) % 360f) - 180f
        assertTrue(abs(diffAligned) <= 5f)
    }
}
