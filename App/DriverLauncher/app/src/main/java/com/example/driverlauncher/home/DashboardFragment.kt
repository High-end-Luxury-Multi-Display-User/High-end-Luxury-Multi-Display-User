package com.example.driverlauncher.home

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.example.driverlauncher.R
import com.github.anastr.speedviewlib.AwesomeSpeedometer
import kotlin.random.Random
class DashboardFragment : Fragment() {

    private lateinit var speedView: AwesomeSpeedometer
    private lateinit var videoView: VideoView
    private lateinit var fallbackImage: ImageView
    private val handler = Handler(Looper.getMainLooper())

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

        // Start dashboard updates
        handler.post(object : Runnable {
            override fun run() {
                updateDashboard()
                handler.postDelayed(this, 2000)
            }
        })

        return view
    }

    private fun setupVideo() {
        val videoUri = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.nav_car_night}")

        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mp: MediaPlayer ->
            mp.isLooping = true
            fallbackImage.visibility = View.GONE // Hide fallback when video is ready
            videoView.start()
        }

        videoView.setOnErrorListener { _, _, _ -> false }
    }

    override fun onResume() {
        super.onResume()
        fallbackImage.visibility = View.VISIBLE // Show image immediately
        videoView.resume()
        videoView.start()
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        videoView.stopPlayback()
    }

    private fun updateDashboard() {
        val speed = Random.nextInt(0, 121).toFloat()
        speedView.speedTo(speed, 1000)
    }
}

