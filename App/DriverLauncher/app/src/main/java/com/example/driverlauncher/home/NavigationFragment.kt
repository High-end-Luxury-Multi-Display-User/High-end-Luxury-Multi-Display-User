package com.example.driverlauncher.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.driverlauncher.IGpsService
import com.example.driverlauncher.R
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.lang.reflect.Method

class NavigationFragment : Fragment() {
    private var gpsService: IGpsService? = null
    private lateinit var mapView: MapView
    private val handler = Handler(Looper.getMainLooper())
    private val updateIntervalMs = 1000L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(requireContext(), requireActivity().getPreferences(0))
    }

    private lateinit var carMarker: Marker

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_navigation, container, false)
        mapView = view.findViewById(R.id.map_view) ?: throw IllegalStateException("MapView not found")

        // Set up map
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        // Initialize marker at dummy start point
        val startPoint = GeoPoint(30.0444, 31.2357) // initial location
        carMarker = Marker(mapView)
        carMarker.position = startPoint
        carMarker.icon = resources.getDrawable(R.drawable.map_car64, null) // use your car drawable
        carMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        carMarker.title = "My Car"
        mapView.overlays.add(carMarker)
        val mapController = mapView.controller
        mapController.setZoom(18.0) // reasonable zoom level; adjust to your needs
        mapController.setCenter(startPoint)

        //    val mapController = mapView.controller
    //    mapController.setZoom(12.0)
    //  val startPoint = GeoPoint(30.0444, 31.2357) // New York
    //    mapController.setCenter(startPoint)
    //    val marker = Marker(mapView)
    //    marker.position = startPoint
    //    marker.title = "Egypt"
    //    mapView.overlays.add(marker)
        // GPS
        bindGpsService()
        startAutoUpdate()
        return view
    }
    private fun bindGpsService() {
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod: Method = serviceManagerClass.getMethod("getService", String::class.java)
            val result = getServiceMethod.invoke(null, "com.example.driverlauncher.IGpsService/default")

            if (result != null) {
                val binder = result as IBinder
                gpsService = IGpsService.Stub.asInterface(binder)
                Log.d("ServiceBinding", "Bound to IGpsService.")
                startAutoUpdate()
            } else {
                Log.e("ServiceBinding", "Failed to get service binder.")
            }

        } catch (e: Exception) {
            Log.e("ServiceBinding", "Error binding service: ${e.message}", e)
        }
    }

    private fun startAutoUpdate() {
        handler.post(updateTask)
    }

    private val updateTask = object : Runnable {
        override fun run() {
            try {
                gpsService?.let {
                    val lat = it.latitude
                    val lon = it.longitude
                    val newPosition = GeoPoint(lat, lon)

                    // Update marker position
                    carMarker.position = newPosition

                    // Optionally center the map
                    val mapController = mapView.controller
                    mapController.animateTo(newPosition) // animate instead of setCenter
                    mapView.invalidate() // force redraw
                    Log.w("GPS-UPDATE", "Updated car position to: $lat, $lon")
                } ?: Log.w("GPS-UPDATE", "gpsService is null")
            } catch (e: RemoteException) {
                Log.e("GPS-UPDATE", "RemoteException: ${e.message}", e)
            }
            handler.postDelayed(this, updateIntervalMs)
        }
    }

    //    private val updateTask = object : Runnable {
//        override fun run() {
//            try {
//                gpsService?.let {
//                    val lat = it.latitude
//                    val lon = it.longitude
//                    val mapController = mapView.controller
//                    mapController.setZoom(12.0)
//                    val startPoint = GeoPoint(lat, lon)
//                    mapController.setCenter(startPoint)
//                    val marker = Marker(mapView)
//                    marker.position = startPoint
//                    marker.title = "Egypt"
//                    mapView.overlays.add(marker)
//                    Log.w("GPS-UPDATE", "Lat & Long got")
//                } ?: Log.w("GPS-UPDATE", "gpsService is null")
//            } catch (e: RemoteException) {
//
//                Log.e("GPS-UPDATE", "RemoteException: ${e.message}", e)
//            }
//
//            handler.postDelayed(this, updateIntervalMs)
//        }
//    }
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTask)
    }
}