package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.SajdaNavGraph
import com.example.ui.theme.SajdaAiTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RuntimePermissionsState(
    val cameraGranted: Boolean = false,
    val fineLocationGranted: Boolean = false,
    val coarseLocationGranted: Boolean = false
) {
    val locationGranted: Boolean
        get() = fineLocationGranted || coarseLocationGranted
}

val LocalRuntimePermissions = compositionLocalOf { RuntimePermissionsState() }

class MainActivity : ComponentActivity() {

    private val _permissionState = MutableStateFlow(RuntimePermissionsState())
    val permissionState: StateFlow<RuntimePermissionsState> = _permissionState.asStateFlow()

    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshPermissionState()
        requestStartupCameraAndLocationPermissionsIfNeeded()

        enableEdgeToEdge()
        setContent {
            val currentPermissions by permissionState.collectAsStateWithLifecycle()
            SajdaAiTheme {
                CompositionLocalProvider(LocalRuntimePermissions provides currentPermissions) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        SajdaNavGraph()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun refreshPermissionState() {
        _permissionState.value = RuntimePermissionsState(
            cameraGranted = hasCameraPermission(),
            fineLocationGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED,
            coarseLocationGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun missingCameraAndLocationPermissions(): Array<String> {
        return buildList {
            if (!hasCameraPermission()) {
                add(Manifest.permission.CAMERA)
            }
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }.toTypedArray()
    }

    fun requestCameraAndLocationPermissions() {
        val missing = missingCameraAndLocationPermissions()
        if (missing.isNotEmpty()) {
            runtimePermissionsLauncher.launch(missing)
        } else {
            refreshPermissionState()
        }
    }

    private fun requestStartupCameraAndLocationPermissionsIfNeeded() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyPrompted = prefs.getBoolean(KEY_STARTUP_PERMS_PROMPTED, false)
        val missing = missingCameraAndLocationPermissions()
        if (!alreadyPrompted && missing.isNotEmpty()) {
            prefs.edit().putBoolean(KEY_STARTUP_PERMS_PROMPTED, true).apply()
            runtimePermissionsLauncher.launch(missing)
        }
    }

    companion object {
        private const val PREFS_NAME = "sajda_runtime_permissions"
        private const val KEY_STARTUP_PERMS_PROMPTED = "startup_camera_location_prompted"

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
