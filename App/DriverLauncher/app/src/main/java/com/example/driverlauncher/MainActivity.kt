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
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
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
import com.example.driverlauncher.drawsiness.EyeDetectionService
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.lang.reflect.Method

class MainActivity : AppCompatActivity(), VoskRecognitionService.RecognitionCallback ,EyeDetectionService.DetectionCallback{
    companion object {
        const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
        var isServiceRunning = false
        var isGestureEnable = false
        var voskService: VoskRecognitionService? = null
        var isBound = false
        var lastCommand = ""
        private const val TAG = "MainActivity"
        private const val PERMISSIONS_REQUEST_CAMERA = 100
    }

    private var isModelClosed = false // Track model state

    private var isDestroyed = false
    private var eyeDetectionService: EyeDetectionService? = null
     var isBoundEye = true
     var isEyeDetectionEnabled = false
    private val VENDOR_EXTENSION_LIGHT_CONTROL_PROPERTY: Int = 0x21400106
    private val areaID = 0
    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager
    private var ledState = false // false = off, true = on
    private lateinit var lightIcon: ImageView
    private lateinit var timeTextView: TextView
    private lateinit var drowsinessStatusIcon: ImageView
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable
    private lateinit var homeIcon: ImageView
    private lateinit var carVitalsIcon: ImageView
    private lateinit var settingsIcon: ImageView

    // Camera gesture model
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
    private var gestureSequenceCount = 0
    private var lastDetectedGesture: String? = null
    private val requiredSequentialFrames = 2
    private var lastGestureTime = 0L
    private val iconRevertDelay = 3000L
    private val gestureDebounceTime = 1000L
    private lateinit var ic_volume:ImageView

    private var currentScreen = Screen.HOME // Track current screen state
    enum class Screen {
        HOME, CAR_VITALS, SETTINGS
    }
    private val serviceConnectionEye = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Eye Service connected")
            val binder = service as EyeDetectionService.LocalBinder
            eyeDetectionService = binder.getService().apply {
                setDetectionCallback(this@MainActivity)
            }

