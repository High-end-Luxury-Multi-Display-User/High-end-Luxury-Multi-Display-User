package com.example.driverlauncher.settings

import android.car.Car
import android.car.hardware.property.CarPropertyManager
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
import com.example.driverlauncher.carvitals.CarVitalsActivity
import com.example.driverlauncher.home.HomeActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    val  VENDOR_EXTENSION_Light_CONTROL_PROPERTY:Int = 0x21400106
    val areaID = 0
    lateinit var car: Car
    lateinit var carPropertyManager: CarPropertyManager
    private var ledState = false // false = off, true = on

    private lateinit var lightIcon: ImageView
    private lateinit var timeTextView: TextView
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        lightIcon = findViewById(R.id.light_icon) // the ImageView inside the light_button

        val lightButton = findViewById<LinearLayout>(R.id.light_button)
        lightButton.setOnClickListener {
            ledState = !ledState
            setLedState(ledState)
            updateLightIcon(ledState)
        }

        car = Car.createCar(this.applicationContext)
        if (car == null) {
            Log.e("LED", "Failed to create Car instance")
        } else {
            carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            Log.d("LED", "CarPropertyManager initialized")
        }

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
        themeImage.setOnClickListener {
            toggleImage(themeImage, R.drawable.day, R.drawable.night)
        }

        // Gesture Control Switch
        val gestureContainer = findViewById<LinearLayout>(R.id.gesture_container)
        val gestureImage = findViewById<ImageButton>(R.id.gesture_image)
        gestureContainer.setOnClickListener {
            toggleImage(gestureImage, R.drawable.gesture, R.drawable.no_gesture)
        }
        gestureImage.setOnClickListener {
            toggleImage(gestureImage, R.drawable.gesture, R.drawable.no_gesture)
        }

        // Voice Assist Switch
        val voiceContainer = findViewById<LinearLayout>(R.id.voice_container)
        val voiceImage = findViewById<ImageButton>(R.id.voice_image)
        voiceContainer.setOnClickListener {
            toggleImage(voiceImage, R.drawable.voice, R.drawable.no_voice)
        }
        voiceImage.setOnClickListener {
            toggleImage(voiceImage, R.drawable.voice, R.drawable.no_voice)
        }

        // Language Switch
        val languageContainer = findViewById<LinearLayout>(R.id.language_container)
        val languageImage = findViewById<ImageButton>(R.id.language_image)
        languageContainer.setOnClickListener {
            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
        }
        languageImage.setOnClickListener {
            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
        }

        // Set click listener for home icon
        val homeIcon = findViewById<ImageView>(R.id.icon_home)
        homeIcon.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for carVitals icon
        val carVitalsIcon = findViewById<ImageView>(R.id.icon_car_vitals)
        carVitalsIcon.setOnClickListener {
            val intent = Intent(this, CarVitalsActivity::class.java)
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
    private fun setLedState(state: Boolean) {
        val value = if (state) 1 else 0
        try {
            synchronized(carPropertyManager) {
                carPropertyManager.setProperty(
                    Integer::class.java,
                    VENDOR_EXTENSION_Light_CONTROL_PROPERTY,
                    areaID,
                    Integer(value)
                )
                Log.d("LED", "LED state set to: $value")
            }
        } catch (e: Exception) {
            Log.e("LED", "Failed to set LED state", e)
        }
    }
    private fun updateLightIcon(state: Boolean) {
        if (state) {
            lightIcon.setImageResource(R.drawable.light_on)
        } else {
            lightIcon.setImageResource(R.drawable.light_off)
        }
    }
}