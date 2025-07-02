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
import android.os.UserManager

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

        lightIcon = findViewById(R.id.light_icon)!! // the ImageView inside the light_button

        val lightButton = findViewById<LinearLayout>(R.id.light_button)!!
        lightButton.setOnClickListener {
            ledState = !ledState
            setLedState(ledState)
            updateLightIcon(ledState)
        }

        car = Car.createCar(this.applicationContext)!!
        if (car == null) {
            Log.e("LED", "Failed to create Car instance")
        } else {
            carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            Log.d("LED", "CarPropertyManager initialized")
        }
        
        // Initialize views with non-null check
        timeTextView = findViewById(R.id.time)
            ?: throw IllegalStateException("Missing time TextView")
        val themeContainer = findViewById<LinearLayout>(R.id.theme_container)
            ?: throw IllegalStateException("Missing theme_container")
        val themeImage = findViewById<ImageButton>(R.id.theme_image)
            ?: throw IllegalStateException("Missing theme_image")
        val gestureContainer = findViewById<LinearLayout>(R.id.gesture_container)
            ?: throw IllegalStateException("Missing gesture_container")
        val gestureImage = findViewById<ImageButton>(R.id.gesture_image)
            ?: throw IllegalStateException("Missing gesture_image")
        val voiceContainer = findViewById<LinearLayout>(R.id.voice_container)
            ?: throw IllegalStateException("Missing voice_container")
        val voiceImage = findViewById<ImageButton>(R.id.voice_image)
            ?: throw IllegalStateException("Missing voice_image")
        val languageContainer = findViewById<LinearLayout>(R.id.language_container)
            ?: throw IllegalStateException("Missing language_container")
        val languageImage = findViewById<ImageButton>(R.id.language_image)
            ?: throw IllegalStateException("Missing language_image")
        val homeIcon = findViewById<ImageView>(R.id.icon_home)
            ?: throw IllegalStateException("Missing icon_home")
        val userIcon = findViewById<ImageView>(R.id.user_profile)
            ?: throw IllegalStateException("Missing user_profile")
        val profileName = findViewById<TextView>(R.id.profile_name)
            ?: throw IllegalStateException("Missing profile_name")

        // Hide system navigation bar (for automotive)
        hideSystemNavigationBar()

        // Initialize time updater
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateTime()
                timeUpdateHandler.postDelayed(this, 60000) // every minute
            }
        }
        updateTime()
        timeUpdateHandler.postDelayed(timeUpdateRunnable, 60000)

        // Setup click listeners
        themeContainer.setOnClickListener {
            toggleImage(themeImage, R.drawable.day, R.drawable.night)
        }
        gestureContainer.setOnClickListener {
            toggleImage(gestureImage, R.drawable.gesture, R.drawable.no_gesture)
        }
        voiceContainer.setOnClickListener {
            toggleImage(voiceImage, R.drawable.voice, R.drawable.no_voice)
        }
        languageContainer.setOnClickListener {
            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
        }
        homeIcon.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        userIcon.setOnClickListener {
            val driverIntent = Intent().apply {
                setClassName(
                    "com.android.systemui",
                    "com.android.systemui.car.userpicker.UserPickerActivity"
                )
            }
            startActivity(driverIntent)
        }
        
        // Set click listener for carVitals icon
        val carVitalsIcon = findViewById<ImageView>(R.id.icon_car_vitals)!!
        carVitalsIcon.setOnClickListener {
            val intent = Intent(this, CarVitalsActivity::class.java)
            startActivity(intent)
        }

        // Load user name safely
        val userManager = getSystemService(UserManager::class.java)
        val userName = try {
            userManager?.getUserName()
        } catch (e: SecurityException) {
            Log.e("SettingsActivity", "Missing permission to get user name", e)
            null
        }
        profileName.text = userName ?: "Unknown User"
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
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
    }

    private fun toggleImage(imageButton: ImageButton, offResource: Int, onResource: Int) {
        if (imageButton.drawable.constantState == resources.getDrawable(offResource, theme).constantState) {
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
            lightIcon.setImageResource(R.drawable.ic_led_off)
        } else {
            lightIcon.setImageResource(R.drawable.ic_led_on)
        }
    }
}

