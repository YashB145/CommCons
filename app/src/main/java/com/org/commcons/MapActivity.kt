package com.org.commcons

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.org.commcons.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Default camera position — India center
        val india = LatLng(20.5937, 78.9629)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 5f))

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        loadTasksOnMap()

        // Click on marker → open task detail
        googleMap.setOnInfoWindowClickListener { marker ->
            val taskId = marker.tag as? String ?: return@setOnInfoWindowClickListener
            val intent = Intent(this, TaskDetailActivity::class.java)
            intent.putExtra("taskId", taskId)
            intent.putExtra("userRole", "volunteer")
            startActivity(intent)
        }
    }

    private fun loadTasksOnMap() {
        db.collection("tasks")
            .whereEqualTo("status", "open")
            .get()
            .addOnSuccessListener { snapshot ->
                var tasksWithLocation = 0

                for (doc in snapshot.documents) {
                    val task = doc.toObject(Task::class.java) ?: continue
                    val locationName = task.locationName

                    if (locationName.isNotEmpty()) {
                        // Geocode location name to coordinates
                        geocodeAndAddMarker(task)
                        tasksWithLocation++
                    }
                }

                if (tasksWithLocation == 0) {
                    // Add sample markers if no location data
                    addSampleMarkers()
                    Toast.makeText(
                        this,
                        "Showing sample locations. Add locations to tasks for real markers.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                addSampleMarkers()
            }
    }

    private fun geocodeAndAddMarker(task: Task) {
        try {
            val geocoder = android.location.Geocoder(this)
            val results = geocoder.getFromLocationName(task.locationName, 1)
            if (results != null && results.isNotEmpty()) {
                val location = results[0]
                val latLng = LatLng(location.latitude, location.longitude)
                addMarker(task, latLng)
            }
        } catch (e: Exception) {
            // Geocoding failed, skip this task
        }
    }

    private fun addMarker(task: Task, latLng: LatLng) {
        val markerColor = when (task.priority) {
            "high" -> BitmapDescriptorFactory.HUE_RED
            "medium" -> BitmapDescriptorFactory.HUE_ORANGE
            else -> BitmapDescriptorFactory.HUE_GREEN
        }

        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(task.title)
                .snippet("Priority: ${task.priority.uppercase()} | Tap for details")
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
        )
        marker?.tag = task.id
    }

    private fun addSampleMarkers() {
        val sampleLocations = listOf(
            Triple("Food Distribution", LatLng(19.0760, 72.8777), "high"),    // Mumbai
            Triple("Medical Camp", LatLng(28.6139, 77.2090), "medium"),       // Delhi
            Triple("Education Drive", LatLng(12.9716, 77.5946), "low"),       // Bangalore
            Triple("Shelter Support", LatLng(22.5726, 88.3639), "high"),      // Kolkata
            Triple("Clean Water", LatLng(17.3850, 78.4867), "medium")         // Hyderabad
        )

        for ((title, latLng, priority) in sampleLocations) {
            val markerColor = when (priority) {
                "high" -> BitmapDescriptorFactory.HUE_RED
                "medium" -> BitmapDescriptorFactory.HUE_ORANGE
                else -> BitmapDescriptorFactory.HUE_GREEN
            }
            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .snippet("Priority: ${priority.uppercase()}")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )
        }
        // Zoom to India
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(20.5937, 78.9629), 5f)
        )
    }
}