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

        loadMap()
    }

    private fun loadMap() {
        val webView = binding.webViewMap
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        db.collection("tasks")
            .whereEqualTo("status", "open")
            .get()
            .addOnSuccessListener { snapshot ->
                val markers = StringBuilder()

                // Sample markers always shown
                val samples = listOf(
                    Triple("Food Distribution - Mumbai", 19.0760, 72.8777),
                    Triple("Medical Camp - Delhi", 28.6139, 77.2090),
                    Triple("Education Drive - Bangalore", 12.9716, 77.5946),
                    Triple("Shelter Support - Kolkata", 22.5726, 88.3639),
                    Triple("Clean Water - Hyderabad", 17.3850, 78.4867)
                )

                for ((title, lat, lng) in samples) {
                    markers.append("L.marker([$lat, $lng]).addTo(map).bindPopup('$title');\n")
                }

                // Real tasks from Firestore
                for (doc in snapshot.documents) {
                    val task = doc.toObject(Task::class.java) ?: continue
                    val color = when (task.priority) {
                        "high" -> "red"
                        "medium" -> "orange"
                        else -> "green"
                    }
                    markers.append(
                        "L.circleMarker([19.0760, 72.8777], {color:'$color',radius:10})" +
                                ".addTo(map).bindPopup('${task.title} - ${task.priority.uppercase()}');\n"
                    )
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
                                box-shadow: 0 2px 6px rgba(0,0,0,0.3);
                                z-index: 1000;
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
                            <div><span class="dot" style="background:red"></span>High Priority</div>
                            <div><span class="dot" style="background:orange"></span>Medium Priority</div>
                            <div><span class="dot" style="background:green"></span>Low Priority</div>
                        </div>
                        <script>
                            var map = L.map('map').setView([20.5937, 78.9629], 5);
                            L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    attribution: '© OpenStreetMap © CARTO'
}).addTo(map);
                            $markers
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                webView.loadDataWithBaseURL(
                    "https://openstreetmap.org", html, "text/html", "UTF-8", null
                )
            }
    }
}