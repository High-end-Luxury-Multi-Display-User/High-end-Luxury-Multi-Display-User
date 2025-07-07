package com.example.driverlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ImageReader
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.driverlauncher.carvitals.BatteryFragment
import com.example.driverlauncher.carvitals.CarVitalsFragment
import com.example.driverlauncher.carvitals.SeatFragment
import com.example.driverlauncher.handgesture.YuvToRgbConverter
import com.example.driverlauncher.home.DashboardFragment
import com.example.driverlauncher.home.NavigationFragment
import com.example.driverlauncher.ml.ModelMetadata
import com.example.driverlauncher.settings.SettingsFragment
import com.example.driverlauncher.voskva.VoskRecognitionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), VoskRecognitionService.RecognitionCallback {
    companion object {
        const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
        var isServiceRunning = false
        var voskService: VoskRecognitionService? = null
        var isBound = false
        var lastCommand = ""
    }
//    private val VENDOR_EXTENSION_LIGHT_CONTROL_PROPERTY: Int = 0x21400106
//    private val areaID = 0
//    private lateinit var car: Car
//    private lateinit var carPropertyManager: CarPropertyManager
    private var ledState = false // false = off, true = on
    private lateinit var lightIcon: ImageView
    private lateinit var timeTextView: TextView
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable
    private lateinit var homeIcon: ImageView
    private lateinit var carVitalsIcon: ImageView
    private lateinit var settingsIcon: ImageView
    private lateinit var model: ModelMetadata
    private lateinit var imageProcessor: ImageProcessor
    private val CAMERA_PERMISSION_CODE = 100
    private lateinit var imageReader: ImageReader
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var audioManager: AudioManager
    private var lastGestureTime = 0L
    private val gestureDebounceTime = 500L // 1 second debounce
    private var isDarkTheme = true

    private var currentScreen = Screen.HOME // Track current screen state
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
        // Initialize AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

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
//        car = Car.createCar(this.applicationContext)
//        if (car == null) {
//            Log.e("LED", "Failed to create Car instance")
//        } else {
//            carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
//            Log.d("LED", "CarPropertyManager initialized")
//        }

        // Set up light button
        val lightButton = findViewById<LinearLayout>(R.id.light_button)
        lightButton.setOnClickListener {
            ledState = !ledState
            setLedState(ledState)
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

        // Camera Model
//        model = ModelMetadata.newInstance(this)
//        imageProcessor = ImageProcessor.Builder()
//            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
//            .build()
//        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            setupCamera()
        }
    }

    private fun setupCamera() {
        startBackgroundThread()
        setupImageReader()
        openCamera()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackgroundThread")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(640, 480, android.graphics.ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val bitmap = YuvToRgbConverter.yuvToRgb(this, image)
            image.close()

            scope.launch {
                try {
                    val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
                    val outputs = model.process(tensorImage)
                    val detectionResult = outputs.probabilityAsCategoryList

                    if (detectionResult.isNotEmpty()) {
                        val bestResult = detectionResult.maxByOrNull { it.score }
                        bestResult?.let {
                            if (it.score > 0.8f) {
                                Log.i("Gesture", "Label: ${it.label}, DisplayName: ${it.displayName}, Score: ${it.score}")
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastGestureTime > gestureDebounceTime) {
                                    when (it.label) {
                                        "scrollup" -> {
                                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                                            runOnUiThread {
                                                Toast.makeText(this@MainActivity, "Volume Up", Toast.LENGTH_SHORT).show()
                                            }
                                            lastGestureTime = currentTime
                                        }
                                        "down" -> {
                                            audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                                            runOnUiThread {
                                                Toast.makeText(this@MainActivity, "Volume Down", Toast.LENGTH_SHORT).show()
                                            }
                                            lastGestureTime = currentTime
                                        }
                                        "up" -> {
                                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                                            runOnUiThread {
                                                Toast.makeText(this@MainActivity, "Volume Up", Toast.LENGTH_SHORT).show()
                                            }
                                            lastGestureTime = currentTime
                                        }

                                        else -> {}
                                    }
                                } else {

                                }
                            } else {
                                Log.i("Gesture", "No gesture confident enough (max score: ${it.score})")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Gesture", "Frame processing error", e)
                }
            }
        }, backgroundHandler)
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.first { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surface = imageReader.surface
                val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                requestBuilder.addTarget(surface)

                camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                        Log.d("CameraDebug", "Capture session started")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("CameraDebug", "Configuration failed")
                    }
                }, backgroundHandler)
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("CameraDebug", "Camera error: $error")
                camera.close()
                cameraDevice = null
            }
        }, backgroundHandler)
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
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsFragmentContainer, SettingsFragment())
                .commitNow() // Use commitNow to ensure synchronous commit
            currentScreen = Screen.SETTINGS

            // Update visibility of containers
            findViewById<FrameLayout>(R.id.navigation_container)?.visibility = View.GONE
            findViewById<FrameLayout>(R.id.dashboard_container)?.visibility = View.GONE
            findViewById<FrameLayout>(R.id.batteryContainer)?.visibility = View.GONE
            findViewById<FrameLayout>(R.id.mainFragmentContainer)?.visibility = View.GONE
            findViewById<FrameLayout>(R.id.rightFragmentContainer)?.visibility = View.GONE
            findViewById<FrameLayout>(R.id.settingsFragmentContainer)?.visibility = View.VISIBLE
        } catch (e: IllegalStateException) {
            Log.e("MainActivity", "Fragment transaction failed: ${e.message}", e)
            Toast.makeText(this, "Failed to load settings screen", Toast.LENGTH_SHORT).show()
        }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupCamera()
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
//            synchronized(carPropertyManager) {
//                carPropertyManager.setProperty(
//                    Integer::class.java,
//                    VENDOR_EXTENSION_LIGHT_CONTROL_PROPERTY,
//                    areaID,
//                    Integer(value)
//                )
//                Log.d("LED", "LED state set to: $value")
//            }
        } catch (e: Exception) {
            Log.e("LED", "Failed to set LED state", e)
        }
        finally {
            ledState = state
        }
    }

    private fun updateLightIcon(state: Boolean) {
        lightIcon.setImageResource(if (state) R.drawable.light_on else R.drawable.light_off)
    }

    override fun onDestroy() {
        super.onDestroy()
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
        scope.cancel()
        model.close()
        imageReader.close()
        cameraDevice?.close()
        captureSession?.close()
        backgroundThread.quitSafely()
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
            "arise_avalon" -> {
                Toast.makeText(this, "Hello! How can I assist you?", Toast.LENGTH_SHORT).show()
                playAudio(R.raw.greet)
            }
            "light_on" -> {
                updateLightIcon(true)
                setLedState(true)
                playAudio(R.raw.lighton)
            }
            "light_off" -> {
                updateLightIcon(false)
                setLedState(false)
                playAudio(R.raw.lightoff)
            }
            "seat_front" -> {
                // adjust seat position 3
                adjustSeatPosition(SeatFragment.SeatPosition.REVERSE, R.raw.seatmax)
            }
            "seat_neutral" -> {
                // adjust seat position 2
                adjustSeatPosition(SeatFragment.SeatPosition.DEFAULT, R.raw.seatdefault)
            }
            "seat_backward" -> {
                // adjust seat position 1
                adjustSeatPosition(SeatFragment.SeatPosition.FORWARD, R.raw.seat)
            }
        }
    }

    private fun adjustSeatPosition(position: SeatFragment.SeatPosition, soundRes: Int) {
        if (currentScreen != Screen.CAR_VITALS) {
            showCarVitalsFragments()
            updateIconStates()
            // Wait for fragment to be created
            Handler(Looper.getMainLooper()).post {
                getSeatFragment()?.switchTo(position)
            }
        } else {
            // Fragment already exists - update immediately
            getSeatFragment()?.switchTo(position)
        }
        playAudio(soundRes)
    }

    private fun getSeatFragment(): SeatFragment? {
        return supportFragmentManager.findFragmentById(R.id.rightFragmentContainer) as? SeatFragment
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

    private fun startServiceOnlyOnce() {
        startService(Intent(this, VoskRecognitionService::class.java))
        isServiceRunning = true
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
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        }
    }

    override fun onCommandReceived(command: String) {
        if (command == lastCommand) return
        lastCommand = command
        runOnUiThread { handleCommand(command) }
    }
}