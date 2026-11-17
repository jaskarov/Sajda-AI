package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.domain.engine.UzbekistanCities
import com.example.ui.viewmodel.LocationDetectionResult
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PermissionsAndLocationTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        runCatching {
            WorkManagerTestInitHelper.initializeTestWorkManager(
                app,
                Configuration.Builder().build()
            )
        }
    }

    @Test
    fun `manifest declares camera and geolocation permissions`() {
        val packageInfo = app.packageManager.getPackageInfo(
            app.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue("Missing CAMERA permission", Manifest.permission.CAMERA in permissions)
        assertTrue(
            "Missing ACCESS_COARSE_LOCATION permission",
            Manifest.permission.ACCESS_COARSE_LOCATION in permissions
        )
        assertTrue(
            "Missing ACCESS_FINE_LOCATION permission",
            Manifest.permission.ACCESS_FINE_LOCATION in permissions
        )
    }

    @Test
    fun `main activity tracks camera and location runtime permission state`() {
        val shadowApp = shadowOf(app)
        shadowApp.denyPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.hasCameraPermission())
                assertFalse(activity.hasLocationPermission())
                assertEquals(3, activity.missingCameraAndLocationPermissions().size)

                shadowApp.grantPermissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                activity.refreshPermissionState()

                assertTrue(activity.hasCameraPermission())
                assertTrue(activity.hasLocationPermission())
                assertTrue(activity.permissionState.value.cameraGranted)
                assertTrue(activity.permissionState.value.locationGranted)
                assertEquals(0, activity.missingCameraAndLocationPermissions().size)
            }
            scenario.moveToState(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun `nearest city detection resolves samarkand from gps last known fix`() = runBlocking {
        val shadowApp = shadowOf(app)
        shadowApp.grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val shadowLm = shadowOf(lm)
        shadowLm.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val samarkandLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 39.6542
            longitude = 66.9597
            time = System.currentTimeMillis()
        }
        shadowLm.simulateLocation(samarkandLocation)

        val viewModel = MainViewModel(app)
        val collectorJob = launch {
            viewModel.uiState.collect {}
        }

        viewModel.detectNearestCityFromLocation()

        var resolved: LocationDetectionResult? = null
        repeat(40) {
            shadowOf(Looper.getMainLooper()).idle()
            resolved = viewModel.uiState.value.locationDetectionResult
            if (resolved != null) return@repeat
            delay(50L)
        }
        collectorJob.cancel()

        assertTrue("Expected Success but got $resolved", resolved is LocationDetectionResult.Success)
        assertEquals("samarqand", (resolved as LocationDetectionResult.Success).city.id)
        assertEquals("toshkent", UzbekistanCities.findNearest(41.30, 69.24).id)
    }
}
