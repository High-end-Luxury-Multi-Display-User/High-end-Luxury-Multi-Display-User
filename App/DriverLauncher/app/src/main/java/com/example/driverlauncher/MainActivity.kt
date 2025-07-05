package com.example.driverlauncher
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.driverlauncher.voskva.VoskRecognitionService
import java.text.SimpleDateFormat
import java.util.*

import android.widget.FrameLayout
import android.widget.Toast
import com.example.driverlauncher.carvitals.BatteryFragment
import com.example.driverlauncher.carvitals.CarVitalsFragment
import com.example.driverlauncher.carvitals.SeatFragment
import com.example.driverlauncher.handgesture.CameraCaptureService
import com.example.driverlauncher.home.DashboardFragment
import com.example.driverlauncher.home.NavigationFragment
import com.example.driverlauncher.settings.SettingsFragment

class MainActivity : AppCompatActivity(), VoskRecognitionService.RecognitionCallback {
    companion object {
        const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
        var isServiceRunning = false
        var voskService: VoskRecognitionService? = null
        var isBound = false
        var lastCommand = ""
    }
    private val VENDOR_EXTENSION_LIGHT_CONTROL_PROPERTY: Int = 0x21400106
    private val areaID = 0
    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager
    private var ledState = false // false = off, true = on
    private var currentScreen = Screen.HOME // Track current screen state

    private lateinit var lightIcon: ImageView
    private lateinit var timeTextView: TextView
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable
    private lateinit var homeIcon: ImageView
    private lateinit var carVitalsIcon: ImageView
    private lateinit var settingsIcon: ImageView

    enum class Screen {
        HOME, CAR_VITALS, SETTINGS
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as VoskRecognitionService.LocalBinder
            voskService = binder.getService().apply {
                setRecognitionCallback(this@MainActivity)
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
        setContentView(R.layout.activity_main)

        /*********************************************/
        // VA shenanigans
        checkPermissions()
        startServiceOnlyOnce()
        /********************************************/

        // Initialize views
        lightIcon = findViewById(R.id.light_icon)
        timeTextView = findViewById(R.id.time) ?: run {
            Log.e("TimeUpdate", "Time TextView not found!")
            return
        }
        homeIcon = findViewById(R.id.icon_home)
        carVitalsIcon = findViewById(R.id.icon_car_vitals)
        settingsIcon = findViewById(R.id.icon_settings)

        // Initialize Car API
        car = Car.createCar(this.applicationContext)
        if (car == null) {
            Log.e("LED", "Failed to create Car instance")
        } else {
            carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            Log.d("LED", "CarPropertyManager initialized")
        }

        // Set up light button
        val lightButton = findViewById<LinearLayout>(R.id.light_button)
        lightButton.setOnClickListener {
            ledState = !ledState
            //setLedState(ledState)
            updateLightIcon(ledState)
        }

        // Hide system navigation bar
        hideSystemBars()

        // Initialize time updater
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateTime()
                timeUpdateHandler.postDelayed(this, 60000) // Update every minute
            }
        }

        // Load initial fragments (Home screen)
        if (savedInstanceState == null) {
            showHomeFragments()
        } else {
            // Restore screen state
            val savedScreen = savedInstanceState.getString("currentScreen")
            if (savedScreen != null) {
                currentScreen = Screen.valueOf(savedScreen)
                when (currentScreen) {
                    Screen.HOME -> showHomeFragments()
                    Screen.CAR_VITALS -> showCarVitalsFragments()
                    Screen.SETTINGS -> showSettingsFragment()
                }
                updateIconStates()
            } else {
                showHomeFragments()
            }
        }

        // Set click listeners for navigation
        homeIcon.setOnClickListener {
            if (currentScreen != Screen.HOME) {
                showHomeFragments()
                updateIconStates()
            }
        }

        carVitalsIcon.setOnClickListener {
            if (currentScreen != Screen.CAR_VITALS) {
                showCarVitalsFragments()
                updateIconStates()
            }
        }

        settingsIcon.setOnClickListener {
            if (currentScreen != Screen.SETTINGS) {
                showSettingsFragment()
                updateIconStates()
            }
        }

        // Update time and schedule updates
        updateTime()
        timeUpdateHandler.postDelayed(timeUpdateRunnable, 60000)

