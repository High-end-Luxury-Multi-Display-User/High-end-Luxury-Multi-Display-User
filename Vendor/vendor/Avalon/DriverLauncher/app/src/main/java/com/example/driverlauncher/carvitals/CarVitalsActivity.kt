package com.example.driverlauncher.carvitals

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.driverlauncher.R
import com.example.driverlauncher.home.HomeActivity
import com.example.driverlauncher.settings.SettingsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.UserManager

class CarVitalsActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_car_vitals)

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

        lightIcon = findViewById<ImageView>(R.id.light_icon)!! // the ImageView inside the light_button

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
        supportFragmentManager.beginTransaction()
            .replace(R.id.batteryContainer, BatteryFragment())
            .replace(R.id.mainFragmentContainer,CarVitalsFragment())
            .replace(R.id.rightFragmentContainer, SeatFragment())
            .commit()

        hideSystemBars()
        
         // Load user name safely
        val userManager = getSystemService(UserManager::class.java)
        val userName = try {
            userManager?.getUserName()
        } catch (e: SecurityException) {
            Log.e("SettingsActivity", "Missing permission to get user name", e)
            null
        }
        
        val profileName = findViewById<TextView>(R.id.profile_name)
            ?: throw IllegalStateException("Missing profile_name")

        profileName.text = userName ?: "Unknown User"

        // Set click listener for home icon
        val homeIcon = findViewById<ImageView>(R.id.icon_home)!!
        homeIcon.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for settings icon
        val settingsIcon = findViewById<ImageView>(R.id.icon_settings)!!
        settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
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
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()

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

    private fun hideSystemBars() {
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

}
