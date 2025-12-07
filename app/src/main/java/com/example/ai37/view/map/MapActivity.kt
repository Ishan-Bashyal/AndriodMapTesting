package com.example.ai37.view.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ai37.model.MapSearchResultModel
import com.example.ai37.ui.theme.*
import com.example.ai37.util.ApiTokenUtil
import com.google.android.gms.location.*
import kotlinx.coroutines.*
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

private var mapViewState: MapView? = null

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapLibre.getInstance(applicationContext)
        setContent { MapViewComposable(savedInstanceState) }
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

// Helper: Check fine OR coarse
fun hasLocationPermission(context: Context): Boolean {
    return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
@Composable
fun MapViewComposable(savedInstanceState: Bundle?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentLat by remember { mutableStateOf(27.7172) }
    var currentLon by remember { mutableStateOf(85.3240) }
    var inputLatText by remember { mutableStateOf(currentLat.toString()) }
    var inputLonText by remember { mutableStateOf(currentLon.toString()) }
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<MapSearchResultModel>()) }
    var expanded by remember { mutableStateOf(false) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val mapView = remember { MapView(context).apply { onCreate(savedInstanceState) } }
    val styleUrl = "https://api.baato.io/api/v1/styles/breeze_cdn?key=${ApiTokenUtil.BAATO_API_KEY}"

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var markerInstance by remember { mutableStateOf<Marker?>(null) }
    var hasCenteredOnGPS by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationRequest = remember { LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build() }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val pos = LatLng(loc.latitude, loc.longitude)

                // Center map only first time
                if (!hasCenteredOnGPS && mapInstance != null) {
                    hasCenteredOnGPS = true
                    mapInstance?.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(pos, 15.0)
                    )

                    // Update selected location to your GPS on start
                    markerInstance?.position = pos
                    currentLat = loc.latitude
                    currentLon = loc.longitude
                    inputLatText = loc.latitude.toString()
                    inputLonText = loc.longitude.toString()
                }
            }

        }
    }

    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    // Map init
    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            mapInstance = map
            map.setStyle(styleUrl) {
                val pos = LatLng(currentLat, currentLon)
                map.cameraPosition = CameraPosition.Builder().target(pos).zoom(12.0).build()

                // Main marker
                markerInstance = map.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title("Selected Location")
                )

                // Map click listener
                map.addOnMapClickListener { point ->
                    markerInstance?.position = point
                    currentLat = point.latitude
                    currentLon = point.longitude
                    inputLatText = point.latitude.toString()
                    inputLonText = point.longitude.toString()
                    true
                }
            }
        }
        onDispose { }
    }

    // Lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        mapViewState = mapView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    mapView.onDestroy()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            mapViewState = null
            lifecycleOwner.lifecycle.removeObserver(observer)
            fusedLocationClient.removeLocationUpdates(locationCallback)
            mapView.onDestroy()
        }
    }

    // Search function
    fun onSuggestionClick(placeId: Int, mapInstance: MapLibreMap?, markerInstance: Marker?) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = "https://api.baato.io/api/v1/places?key=${ApiTokenUtil.BAATO_API_KEY}&placeId=$placeId"
                val client = OkHttpClient()
                val req = Request.Builder().url(url).build()
                val res = client.newCall(req).execute()
                val body = res.body?.string() ?: return@launch
                val json = JSONObject(body)
                val data = json.getJSONArray("data").getJSONObject(0)
                val c = data.getJSONObject("centroid")
                val lat = c.getDouble("lat")
                val lon = c.getDouble("lon")
                val pos = LatLng(lat, lon)
                withContext(Dispatchers.Main) {
                    // Move existing marker
                    markerInstance?.position = pos
                    mapInstance?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(pos, 14.0))
                    currentLat = lat
                    currentLon = lon
                    inputLatText = lat.toString()
                    inputLonText = lon.toString()
                }
            } catch (_: Exception) {}
        }
    }

    // UI
    Scaffold { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {

            // Top card
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            debounceJob?.cancel()
                            if (it.isNotEmpty()) {
                                expanded = false
                                debounceJob = scope.launch {
                                    delay(500)
                                    expanded = true
                                    launch(Dispatchers.IO) {
                                        try {
                                            val url = "https://api.baato.io/api/v1/search?key=${ApiTokenUtil.BAATO_API_KEY}&q=$it&limit=8"
                                            val client = OkHttpClient()
                                            val req = Request.Builder().url(url).build()
                                            val res = client.newCall(req).execute()
                                            val body = res.body?.string() ?: return@launch
                                            val json = JSONObject(body)
                                            val arr = json.getJSONArray("data")
                                            val temp = mutableListOf<MapSearchResultModel>()
                                            for (i in 0 until arr.length()) {
                                                val obj = arr.getJSONObject(i)
                                                temp.add(MapSearchResultModel(obj.getInt("placeId"), obj.getString("name"), obj.getString("address")))
                                            }
                                            withContext(Dispatchers.Main) { suggestions = temp }
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
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).background(TextBoxColor),
                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
                    )

                    DropdownMenu(expanded, { expanded = false }) {
                        suggestions.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.name}\n${item.address}") },
                                onClick = {
                                    expanded = false
                                    searchQuery = item.name
                                    onSuggestionClick(item.placeId, mapInstance, markerInstance)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Choose Location:", style = MaterialTheme.typography.titleMedium)
                    Text("• Search your location", fontSize = 16.sp)
                    Text("• OR tap anywhere on the map", fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                try {
                                    val lat = inputLatText.toDouble()
                                    val lon = inputLonText.toDouble()
                                    val pos = LatLng(lat, lon)
                                    mapInstance?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(pos, 12.0))
                                    markerInstance?.position = pos
                                    currentLat = lat
                                    currentLon = lon
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid coordinates", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(14.dp)
                        ) { Text("Center Map", color = Color.White) }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Location Confirmed:\nLat: $currentLat\nLon: $currentLon", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).background(Brush.horizontalGradient(ButtonColor), shape = RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) { Text("Confirm", color = White) }
                    }
                }
            }

            // Map
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            }

            // Bottom card
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = TextBoxColor),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Selected Location (Tap to Move):", style = MaterialTheme.typography.titleMedium)
                    Text("Lat: ${String.format("%.6f", currentLat)}, Lon: ${String.format("%.6f", currentLon)}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
