package android.vendor.camerawithservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat

class CameraCaptureService : Service() {
    private val TAG = "CameraService"
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var backgroundHandler: Handler

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created ✅")
        startBackgroundThread()
        openCamera()
    }

    private fun startBackgroundThread() {
        val handlerThread = HandlerThread("CameraBackground")
        handlerThread.start()

        backgroundHandler = Handler(handlerThread.looper)
    }

    private fun openCamera() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        Log.d(TAG, "Attempting to open camera...")

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) return

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                cameraDevice = device
                startCapture()
            }

            override fun onDisconnected(device: CameraDevice) {
                device.close()
            }

            override fun onError(device: CameraDevice, error: Int) {
                device.close()
            }
        }, backgroundHandler)
    }

    private fun startCapture() {
        val imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2)
        val surface = imageReader.surface
        Log.d(TAG, "Camera opened. Starting capture session...")
        val requestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        requestBuilder.addTarget(surface)

        cameraDevice!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }, backgroundHandler)

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.close() // Just releasing the buffer for now
        }, backgroundHandler)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed ❌")
        captureSession?.close()
        cameraDevice?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
