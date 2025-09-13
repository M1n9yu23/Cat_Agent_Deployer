package com.bossmg.android.catagentdeployer

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import com.bossmg.android.catagentdeployer.databinding.ActivityGoogleMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class GoogleMapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null
    private lateinit var binding: ActivityGoogleMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGoogleMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                getLastLocation()
            } else {
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            setOnMapClickListener { latLng ->
                addOrMoveSelectedPositionMarker(latLng)
            }
        }
        when {
            hasLocationPermission() -> getLastLocation()
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }

            else -> requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLocation = LatLng(
                        location.latitude, location.longitude
                    )
                    updateMapLocation(userLocation)
                    addMarkerAtLocation(userLocation, "You")
                }
            }
    }

    private fun updateMapLocation(location: LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 7f))
    }

    private fun addMarkerAtLocation(location: LatLng, title: String, markerIcon: BitmapDescriptor? = null) =
        mMap.addMarker(MarkerOptions().title(title).position(location))
            .apply {
                markerIcon?.let { this?.setIcon(it) }
            }


    private fun getBitmapDescriptorFromVector(@DrawableRes vectorDrawableResId: Int): BitmapDescriptor? {
        val bitmap =
            ContextCompat.getDrawable(this, vectorDrawableResId)?.let { vectorDrawable ->
                vectorDrawable
                    .setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)

                val drawableWithTint = DrawableCompat.wrap(vectorDrawable)
                DrawableCompat.setTint(drawableWithTint, Color.RED)

                val bitmap = createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
                val canvas = Canvas(bitmap)
                drawableWithTint.draw(canvas)
                bitmap
            } ?: return null
        return BitmapDescriptorFactory.fromBitmap(bitmap).also {
            bitmap.recycle()
        }
    }

    private fun addOrMoveSelectedPositionMarker(latLng: LatLng) {
        if (marker == null) {
            marker = addMarkerAtLocation(
                latLng, "Deploy here", getBitmapDescriptorFromVector(R.drawable.tartget_icon)
            )
        } else {
            marker?.apply {
                position = latLng
            }
        }
    }

    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Location permission")
            .setMessage("This app will not work without knowing your current location")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                positiveAction()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }.create().show()
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED

    companion object {
        private const val TAG = "GoogleMapsActivity"
    }
}