            isBoundEye = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Eye Service disconnected")
            isBoundEye = false
            eyeDetectionService = null
            isEyeDetectionEnabled = false

        }
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

        Log.w("ExecCommand","test")
        /*********************************************/
        // VA shenanigans
       // checkPermissions()
       // startServiceOnlyOnce()
        /********************************************/
        // Initialize AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // Drawsiness Icon
        drowsinessStatusIcon = findViewById(R.id.drowsinessStatusIcon)
        drowsinessStatusIcon.setImageResource(R.drawable.eye_disabled)
        // Initialize views
        lightIcon = findViewById(R.id.light_icon)
        ic_volume = findViewById(R.id.ic_volume)
        timeTextView = findViewById(R.id.time) ?: run {
            Log.e("TimeUpdate", "Time TextView not found!")
            return
        }
        homeIcon = findViewById(R.id.icon_home)
        carVitalsIcon = findViewById(R.id.icon_car_vitals)
        settingsIcon = findViewById(R.id.icon_settings)

       //  Initialize Car API
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

        /********************** Camera Gesture Model ************************/
        model = ModelMetadata.newInstance(this)
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        checkCameraPermission()

        /***************Drawsiness**************/
        startServiceOnlyOnce()

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting camera permission")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
        } else {
            Log.d(TAG, "Camera permission already granted")
            bindService()
        }

        /******************UserPicker&UserName**********/
        val userContainer = findViewById<LinearLayout>(R.id.driver_profile)
            ?: throw IllegalStateException("Missing user_container")
        userContainer.setOnClickListener {
            startUserPickerActivity()
        }
        val userIcon = findViewById<ImageView>(R.id.user_profile)!!
        userIcon?.setOnClickListener {
            startUserPickerActivity()
        }
        updateProfileName()
    }

    private fun updateProfileName() {
        val profileName = findViewById<TextView>(R.id.profile_name)
        if (profileName == null) {
            Log.e("ProfileName", "Profile name TextView not found")
            return
        }

        try {
            // Get the UserManager service
            val userManager = getSystemService(Context.USER_SERVICE)
            if (userManager == null) {
                Log.e("ProfileName", "UserManager service is null")
                runOnUiThread {
                    profileName.text = "Unknown User"
                    Toast.makeText(this, "User service unavailable", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // Use reflection to invoke UserManager.getUserName()
            val userManagerClass = userManager.javaClass
            val getUserNameMethod = userManagerClass.getMethod("getUserName")
            val userName = getUserNameMethod.invoke(userManager) as? String

            runOnUiThread {
                profileName.text = userName ?: "Unknown User"
                Log.d("ProfileName", "User name set to: ${userName ?: "Unknown User"}")
            }
        } catch (e: NoSuchMethodException) {
            Log.e("ProfileName", "getUserName method not found", e)
            runOnUiThread {
                profileName.text = "Unknown User"
                Toast.makeText(this, "Cannot retrieve user name: Method unavailable", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Log.e("ProfileName", "Permission denied for accessing user name", e)
            runOnUiThread {
                profileName.text = "Unknown User"
                Toast.makeText(this, "Permission denied for user name", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ProfileName", "Error accessing user name", e)
            runOnUiThread {
                profileName.text = "Unknown User"
                Toast.makeText(this, "Error retrieving user name: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startUserPickerActivity() {
        try {
            // Create an Intent without directly referencing the class
            val driverIntent = Intent().apply {
                setClassName("com.android.systemui", "com.android.systemui.car.userpicker.UserPickerActivity")
            }

            // Verify if the activity can be resolved
            val packageManager = packageManager
            if (driverIntent.resolveActivity(packageManager) != null) {
                startActivity(driverIntent)
                Log.d("UserPicker", "Successfully started UserPickerActivity")
            } else {
                Log.e("UserPicker", "UserPickerActivity not found or cannot be resolved")
                runOnUiThread {
                    Toast.makeText(this, "Cannot open user picker: Activity not found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Log.e("UserPicker", "Security exception while starting UserPickerActivity", e)
            runOnUiThread {
                Toast.makeText(this, "Permission denied to open user picker", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("UserPicker", "Failed to start UserPickerActivity", e)
            runOnUiThread {
                Toast.makeText(this, "Error opening user picker: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Volume UP and Down using shell commands
    private fun executeShellCommand(command: String): Pair<Boolean, String> {
        try {
            // Use reflection to invoke Runtime.getRuntime().exec
            val runtimeClass = Class.forName("java.lang.Runtime")
            val getRuntimeMethod: Method = runtimeClass.getDeclaredMethod("getRuntime")
            val runtime: Any = getRuntimeMethod.invoke(null) // Get Runtime instance
            val execMethod: Method = runtimeClass.getDeclaredMethod("exec", String::class.java)
            val process: Process = execMethod.invoke(runtime, command) as Process

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Log.i("ExecCommand", "Executed shell command via reflection: $command, output: $output")
                return Pair(true, output)
            } else {
                Log.e("ExecCommand", "Shell command failed: $command, error: $error, exit code: $exitCode")
                return Pair(false, error)
            }
        } catch (e: Exception) {
            Log.e("ExecCommand", "Failed to execute shell command via reflection: ${e.message}", e)
            return Pair(false, e.message ?: "Unknown error")
        }
    }
    private fun executeCommandViaReflection(command: String): Boolean {
        try {
            val runtimeClass = Class.forName("java.lang.Runtime")
            val getRuntimeMethod: Method = runtimeClass.getDeclaredMethod("getRuntime")
            val runtime: Any = getRuntimeMethod.invoke(null) // Get Runtime instance
            val execMethod: Method = runtimeClass.getDeclaredMethod("exec", String::class.java)
            val process: Process = execMethod.invoke(runtime, command) as Process
            val exitCode = process.waitFor()
            Log.d("Reflection", "Command executed: $command, Exit code: $exitCode")
            return exitCode == 0
        } catch (e: Exception) {
            Log.e("Reflection", "Failed to execute command via reflection: ${e.message}", e)
            return false
        }
    }
    
    private fun bindService() {
        if (isDestroyed) return
        Log.d(TAG, "Binding to service")
        bindService(Intent(this, EyeDetectionService::class.java), serviceConnectionEye, Context.BIND_AUTO_CREATE)
    }

    fun toggleEyeService(): Boolean {
        val intent = Intent(this, EyeDetectionService::class.java).apply {
            action = if (isEyeDetectionEnabled) {
                EyeDetectionService.ACTION_STOP_DETECTION
            } else {
                EyeDetectionService.ACTION_START_DETECTION
            }
        }
        startService(intent)

        // Only update callback if service is bound
        if (isBoundEye && eyeDetectionService != null) {
            if (isEyeDetectionEnabled) {
                eyeDetectionService?.setDetectionCallback(null)
            } else {
                eyeDetectionService?.setDetectionCallback(this)
            }
        } else {
            Log.w(TAG, "toggleEyeService: EyeDetectionService not yet bound")
        }

        isEyeDetectionEnabled = !isEyeDetectionEnabled

        // Update dashboard icon immediately
        runOnUiThread {
            drowsinessStatusIcon.setImageResource(
                if (isEyeDetectionEnabled) R.drawable.eye_enabled else R.drawable.eye_disabled
            )
        }

        return isEyeDetectionEnabled
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
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackgroundThread")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(224, 224, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage() ?: return@setOnImageAvailableListener
            try {
                val bitmap = YuvToRgbConverter.yuvToRgb(this, image) ?: return@setOnImageAvailableListener
                scope.launch {
                    try {
                        synchronized(model) {
                            if (!isModelClosed) {
                                val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
                                val outputs = model.process(tensorImage)
                                val detectionResult = outputs.probabilityAsCategoryList
                                if (detectionResult.isNotEmpty()) {
                                    val bestResult = detectionResult.maxByOrNull { it.score }
                                    bestResult?.let {
                                        if (it.score > 0.8f) {
                                            Log.i("Gesture", "Label: ${it.label}, Score: ${it.score}")
                                            val currentTime = System.currentTimeMillis()
                                            if (it.label == lastDetectedGesture) {
                                                gestureSequenceCount++
                                            } else {
                                                lastDetectedGesture = it.label
                                                gestureSequenceCount = 1
                                            }
                                            if (gestureSequenceCount >= requiredSequentialFrames &&
                                                currentTime - lastGestureTime > gestureDebounceTime
                                            ) {
                                                when (it.label) {
                                                    "scrollup" -> {
                                                        audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                                                        runOnUiThread {
                                                            Toast.makeText(this@MainActivity, "Mute", Toast.LENGTH_SHORT).show()
                                                            ic_volume.setImageResource(R.drawable.ic_mute)
                                                            updateVolumeIcon()
                                                        }
                                                        lastGestureTime = currentTime
                                                        resetGestureTracking()
                                                    }
                                                    "down" -> {
                                                        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                                                        runOnUiThread {
                                                            Toast.makeText(this@MainActivity, "Volume Down", Toast.LENGTH_SHORT).show()
                                                            ic_volume.setImageResource(R.drawable.ic_decrease)
                                                            timeUpdateHandler.postDelayed({
                                                                updateVolumeIcon()
                                                            }, iconRevertDelay)
                                                        }
                                                        lastGestureTime = currentTime
                                                        resetGestureTracking()
                                                    }
                                                    "up" -> {
                                                        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                                                        runOnUiThread {
                                                            Toast.makeText(this@MainActivity, "Volume Up", Toast.LENGTH_SHORT).show()
                                                            ic_volume.setImageResource(R.drawable.ic_increase)
                                                            timeUpdateHandler.postDelayed({
                                                                updateVolumeIcon()
                                                            }, iconRevertDelay)
                                                        }
                                                        lastGestureTime = currentTime
                                                        resetGestureTracking()
                                                    }
                                                    else -> {
//                                                        runOnUiThread { updateVolumeIcon() }
                                                    }
                                                }
                                            } else {
//                                                runOnUiThread { updateVolumeIcon() }
                                            }
                                        } else {
                                            Log.i("Gesture", "No gesture confident enough (max score: ${it.score})")
                                            runOnUiThread { updateVolumeIcon() }
                                            resetGestureTracking()
                                        }
                                    }
                                } else {
//                                    runOnUiThread { updateVolumeIcon() }
                                    resetGestureTracking()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Gesture", "Frame processing error", e)
                        resetGestureTracking()
                    }
                }
            } catch (e: Exception) {
                Log.e("Gesture", "Image processing error", e)
            } finally {
                image.close()
            }
        }, backgroundHandler)
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isEmpty()) {
                Log.e("CameraDebug", "No cameras found in cameraIdList")
//                handleNoCameraAvailable("No cameras available")
                return
            }
            Log.d("CameraDebug", "Available cameras: ${cameraIds.joinToString()}")
            cameraIds.forEach { id ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val facingString = when (facing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "Front-facing"
                        CameraCharacteristics.LENS_FACING_BACK -> "Rear-facing"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                        else -> "Unknown ($facing)"
                    }
                    Log.d("CameraDebug", "Camera ID: $id, Facing: $facingString")
                } catch (e: CameraAccessException) {
                    Log.e("CameraDebug", "Failed to get characteristics for camera ID: $id", e)
                }
            }
        } catch (e: CameraAccessException) {
            Log.e("CameraDebug", "Failed to list cameras: ${e.reason}", e)
//            handleNoCameraAvailable("Camera access denied: ${e.reason}")
            return
        } catch (e: Exception) {
            Log.e("CameraDebug", "Unexpected error listing cameras", e)
//            handleNoCameraAvailable("Unexpected error listing cameras")
            return
        }

        val cameraId = try {
            val rearCamera = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
            Log.d("CameraDebug", "Rear-facing camera: ${rearCamera ?: "none"}")
            rearCamera ?: cameraManager.cameraIdList.firstOrNull().also {
                Log.d("CameraDebug", "Falling back to camera: $it")
            }
        } catch (e: Exception) {
            Log.e("CameraDebug", "Failed to select camera", e)
//            handleNoCameraAvailable("Failed to select camera")
            return
        }

        if (cameraId == null) {
            Log.e("CameraDebug", "No cameras available or accessible")
//            handleNoCameraAvailable("No cameras available")
            return
        }

        try {
            Log.d("CameraDebug", "Attempting to open camera ID: $cameraId")
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    Log.d("CameraDebug", "Camera $cameraId opened successfully")
                    val surface = imageReader?.surface ?: run {
                        Log.e("CameraDebug", "ImageReader surface is null")
//                        handleNoCameraAvailable("ImageReader surface unavailable")
                        return
                    }
                    val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    requestBuilder.addTarget(surface)
                    camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            try {
                                session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                                Log.d("CameraDebug", "Capture session started for camera ID: $cameraId")
                            } catch (e: Exception) {
                                Log.e("CameraDebug", "Failed to start repeating request", e)
//                                handleNoCameraAvailable("Failed to start capture session")
                            }
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e("CameraDebug", "Capture session configuration failed for camera ID: $cameraId")
//                            handleNoCameraAvailable("Capture session configuration failed")
                        }
                    }, backgroundHandler)
                }
                override fun onDisconnected(camera: CameraDevice) {
                    Log.w("CameraDebug", "Camera disconnected: $cameraId")
                    camera.close()
                    cameraDevice = null
//                    handleNoCameraAvailable("Camera disconnected")
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    val errorMsg = when (error) {
                        CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "Camera in use"
                        CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "Max cameras in use"
                        CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "Camera disabled"
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "Camera device error"
                        CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "Camera service error"
                        else -> "Unknown error ($error)"
                    }
                    Log.e("CameraDebug", "Camera error for ID $cameraId: $errorMsg")
                    camera.close()
                    cameraDevice = null
//                    handleNoCameraAvailable("Camera error: $errorMsg")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e("CameraDebug", "Failed to open camera ID: $cameraId, reason: ${e.reason}", e)
//            handleNoCameraAvailable("Camera access denied: ${e.reason}")
        } catch (e: Exception) {
            Log.e("CameraDebug", "Unexpected error opening camera ID: $cameraId", e)
//            handleNoCameraAvailable("Unexpected error opening camera")
        }
    }

    fun toggleGesture() {
        isGestureEnable = !isGestureEnable
        Log.d(TAG, "Toggling gesture detection: isGestureEnable=$isGestureEnable")

        if (isGestureEnable) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                setupCamera()
                Log.d(TAG, "Gesture detection enabled")
            } else {
                isGestureEnable = false // Revert if permission is not granted
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
                Toast.makeText(this, "Camera permission required for gesture detection", Toast.LENGTH_SHORT).show()
            }
        } else {
            stopGestureDetection()
            Log.d(TAG, "Gesture detection disabled")
        }

        // Update SettingsFragment UI
        supportFragmentManager.fragments.forEach {
            if (it is SettingsFragment) {
                it.updateGestureImage()
            }
        }
    }

    private fun setupCamera() {
        try {
            startBackgroundThread()
            setupImageReader()
            openCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set up camera: ${e.message}", e)
            isGestureEnable = false
            stopGestureDetection()
            Toast.makeText(this, "Failed to enable gesture detection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopGestureDetection() {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
            captureSession = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture session: ${e.message}", e)
        }
        try {
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera device: ${e.message}", e)
        }
        try {
            imageReader.setOnImageAvailableListener(null, null)
            imageReader.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing image reader: ${e.message}", e)
        }
        try {
            backgroundThread.quitSafely()
            backgroundHandler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error quitting background thread: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGestureDetection()
        try {
            scope.cancel()
            isModelClosed = true
            model.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing model: ${e.message}", e)
        }
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
    }
    // Helper function to update volume icon based on current audio state
    private fun updateVolumeIcon() {
        val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC) ||
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        ic_volume.setImageResource(if (isMuted) R.drawable.ic_mute else R.drawable.ic_volume)
    }

    // Helper function to reset gesture tracking
    private fun resetGestureTracking() {
        gestureSequenceCount = 0
        lastDetectedGesture = null
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
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted")
                if (isGestureEnable) {
                    setupCamera()
                }
            } else {
                Log.w(TAG, "Camera permission denied")
                isGestureEnable = false
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show()
                // Update SettingsFragment UI
                supportFragmentManager.fragments.forEach {
                    if (it is SettingsFragment) {
                        it.updateGestureImage()
                    }
                }
            }
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

            // Auto Theme Switching
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDayTime = hour in 6..17  // 6 AM to 5:59 PM

            val overlayShouldBeEnabled = isDayTime // Day → enable overlay

            val settingsFragment = SettingsFragment()
            val overlayCurrentlyEnabled = settingsFragment.isOverlayEnabled()

            if (overlayCurrentlyEnabled != overlayShouldBeEnabled) {
                Log.i("ThemeAutoSwitch", "Switching theme. DayTime=$isDayTime")
                settingsFragment.setOverlayTheme(overlayShouldBeEnabled)
            }

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
        finally {
            ledState = state
        }
    }

    private fun updateLightIcon(state: Boolean) {
        lightIcon.setImageResource(if (state) R.drawable.light_on else R.drawable.light_off)
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            bindService()
        }
        bindService(
            Intent(this, VoskRecognitionService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onPause() {
        super.onPause()
        if (isBoundEye) {
            eyeDetectionService?.setDetectionCallback(null)
            unbindService(serviceConnectionEye)
            isBoundEye = false
        }
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
        Log.d(TAG, "Starting service -- Vosk and Drawsiness")
        startService(Intent(this, EyeDetectionService::class.java))
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
    
    override fun onResultReceived(status: String) {
        if (isDestroyed) return
        Log.d(TAG, "Result received: $status")

        runOnUiThread {
            if (isEyeDetectionEnabled) {
                when (status.lowercase(Locale.ROOT)) {
                    "open" -> drowsinessStatusIcon.setImageResource(R.drawable.ic_drowsiness)
                    "closed" -> drowsinessStatusIcon.setImageResource(R.drawable.eye_closed)
                    else -> drowsinessStatusIcon.setImageResource(R.drawable.eye_enabled)
                }
            } else {
                drowsinessStatusIcon.setImageResource(R.drawable.eye_disabled)
            }
        }
    }

    override fun onErrorReceived(error: String) {
        Log.e(TAG, "Error received: $error")
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()

        }
    }

    override fun onStatusReceived(isEnabled: Boolean) {
        if (isDestroyed) return
        Log.d(TAG, "Status received: isEnabled=$isEnabled")
        isEyeDetectionEnabled = isEnabled

        // Make sure the activity is still alive
        if (!isFinishing && !isDestroyed) {
            runOnUiThread {
                // Safely try to find the icon and update it
                val eyeIcon = findViewById<ImageView?>(R.id.drowsinessStatusIcon)
                eyeIcon?.setImageResource(
                    if (isEnabled) R.drawable.eye_enabled else R.drawable.eye_disabled
                )
            }
        }

        // Also update the icon in SettingsFragment if it's currently shown
        val settingsFragment = supportFragmentManager.findFragmentById(R.id.settingsFragmentContainer)
        if (settingsFragment is SettingsFragment) {
            settingsFragment.updateDrowsinessIcon(isEnabled)
        }
    }


}
