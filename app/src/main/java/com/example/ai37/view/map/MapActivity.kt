package com.example.ai37.view.map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
// Correctly import LocalLifecycleOwner and LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import android.util.Log // For optional debugging

// Correct imports for MapLibre
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.annotations.MarkerOptions

// Import the API Key constant
import com.example.ai37.util.Constants

// This property holds the MapView instance created in Compose, allowing the Activity
// to call onSaveInstanceState/onLowMemory on it for proper lifecycle bridging.
private var mapViewState: MapView? = null

class MapActivity : ComponentActivity() {

    // IMPORTANT: MapLibre initialization MUST happen before setContent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initializes the MapLibre SDK. This is the correct call signature.
        MapLibre.getInstance(applicationContext)

        setContent {
            // Pass the Activity's savedInstanceState bundle to the Composable
            MapViewComposable(savedInstanceState = savedInstanceState)
        }
    }

    // CRITICAL: Override Activity lifecycle methods to pass calls to the MapView instance (mapViewState)
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Delegate state saving to the actual MapView instance
        mapViewState?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Delegate low memory warning to the actual MapView instance
        mapViewState?.onLowMemory()
    }

    // The MapView lifecycle (onStart, onResume, etc.) is handled correctly inside MapViewComposable's DisposableEffect.
}

@Composable
fun MapViewComposable(savedInstanceState: Bundle?) {
    // FIX: Using the constant defined in Constants.kt instead of the placeholder
    val styleUrl = "https://api.baato.io/api/v1/styles/breeze_cdn?key=${Constants.BAATO_API_KEY}"

    // 1. Get the current lifecycle owner for event observation
    val lifecycleOwner = LocalLifecycleOwner.current
    // 2. Get the current context for MapView instantiation (using LocalContext is safer)
    val context = LocalContext.current

    // 3. Remember the MapView instance, creating it using LocalContext.
    val mapView = remember {
        MapView(context).apply {
            // MapView's onCreate needs to be called with the Activity's savedInstanceState
            onCreate(savedInstanceState)
        }
    }

    // 4. Use the remembered MapView in the AndroidView composable
    AndroidView(
        factory = { mapView },
        update = {
            // Update logic here if needed (e.g., if props change)
        }
    )

    // 5. Set up the map style and markers
    mapView.getMapAsync { map ->
        map.setStyle(styleUrl) {
            val kathmanduCenter = LatLng(27.7172, 85.3240)

            val position = CameraPosition.Builder()
                .target(kathmanduCenter)
                .zoom(10.0)
                .build()

            map.cameraPosition = position

            map.addMarker(
                MarkerOptions()
                    .position(kathmanduCenter)
                    .title("Kathmandu")
            )
        }
    }

    // 6. Manage MapView lifecycle integration with Compose via DisposableEffect
    DisposableEffect(lifecycleOwner, mapView) {
        // Set the global reference when the MapView is active in the composition
        mapViewState = mapView

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                // Manually forward lifecycle events to the MapView instance
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        // Clean up when the composable leaves the composition
        onDispose {
            // Clear the global reference
            mapViewState = null
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            // Ensure MapView is properly destroyed when the composable is removed
            mapView.onDestroy()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
    // Note: The preview will likely not render the map correctly without a real device/emulator
    MapViewComposable(savedInstanceState = null)
}