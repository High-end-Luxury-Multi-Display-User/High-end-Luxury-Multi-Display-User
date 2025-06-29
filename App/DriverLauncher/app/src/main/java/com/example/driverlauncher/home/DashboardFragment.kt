package com.example.driverlauncher.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.driverlauncher.R
import com.github.anastr.speedviewlib.AwesomeSpeedometer
import kotlin.random.Random

class DashboardFragment : Fragment() {

    private lateinit var speedView: AwesomeSpeedometer
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        speedView = view.findViewById(R.id.speedView) ?: throw IllegalStateException("AwesomeSpeedometer not found")

        // Start dashboard updates
        handler.post(object : Runnable {
            override fun run() {
                updateDashboard()
                handler.postDelayed(this, 2000) // Update every 2 seconds
            }
        })

        return view
    }

    private fun updateDashboard() {
        // Update speedometer
        val speed = Random.nextInt(0, 121).toFloat()
        speedView.speedTo(speed, 1000)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // Clean up handler
    }
}