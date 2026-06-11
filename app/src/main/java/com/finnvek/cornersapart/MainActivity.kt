package com.finnvek.cornersapart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.finnvek.cornersapart.multiplayer.NearbyPermissions
import com.finnvek.cornersapart.ui.screens.GameRoute
import com.finnvek.cornersapart.ui.theme.CornersApartTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val nearbyPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CornersApartTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameRoute(onRequestNearbyPermissions = ::requestNearbyPermissions)
                }
            }
        }
    }

    private fun requestNearbyPermissions() {
        val permissions = NearbyPermissions.requiredRuntimePermissions()
        if (permissions.isNotEmpty()) {
            nearbyPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
