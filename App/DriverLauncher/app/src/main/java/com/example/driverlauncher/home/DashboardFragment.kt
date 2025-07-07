package com.example.driverlauncher.home

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.example.driverlauncher.IGpsService
import com.example.driverlauncher.R
import com.github.anastr.speedviewlib.AwesomeSpeedometer
import java.lang.reflect.Method

class DashboardFragment : Fragment() {

    private var gpsService: IGpsService? = null
    private var lastSpeed: Float? = null
    private lateinit var speedView: AwesomeSpeedometer
    private lateinit var videoView: VideoView
    private lateinit var fallbackImage: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private val updateIntervalMs = 1000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        speedView = view.findViewById(R.id.speedView) ?: throw IllegalStateException("AwesomeSpeedometer not found")
        videoView = view.findViewById(R.id.background_video) ?: throw IllegalStateException("VideoView not found")
        fallbackImage = view.findViewById(R.id.background_image) ?: throw IllegalStateException("Fallback ImageView not found")

        fallbackImage.visibility = View.VISIBLE

        setupVideo()
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
                    val speed = it.speed
                    updateDashboard(speed)
                    Log.d("GPS-SPEED","The Speed got $speed")
                } ?: Log.w("GPS-UPDATE", "gpsService is null")
            } catch (e: RemoteException) {

                Log.e("GPS-UPDATE", "RemoteException: ${e.message}", e)
            }

            handler.postDelayed(this, updateIntervalMs)
        }
    }
    private fun setupVideo() {
        val videoUri = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.nav_car_night}")

        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mp: MediaPlayer ->
            mp.isLooping = true
            fallbackImage.visibility = View.GONE // Hide fallback when video is ready
            videoView.start()
            videoView.pause()
        }

        videoView.setOnErrorListener { _, _, _ -> false }
    }

    override fun onResume() {
        super.onResume()
        fallbackImage.visibility = View.VISIBLE // Show image immediately
//        videoView.resume()
//        videoView.start()
    }

    override fun onPause() {
        super.onPause()
//        videoView.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        videoView.stopPlayback()
    }

    private fun updateDashboard(speed: Float) {
        speedView.speedTo(speed, 1000)

        lastSpeed?.let { previous ->
//            val delta = kotlin.math.abs(speed - previous)
            if (speed >= 5f) {
                if (videoView.isPlaying) {
                    videoView.pause()
                    Log.d("VIDEO", "Paused video because speed change Δ=$speed km/h")
                }
            } else {
                if (!videoView.isPlaying) {
                    videoView.start()
                    Log.d("VIDEO", "Started video because speed change Δ=$speed km/h")
                }
            }
        }
        lastSpeed = speed
    }

}
