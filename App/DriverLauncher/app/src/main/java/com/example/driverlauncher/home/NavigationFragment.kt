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
    private val updateIntervalMs = 10000L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(requireContext(), requireActivity().getPreferences(0))
    }

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
                Log.d("ServiceBinding", "✅ Bound to IGpsService.")
                startAutoUpdate()
            } else {
                Log.e("ServiceBinding", "❌ Failed to get service binder.")
            }

        } catch (e: Exception) {
            Log.e("ServiceBinding", "❌ Error binding service: ${e.message}", e)
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
                    val mapController = mapView.controller
                    mapController.setZoom(12.0)
                    val startPoint = GeoPoint(lat, lon)
                    mapController.setCenter(startPoint)
                    val marker = Marker(mapView)
                    marker.position = startPoint
                    marker.title = "Egypt"
                    mapView.overlays.add(marker)
                    Log.w("GPS-UPDATE", "Lat & Long got")
                } ?: Log.w("GPS-UPDATE", "gpsService is null")
            } catch (e: RemoteException) {

                Log.e("GPS-UPDATE", "RemoteException: ${e.message}", e)
            }

            handler.postDelayed(this, updateIntervalMs)
        }
    }
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