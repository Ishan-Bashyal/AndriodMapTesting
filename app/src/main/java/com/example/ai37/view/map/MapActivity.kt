package com.example.ai37.view.map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ai37.util.Constants
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
// FIX: Changed MapboxMap to MapLibreMap for correct class reference
import org.maplibre.android.maps.MapLibreMap

// FIX: Add necessary imports for KeyboardOptions and KeyboardType
import androidx.compose.ui.text.input.KeyboardType

// Global reference for MapView lifecycle integration
private var mapViewState: MapView? = null

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapLibre.getInstance(applicationContext)

        setContent {
            MapViewComposable(savedInstanceState = savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapViewState?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapViewState?.onLowMemory()
    }
}

@Composable
fun MapViewComposable(savedInstanceState: Bundle?) {
    // Initial Kathmandu coordinates
    val initialLat = 27.7172
    val initialLon = 85.3240
    val context = LocalContext.current

    // State for the currently selected/displayed coordinates (Requirement 2 output)
    var currentLat by remember { mutableStateOf(initialLat) }
    var currentLon by remember { mutableStateOf(initialLon) }

    // State for user input fields (Requirement 1 input)
    var inputLatText by remember { mutableStateOf(initialLat.toString()) }
    var inputLonText by remember { mutableStateOf(initialLon.toString()) }

    // Map style URL using the API key constant
    val styleUrl = remember {
        "https://api.baato.io/api/v1/styles/breeze_cdn?key=${Constants.BAATO_API_KEY}"
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(savedInstanceState)
        }
    }

    // FIX: Changed MapboxMap to MapLibreMap
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var markerInstance by remember { mutableStateOf<Marker?>(null) }


    // Map setup and listeners (runs once after map is ready)
    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            mapInstance = map
            map.setStyle(styleUrl) {

                // Initial position
                val initialPosition = LatLng(initialLat, initialLon)

                // Center map
                map.cameraPosition = CameraPosition.Builder()
                    .target(initialPosition)
                    .zoom(12.0)
                    .build()

                // Add marker once
                val markerOptions = MarkerOptions()
                    .position(initialPosition)
                    .title("Selected Location")

                markerInstance = map.addMarker(markerOptions)

                // --- USER TAPS TO SELECT LOCATION ---
                map.addOnMapClickListener { point ->
                    // Move marker
                    markerInstance?.position = point

                    // Update state
                    currentLat = point.latitude
                    currentLon = point.longitude
                    inputLatText = point.latitude.toString()
                    inputLonText = point.longitude.toString()

                    true
                }
            }

        }
        onDispose { /* Cleanup not strictly needed here */ }
    }

    // Lifecycle observer for MapView
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        mapViewState = mapView
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            mapViewState = null
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.onDestroy()
        }
    }

    Scaffold { paddingValues -> // TopAppBar has been removed as requested.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- UI Controls for Location Input (Requirement 1) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Choose Location:", style = MaterialTheme.typography.titleMedium)
                    Text("• Type latitude/longitude and press the button", style = MaterialTheme.typography.bodySmall)
                    Text("• OR tap anywhere on the map", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputLatText,
                            onValueChange = { inputLatText = it },
                            label = { Text("Latitude") },
                            // FIX: Changed KeyboardType.NumberDecimal to the correct KeyboardType.Decimal
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputLonText,
                            onValueChange = { inputLonText = it },
                            label = { Text("Longitude") },
                            // FIX: Changed KeyboardType.NumberDecimal to the correct KeyboardType.Decimal
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val lat = inputLatText.toDouble()
                                    val lon = inputLonText.toDouble()
                                    val newPosition = LatLng(lat, lon)

                                    mapInstance?.animateCamera(
                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(newPosition, 12.0)
                                    )
                                    markerInstance?.position = newPosition
                                    currentLat = lat
                                    currentLon = lon
                                } catch (e: NumberFormatException) {
                                    Toast.makeText(context, "Invalid coordinates. Please enter numbers.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Center Map")
                        }

                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Location Confirmed:\nLat: $currentLat\nLon: $currentLon",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // TODO: NAVIGATE BACK OR SEND RESULT
                                // Example: (if using Activity)
                                // val result = Intent()
                                // result.putExtra("lat", currentLat)
                                // result.putExtra("lon", currentLon)
                                // (context as Activity).setResult(Activity.RESULT_OK, result)
                                // (context as Activity).finish()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm")
                        }
                    }

                }
            }

            // --- Map View (Takes up remaining space) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Fills available space
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // --- Current Location Display (Requirement 2 Output) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Selected Location (Click/Drag Marker):", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Lat: ${String.format("%.6f", currentLat)}, Lon: ${String.format("%.6f", currentLon)}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
    // Note: The preview will likely not render the map correctly
    MapViewComposable(savedInstanceState = null)
}