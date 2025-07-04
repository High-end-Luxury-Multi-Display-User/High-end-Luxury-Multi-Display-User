package com.example.driverlauncher.home

import android.Manifest
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.driverlauncher.R
import com.example.driverlauncher.carvitals.CarVitalsActivity
import com.example.driverlauncher.settings.SettingsActivity
import com.example.driverlauncher.voskva.VoskRecognitionService
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity(), VoskRecognitionService.RecognitionCallback {
//    val  VENDOR_EXTENSION_Light_CONTROL_PROPERTY:Int = 0x21400106
//    val areaID = 0
//    lateinit var car: Car
//    lateinit var carPropertyManager: CarPropertyManager
companion object {
    private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
}

//    private lateinit var micIcon: ImageView
//    private lateinit var statusImage: ImageView
    private var isServiceRunning = false
    private var voskService: VoskRecognitionService? = null
    private var isBound = false
    private var lastCommand = ""
    private var ledState = false // false = off, true = on

    private lateinit var lightIcon: ImageView
    private lateinit var timeTextView: TextView
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as VoskRecognitionService.LocalBinder
            voskService = binder.getService().apply {
                setRecognitionCallback(this@HomeActivity)
            }
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            voskService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        /********************************************/
//        micIcon = findViewById(R.id.mic_icon)
//        statusImage = findViewById(R.id.statusImage)
//
//        micIcon.setOnClickListener { toggleService() }

        checkPermissions()
        startServiceOnlyOnce()
        /********************************************/

        lightIcon = findViewById(R.id.light_icon) // the ImageView inside the light_button

        val lightButton = findViewById<LinearLayout>(R.id.light_button)
        lightButton.setOnClickListener {
            ledState = !ledState
            setLedState(ledState)
            updateLightIcon(ledState)
        }

//        car = Car.createCar(this.applicationContext)
//        if (car == null) {
//            Log.e("LED", "Failed to create Car instance")
//        } else {
//            carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
//            Log.d("LED", "CarPropertyManager initialized")
//        }

        // Initialize the time TextView
        timeTextView = findViewById(R.id.time) ?: run {
            Log.e("TimeUpdate", "Time TextView not found!")
            return
        }

        // Hide system navigation bar (bottom) while keeping status bar (top) visible
        hideSystemBars()

        // Initialize time updater
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateTime()
                timeUpdateHandler.postDelayed(this, 60000) // Update every minute
            }
        }

        // Load fragments
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.navigation_container, NavigationFragment())
                .replace(R.id.dashboard_container, DashboardFragment())
                .commit()
        }

        // Set click listener for settings icon
        val settingsIcon = findViewById<ImageView>(R.id.icon_settings)
        settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for carVitals icon
        val carVitalsIcon = findViewById<ImageView>(R.id.icon_car_vitals)
        carVitalsIcon.setOnClickListener {
            val intent = Intent(this, CarVitalsActivity::class.java)
            startActivity(intent)
        }

        // Update time immediately and schedule updates
        updateTime()
        timeUpdateHandler.postDelayed(timeUpdateRunnable, 60000)
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && packageManager.hasSystemFeature("android.hardware.type.automotive")) {
            hideSystemBars()
        }
    }

    private fun setLedState(state: Boolean) {
//        val value = if (state) 1 else 0
//        try {
//            synchronized(carPropertyManager) {
//                carPropertyManager.setProperty(
//                    Integer::class.java,
//                    VENDOR_EXTENSION_Light_CONTROL_PROPERTY,
//                    areaID,
//                    Integer(value)
//                )
//                Log.d("LED", "LED state set to: $value")
//            }
//        } catch (e: Exception) {
//            Log.e("LED", "Failed to set LED state", e)
//        }
    }
    private fun updateLightIcon(state: Boolean) {
        if (state) {
            lightIcon.setImageResource(R.drawable.light_on)
        } else {
            lightIcon.setImageResource(R.drawable.light_off)
        }
    }

    override fun onResume() {
        super.onResume()
        bindService(
            Intent(this, VoskRecognitionService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onPause() {
        super.onPause()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun handleCommand(command: String) {
        when (command) {
            "greet" -> {
                Toast.makeText(this, "Hello! How can I assist you?", Toast.LENGTH_SHORT).show()
                playAudio(R.raw.greet)
            }
            "light_on" -> {
//                statusImage.setImageResource(R.drawable.ledon)
                updateLightIcon(true)
                playAudio(R.raw.lighton)
            }
            "light_off" -> {
//                statusImage.setImageResource(R.drawable.ledoff)
                updateLightIcon(false)
                playAudio(R.raw.lightoff)
            }
            "day_mode" -> {
//                statusImage.setImageResource(R.drawable.sun)
                playAudio(R.raw.day)
            }
            "night_mode" -> {
//                statusImage.setImageResource(R.drawable.moon)
                playAudio(R.raw.night)
            }
            "seat_45" -> {
//                statusImage.setImageResource(R.drawable.seat_45)
                playAudio(R.raw.seat)
            }
            "seat_70" -> {
//                statusImage.setImageResource(R.drawable.seat_70)
                playAudio(R.raw.seatdefault)
            }
            "seat_90" -> {
//                statusImage.setImageResource(R.drawable.seat_90)
                playAudio(R.raw.seatmax)
            }
        }
    }

    private fun playAudio(resId: Int) {
        MediaPlayer.create(this, resId).apply {
            setOnCompletionListener { mp -> mp.release() }
            start()
        }
    }

    private fun toggleService() {
        Intent(this, VoskRecognitionService::class.java).apply {
            action = if (isServiceRunning) {
                VoskRecognitionService.ACTION_STOP_RECOGNITION
            } else {
                VoskRecognitionService.ACTION_START_RECOGNITION
            }
            startService(this)
        }
        isServiceRunning = !isServiceRunning
        updateMicIcon()
    }

    private fun updateMicIcon() {
//        micIcon.setImageResource(
//            if (isServiceRunning) R.drawable.ic_mic_on else R.drawable.ic_mic_off
//        )
    }

    private fun startServiceOnlyOnce() {
        startService(Intent(this, VoskRecognitionService::class.java))
        isServiceRunning = true
        updateMicIcon()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                HomeActivity.Companion.PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        }
    }

    override fun onCommandReceived(command: String) {
        if (command == lastCommand) return
        lastCommand = command

        runOnUiThread { handleCommand(command) }
    }
}