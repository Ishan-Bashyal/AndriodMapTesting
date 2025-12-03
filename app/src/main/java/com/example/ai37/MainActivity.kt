package com.example.ai37

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ai37.view.map.MapViewComposable // Assuming this is the correct import path

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Passing the required 'savedInstanceState' argument to MapViewComposable
            MapViewComposable(savedInstanceState = savedInstanceState)
        }
    }
}