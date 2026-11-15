package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.audio.configureAppCheck
import com.example.ui.navigation.SajdaNavGraph
import com.example.ui.theme.SajdaAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before rendering UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            SajdaAiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SajdaNavGraph()
                }
            }
        }
    }
}
