package com.finnvek.cornersapart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.finnvek.cornersapart.ui.screens.GameRoute
import com.finnvek.cornersapart.ui.theme.CornersApartTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CornersApartTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameRoute()
                }
            }
        }
    }
}
