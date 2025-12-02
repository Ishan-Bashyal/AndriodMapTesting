package com.example.ai37

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ai37.view.map.MapViewComposable // Assuming this is the correct import path

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // FIX: Passing the required 'savedInstanceState' argument to MapViewComposable
            MapViewComposable(savedInstanceState = savedInstanceState)
        }
    }
}