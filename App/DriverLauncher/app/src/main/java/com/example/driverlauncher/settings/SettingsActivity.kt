package com.example.driverlauncher.settings

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.driverlauncher.R
import com.example.driverlauncher.home.HomeActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var timeTextView: TextView
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // Initialize the time TextView
        timeTextView = findViewById(R.id.time) ?: run {
            Log.e("TimeUpdate", "Time TextView not found!")
            return
        }
        // Initialize time updater
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateTime()
                timeUpdateHandler.postDelayed(this, 60000) // Update every minute
            }
        }

        // Update time immediately and schedule updates
        updateTime()
        timeUpdateHandler.postDelayed(timeUpdateRunnable, 60000)
        hideSystemNavigationBar()

        // Theme Switch
        val themeContainer = findViewById<LinearLayout>(R.id.theme_container)
        val themeImage = findViewById<ImageButton>(R.id.theme_image)
        themeContainer.setOnClickListener {
            toggleImage(themeImage, R.drawable.day, R.drawable.night)
        }

        // Gesture Control Switch
        val gestureContainer = findViewById<LinearLayout>(R.id.gesture_container)
        val gestureImage = findViewById<ImageButton>(R.id.gesture_image)
        gestureContainer.setOnClickListener {
            toggleImage(gestureImage, R.drawable.gesture, R.drawable.no_gesture)
        }

        // Voice Assist Switch
        val voiceContainer = findViewById<LinearLayout>(R.id.voice_container)
        val voiceImage = findViewById<ImageButton>(R.id.voice_image)
        voiceContainer.setOnClickListener {
            toggleImage(voiceImage, R.drawable.voice, R.drawable.no_voice)
        }

        // Language Switch
        val languageContainer = findViewById<LinearLayout>(R.id.language_container)
        val languageImage = findViewById<ImageButton>(R.id.language_image)
        languageContainer.setOnClickListener {
            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
        }

        // Set click listener for settings icon
        val homeIcon = findViewById<ImageView>(R.id.icon_home)
        homeIcon.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun hideSystemNavigationBar() {
        if (packageManager.hasSystemFeature("android.hardware.type.automotive")) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun updateTime() {
        try {
            val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            Log.d("TimeUpdate", "Current time: $currentTime")
            timeTextView.text = currentTime
        } catch (e: Exception) {
            Log.e("TimeUpdate", "Error updating time", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable) // Clean up
    }

    private fun toggleImage(imageButton: ImageButton, offResource: Int, onResource: Int) {
        if (imageButton.drawable.constantState == resources.getDrawable(offResource).constantState) {
            imageButton.setImageResource(onResource)
        } else {
            imageButton.setImageResource(offResource)
        }
    }
}