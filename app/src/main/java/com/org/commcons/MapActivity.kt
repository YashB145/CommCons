package com.org.commcons

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.org.commcons.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        loadTasksAndShowMap()
    }

    private fun loadTasksAndShowMap() {
        db.collection("tasks")
            .whereEqualTo("status", "open")
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks = snapshot.documents
                    .mapNotNull { it.toObject(Task::class.java) }
                    .filter { it.locationName.isNotEmpty() }

                if (tasks.isEmpty()) {
                    showMap(getSampleMarkers())
                    return@addOnSuccessListener
                }

                Thread {
                    val results = mutableListOf<MapMarker>()

                    for (task in tasks) {
                        val coords = geocode(task.locationName)
                        if (coords != null) {
                            results.add(
                                MapMarker(
                                    title = task.title,
                                    priority = task.priority,
                                    lat = coords.first,
                                    lng = coords.second
                                )
                            )
                        }
                    }

                    // If geocoding all failed, use samples
                    val finalList = if (results.isEmpty()) getSampleMarkers() else results

                    runOnUiThread { showMap(finalList) }
                }.start()
            }
            .addOnFailureListener {
                showMap(getSampleMarkers())
            }
    }

    private fun geocode(locationName: String): Pair<Double, Double>? {
        return try {
            val query = "$locationName, India".replace(" ", "%20")
            val url = java.net.URL(
                "https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=1"
            )
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("User-Agent", "CommConsApp/1.0")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val response = connection.inputStream.bufferedReader().readText()
            val arr = org.json.JSONArray(response)

            if (arr.length() > 0) {
                val obj = arr.getJSONObject(0)
                Pair(obj.getDouble("lat"), obj.getDouble("lon"))
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getSampleMarkers() = listOf(
        MapMarker("Food Distribution", "high", 19.0760, 72.8777),
        MapMarker("Medical Camp", "medium", 28.6139, 77.2090),
        MapMarker("Education Drive", "low", 12.9716, 77.5946),
        MapMarker("Shelter Support", "high", 22.5726, 88.3639),
        MapMarker("Clean Water Drive", "medium", 17.3850, 78.4867)
    )

    private fun showMap(markers: List<MapMarker>) {
        val webView = binding.webViewMap
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        val markerJs = StringBuilder()
        for (m in markers) {
            val color = when (m.priority) {
                "high" -> "red"
                "medium" -> "orange"
                else -> "green"
            }
            val safeTitle = m.title.replace("'", "\\'")
            markerJs.append(
                """
                L.circleMarker([${m.lat}, ${m.lng}], {
                    color: '$color',
                    fillColor: '$color',
                    fillOpacity: 0.8,
                    radius: 12
                }).addTo(map).bindPopup('<b>$safeTitle</b><br>Priority: ${m.priority.uppercase()}');
                """.trimIndent()
            )
        }

        // Auto-fit map to markers
        val boundsJs = if (markers.isNotEmpty()) {
            val coords = markers.joinToString(",") { "[${it.lat},${it.lng}]" }
            "map.fitBounds([$coords], {padding: [30, 30]});"
        } else {
            "map.setView([20.5937, 78.9629], 5);"
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body { margin: 0; padding: 0; }
                    #map { width: 100vw; height: 100vh; }
                    .legend {
                        position: fixed; bottom: 20px; right: 10px;
                        background: white; padding: 10px; border-radius: 8px;
                        font-family: sans-serif; font-size: 13px;
                        box-shadow: 0 2px 6px rgba(0,0,0,0.3); z-index: 1000;
                    }
                    .dot {
                        display: inline-block; width: 12px; height: 12px;
                        border-radius: 50%; margin-right: 6px;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <div class="legend">
                    <div><span class="dot" style="background:red"></span>High</div>
                    <div><span class="dot" style="background:orange"></span>Medium</div>
                    <div><span class="dot" style="background:green"></span>Low</div>
                </div>
                <script>
                    var map = L.map('map');
                    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                        attribution: '© OpenStreetMap © CARTO'
                    }).addTo(map);
                    $markerJs
                    $boundsJs
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(
            "https://openstreetmap.org", html, "text/html", "UTF-8", null
        )
    }

    data class MapMarker(
        val title: String,
        val priority: String,
        val lat: Double,
        val lng: Double
    )
}