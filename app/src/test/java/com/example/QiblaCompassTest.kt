package com.example

import android.hardware.SensorManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.sensor.QiblaCompassSensor
import com.example.ui.screens.QiblaScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.CompassUiState
import com.example.ui.viewmodel.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QiblaCompassTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun circularAzimuthSmoothing_crossesNorthWrapWithoutSpinback() {
        val cw = QiblaCompassSensor.smoothAzimuthCircular(currentDeg = 355f, targetDeg = 5f, alpha = 0.5f)
        assertEquals(0f, cw, 0.01f)

        val ccw = QiblaCompassSensor.smoothAzimuthCircular(currentDeg = 5f, targetDeg = 355f, alpha = 0.5f)
        assertEquals(0f, ccw, 0.01f)
    }

    @Test
    fun adaptiveAzimuthSmoothing_respondsQuicklyOnLargeTurnsAndDampensOnStill() {
        // Large turn: 0 to 90 deg (delta 90 > 10) -> alpha 0.92f
        val fastTurn = QiblaCompassSensor.smoothAzimuthAdaptive(currentDeg = 0f, targetDeg = 90f)
        assertEquals(90f * 0.92f, fastTurn, 0.1f)

        // Micro movement: 100 to 100.5 deg (delta 0.5 < 1) -> alpha 0.15f
        val still = QiblaCompassSensor.smoothAzimuthAdaptive(currentDeg = 100f, targetDeg = 100.5f)
        assertEquals(100f + 0.5f * 0.15f, still, 0.01f)
    }

    @Test
    fun lowPassFilter_blendsAccelerometerAndMagnetometerVectors() {
        val prev = floatArrayOf(0f, 0f, 9.8f)
        val raw = floatArrayOf(1f, 0f, 9.8f)
        val filtered = QiblaCompassSensor.lowPassFilter(raw, prev, alpha = 0.25f)
        assertEquals(0.25f, filtered[0], 0.001f)
        assertEquals(9.8f, filtered[2], 0.001f)
    }

    @Test
    fun kaabaBearingAndDistance_areAccurateForTashkent() {
        val tashkent = UzbekistanCities.findById("toshkent")
        val bearing = UzbekistanCities.calculateQiblaBearing(tashkent.latitude, tashkent.longitude)
        val distanceKm = UzbekistanCities.calculateDistanceToKaabaKm(tashkent.latitude, tashkent.longitude)

        assertTrue("Expected ~240° Qibla bearing for Tashkent, got $bearing", bearing in 236.0..244.0)
        assertTrue("Expected ~3500-3700 km distance for Tashkent, got $distanceKm", distanceKm in 3450..3700)
    }

    @Test
    fun qiblaScreen_displaysAlignedStatusDialAndTelemetry() {
        val tashkent = UzbekistanCities.findById("toshkent")
        val schedule = GetPrayerTimesUseCase().calculateForToday(
            city = tashkent,
            completedToday = emptySet(),
            alarmMap = emptyMap()
        )
        val qiblaDeg = schedule.qiblaBearingDegrees.toFloat()

        composeRule.setContent {
            SajdaAiTheme {
                QiblaScreen(
                    uiState = MainUiState(schedule = schedule, isReady = true),
                    compassState = CompassUiState(
                        compassAzimuth = qiblaDeg,
                        compassAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
                        hasCompassSensor = true,
                        tiltX = 0.02f,
                        tiltY = -0.01f,
                        tiltDegrees = 2f,
                        isLevel = true
                    ),
                    onDetectNearestCity = { null }
                )
            }
        }

        composeRule.onNodeWithText("Qibla yo‘nalishi topildi!").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla_compass_dial").assertIsDisplayed()
        composeRule.onNodeWithTag("compass_azimuth_value").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("qibla_kaaba_distance").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("qibla_sensor_status").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Datchik holati: A‘lo").assertIsDisplayed()
        composeRule.onNodeWithText("Tekis ✓").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Magnetometr", substring = true).assertDoesNotExist()
    }

    @Test
    fun qiblaScreen_showsCalibrationPromptWhenMagnetometerAccuracyLow() {
        val tashkent = UzbekistanCities.findById("toshkent")
        val schedule = GetPrayerTimesUseCase().calculateForToday(
            city = tashkent,
            completedToday = emptySet(),
            alarmMap = emptyMap()
        )

        composeRule.setContent {
            SajdaAiTheme {
                QiblaScreen(
                    uiState = MainUiState(schedule = schedule, isReady = true),
                    compassState = CompassUiState(
                        compassAzimuth = 120f,
                        compassAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_LOW,
                        hasCompassSensor = true,
                        tiltDegrees = 18f,
                        isLevel = false
                    ),
                    onDetectNearestCity = { null }
                )
            }
        }

        val expectedTurnDeg = kotlin.math.abs(
            ((schedule.qiblaBearingDegrees.toFloat() - 120f + 540f) % 360f) - 180f
        ).let { kotlin.math.round(it).toInt() }
        composeRule.onNodeWithText("O‘ngga ${expectedTurnDeg}° buriling").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla_level_hint_banner").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Kompas aniqligi past — telefonni havoda 8 shaklida aylantiring."
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Biroz tekislang").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("egilgan", substring = true).assertDoesNotExist()
    }

    @Test
    fun computeAzimuthFromRotationMatrix_preservesTrueHeadingUnderHandTiltWithout45DegJump() {
        // Pure heading of 109° clockwise from North when phone is flat (yaw = +109° in world matrix)
        //Wait: Android SensorManager.getOrientation defines azimuth = atan2(R[1], R[4]),
        // so R = RotZ(-theta) gives R[1] = sin(theta), R[4] = cos(theta) => azimuth = theta.
        val headingRad = Math.toRadians(109.0)
        val cH = kotlin.math.cos(headingRad).toFloat()
        val sH = kotlin.math.sin(headingRad).toFloat()
        val flatMatrix = floatArrayOf(
            cH, sH, 0f,
            -sH, cH, 0f,
            0f, 0f, 1f
        )
        val flatAzimuth = QiblaCompassSensor.computeAzimuthFromRotationMatrix(flatMatrix)
        assertEquals(109f, flatAzimuth, 0.5f)

        // Pitch phone up by 70° (R[8] = cos(70°) ≈ 0.34 < 0.45, previously triggered 45°+ jump!)
        // R = RotZ(-109°) * RotX(+70°) preserves yaw = 109° in R[1]=sin(109), R[4]=cos(109)*cos(70)
        val tiltRad = Math.toRadians(70.0)
        val cT = kotlin.math.cos(tiltRad).toFloat()
        val sT = kotlin.math.sin(tiltRad).toFloat()
        val tiltedMatrix = floatArrayOf(
            cH, sH * cT, sH * sT,
            -sH, cH * cT, cH * sT,
            0f, -sT, cT
        )
        val tiltedAzimuth = QiblaCompassSensor.computeAzimuthFromRotationMatrix(tiltedMatrix)
        assertEquals(109f, tiltedAzimuth, 1.5f)
    }
}
