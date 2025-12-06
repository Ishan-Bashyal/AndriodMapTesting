package com.example.ai37.view.map

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ai37.model.SearchResult
import com.example.ai37.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


// Global MapView reference
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Initial coordinates
    var currentLat by remember { mutableStateOf(27.7172) }
    var currentLon by remember { mutableStateOf(85.3240) }

    // Input fields
    var inputLatText by remember { mutableStateOf(currentLat.toString()) }
    var inputLonText by remember { mutableStateOf(currentLon.toString()) }

    // Search states
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<SearchResult>()) }
    var expanded by remember { mutableStateOf(false) }

    val styleUrl = remember { "https://api.baato.io/api/v1/styles/breeze_cdn?key=${Constants.BAATO_API_KEY}" }

    val mapView = remember {
        MapView(context).apply { onCreate(savedInstanceState) }
    }

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var markerInstance by remember { mutableStateOf<Marker?>(null) }

    var debounceJob by remember { mutableStateOf<Job?>(null) }

    // Map setup
    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            mapInstance = map
            map.setStyle(styleUrl) {
                val initialPosition = LatLng(currentLat, currentLon)
                map.cameraPosition = CameraPosition.Builder().target(initialPosition).zoom(12.0).build()
                val markerOptions = MarkerOptions().position(initialPosition).title("Selected Location")
                markerInstance = map.addMarker(markerOptions)

                map.addOnMapClickListener { point ->
                    markerInstance?.position = point
                    currentLat = point.latitude
                    currentLon = point.longitude
                    inputLatText = currentLat.toString()
                    inputLonText = currentLon.toString()
                    true
                }
            }
        }
        onDispose { }
    }

    // MapView lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        mapViewState = mapView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            mapViewState = null
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Function to handle suggestion click
    fun onSuggestionClick(
        placeId: Int,
        mapInstance: MapLibreMap?,
        markerInstance: Marker?,
        updateLatLon: (Double, Double) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = "https://api.baato.io/api/v1/places?key=${Constants.BAATO_API_KEY}&placeId=$placeId"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (!body.isNullOrEmpty()) {
                    val json = JSONObject(body)
                    val data = json.getJSONArray("data").getJSONObject(0)
                    val centroid = data.getJSONObject("centroid")
                    val lat = centroid.getDouble("lat")
                    val lon = centroid.getDouble("lon")
                    val newPos = LatLng(lat, lon)

                    withContext(Dispatchers.Main) {
                        mapInstance?.animateCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(newPos, 14.0)
                        )
                        markerInstance?.position = newPos
                        updateLatLon(lat, lon)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading place details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Search + Lat/Lon Input
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it

                            // Cancel previous debounce job
                            debounceJob?.cancel()

                            if (it.isNotEmpty()) {
                                expanded = false  // Don't show dropdown until search is ready

                                debounceJob = scope.launch {
                                    delay(500)  // delay in millisecond

                                    expanded = true

                                    launch(Dispatchers.IO) {
                                        try {
                                            val url =
                                                "https://api.baato.io/api/v1/search?key=${Constants.BAATO_API_KEY}&q=$it&limit=8"

                                            val client = OkHttpClient()
                                            val request = Request.Builder().url(url).build()
                                            val response = client.newCall(request).execute()
                                            val body = response.body?.string()

                                            if (!body.isNullOrEmpty()) {
                                                val json = JSONObject(body)
                                                val dataArray = json.getJSONArray("data")

                                                val tempList = mutableListOf<SearchResult>()

                                                for (i in 0 until dataArray.length()) {
                                                    val obj = dataArray.getJSONObject(i)
                                                    tempList.add(
                                                        SearchResult(
                                                            placeId = obj.getInt("placeId"),
                                                            name = obj.getString("name"),
                                                            address = obj.getString("address")
                                                        )
                                                    )
                                                }

                                                withContext(Dispatchers.Main) {
                                                    suggestions = tempList
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                            } else {
                                expanded = false
                                suggestions = emptyList()
                            }
                        },
                        placeholder = { Text("Search location...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        trailingIcon = {
                            IconButton(onClick = { /* optional: trigger search */ }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    )

                    // Dropdown
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestions.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.name}\n${item.address}") },
                                onClick = {
                                    expanded = false
                                    searchQuery = item.name
                                    onSuggestionClick(
                                        placeId = item.placeId,
                                        mapInstance = mapInstance,
                                        markerInstance = markerInstance
                                    ) { lat, lon ->
                                        currentLat = lat
                                        currentLon = lon
                                        inputLatText = lat.toString()
                                        inputLonText = lon.toString()
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text("Choose Location:", style = MaterialTheme.typography.titleMedium)
                    Text("• Type latitude/longitude and press the button", fontSize = 16.sp)
                    Text("• OR tap anywhere on the map", fontSize = 16.sp)
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputLonText,
                            onValueChange = { inputLonText = it },
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                try {
                                    val lat = inputLatText.toDouble()
                                    val lon = inputLonText.toDouble()
                                    val newPos = LatLng(lat, lon)
                                    mapInstance?.animateCamera(
                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(newPos, 12.0)
                                    )
                                    markerInstance?.position = newPos
                                    currentLat = lat
                                    currentLon = lon
                                } catch (e: NumberFormatException) {
                                    Toast.makeText(context, "Invalid coordinates", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Center Map") }

                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Location Confirmed:\nLat: $currentLat\nLon: $currentLon",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Confirm") }
                    }
                }
            }

            // Map
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            }

            // Current location display
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Selected Location (Click/Drag Marker):", style = MaterialTheme.typography.titleMedium)
                    Text("Lat: ${String.format("%.6f", currentLat)}, Lon: ${String.format("%.6f", currentLon)}",
                        style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