        // Request CAMERA permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
        } else {
            startCameraService()
        }
    }

    private fun showHomeFragments() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navigation_container, NavigationFragment())
            .replace(R.id.dashboard_container, DashboardFragment())
            .commit()
        currentScreen = Screen.HOME

        findViewById<FrameLayout>(R.id.batteryContainer)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.mainFragmentContainer)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.rightFragmentContainer)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.settingsFragmentContainer)?.visibility = View.GONE

        findViewById<FrameLayout>(R.id.navigation_container)?.visibility = View.VISIBLE
        findViewById<FrameLayout>(R.id.dashboard_container)?.visibility = View.VISIBLE
    }

    private fun showCarVitalsFragments() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.batteryContainer, BatteryFragment())
            .replace(R.id.mainFragmentContainer, CarVitalsFragment())
            .replace(R.id.rightFragmentContainer, SeatFragment())
            .commit()
        currentScreen = Screen.CAR_VITALS

        findViewById<FrameLayout>(R.id.navigation_container)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.dashboard_container)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.settingsFragmentContainer)?.visibility = View.GONE

        findViewById<FrameLayout>(R.id.batteryContainer)?.visibility = View.VISIBLE
        findViewById<FrameLayout>(R.id.mainFragmentContainer)?.visibility = View.VISIBLE
        findViewById<FrameLayout>(R.id.rightFragmentContainer)?.visibility = View.VISIBLE
    }

    private fun showSettingsFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.settingsFragmentContainer, SettingsFragment())
            .commit()
        currentScreen = Screen.SETTINGS

        findViewById<FrameLayout>(R.id.navigation_container)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.dashboard_container)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.batteryContainer)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.mainFragmentContainer)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.rightFragmentContainer)?.visibility = View.GONE

        findViewById<FrameLayout>(R.id.settingsFragmentContainer)?.visibility = View.VISIBLE
    }

    private fun updateIconStates() {
        homeIcon.setBackgroundResource(
            if (currentScreen == Screen.HOME) R.drawable.filled_ellipse else R.drawable.icon_background
        )
        carVitalsIcon.setBackgroundResource(
            if (currentScreen == Screen.CAR_VITALS) R.drawable.filled_ellipse else R.drawable.icon_background
        )
        settingsIcon.setBackgroundResource(
            if (currentScreen == Screen.SETTINGS) R.drawable.filled_ellipse else R.drawable.icon_background
        )
    }

    private fun startCameraService() {
        val intent = Intent(this, CameraCaptureService::class.java)
        startService(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCameraService()
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

    private fun updateTime() {
        try {
            val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            Log.d("TimeUpdate", "Current time: $currentTime")
            timeTextView.text = currentTime
        } catch (e: Exception) {
            Log.e("TimeUpdate", "Error updating time", e)
        }
    }

    private fun setLedState(state: Boolean) {
        val value = if (state) 1 else 0
        try {
            synchronized(carPropertyManager) {
                carPropertyManager.setProperty(
                    Integer::class.java,
                    VENDOR_EXTENSION_LIGHT_CONTROL_PROPERTY,
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
        lightIcon.setImageResource(if (state) R.drawable.light_on else R.drawable.light_off)
    }

    override fun onDestroy() {
        super.onDestroy()
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && packageManager.hasSystemFeature("android.hardware.type.automotive")) {
            hideSystemBars()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentScreen", currentScreen.name)
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
                updateLightIcon(true)
                playAudio(R.raw.lighton)
            }
            "light_off" -> {
                updateLightIcon(false)
                playAudio(R.raw.lightoff)
            }
            "day_mode" -> {
                // change to light mode
                playAudio(R.raw.day)
            }
            "night_mode" -> {
                // change to dark mode
                playAudio(R.raw.night)
            }
            "seat_45" -> {
                // adjust seat position 3
                playAudio(R.raw.seat)
            }
            "seat_70" -> {
                // adjust seat position 2
                playAudio(R.raw.seatdefault)
            }
            "seat_90" -> {
                // adjust seat position 1
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

    fun toggleService() {
        isServiceRunning = !isServiceRunning  // Update companion object variable

        Intent(this, VoskRecognitionService::class.java).apply {
            action = if (isServiceRunning) {
                VoskRecognitionService.ACTION_START_RECOGNITION
            } else {
                VoskRecognitionService.ACTION_STOP_RECOGNITION
            }
            startService(this)
        }

        // Notify SettingsFragment about state change
        supportFragmentManager.fragments.forEach {
            if (it is SettingsFragment) it.updateVoiceImage()
        }
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
                MainActivity.Companion.PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        }
    }

    override fun onCommandReceived(command: String) {
        if (command == lastCommand) return
        lastCommand = command
        runOnUiThread { handleCommand(command) }
    }
}