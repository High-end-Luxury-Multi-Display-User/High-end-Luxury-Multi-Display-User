package com.example.driverlauncher.voskva

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.driverlauncher.R
import org.json.JSONException
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class VoskRecognitionService : Service(), RecognitionListener {

    companion object {
        const val TAG = "VoskService"
        const val NOTIFICATION_ID = 1234
        const val CHANNEL_ID = "vosk_channel"
        const val ACTION_START_RECOGNITION = "org.vosk.demo.ACTION_START"
        const val ACTION_STOP_RECOGNITION = "org.vosk.demo.ACTION_STOP"
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isRunning = false
    private var callback: RecognitionCallback? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): VoskRecognitionService = this@VoskRecognitionService
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun setRecognitionCallback(callback: RecognitionCallback) {
        this.callback = callback
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.i("TAG", "onCreate: Vosk Service Started With Notification")
        initModel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECOGNITION -> startListening()
            ACTION_STOP_RECOGNITION -> stopListening()
        }
        return START_STICKY
    }

    private fun initModel() {
        if (model != null) {
            startListening()
            return
        }

        StorageService.unpack(this, "vosk-model-small-en-us-0.15", "model",
            { unpackedModel ->
                model = unpackedModel
                startListening()
            },
            { exception ->
                Log.e(TAG, "Model unpack failed", exception)
                stopSelf()
            })

        Log.i("TAG", "initModel: Vosk Initialized Successfully")
    }

    private fun startListening() {
        if (isRunning || model == null) return

        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
            isRunning = true
            updateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recognition", e)
        }
    }

    private fun stopListening() {
        if (!isRunning) return

        speechService?.apply {
            stop()
            shutdown()
        }
        speechService = null
        isRunning = false
        updateNotification()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Recognition")
            .setContentText(if (isRunning) "Listening..." else "Idle")
            .setSmallIcon(R.drawable.voice)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vosk Voice Recognition",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoskService::WakeLock")
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)
    }

    override fun onDestroy() {
        stopListening()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    override fun onResult(hypothesis: String) {
        try {
            val json = JSONObject(hypothesis)
            var text = json.getString("text")
                .lowercase()
                .trim()
                .replace("\\s+".toRegex(), " ")

            Log.d(TAG, "Recognized: $text")

            val command = when {
                matchesPattern(text, greetSynonyms, carSynonyms) -> "greet"
                matchesPattern(text, seatSynonyms, "default") -> "seat_70"
                matchesPattern(text, seatSynonyms, maxSynonyms) -> "seat_90"
                matchesPattern(text, seatSynonyms, minSynonyms) -> "seat_45"
                matchesPattern(text, lightSynonyms, "on") -> "light_on"
                matchesPattern(text, lightSynonyms, offSynonyms) -> "light_off"
                matchesPattern(text, daySynonyms, modeSynonyms) -> "day_mode"
                matchesPattern(text, nightSynonyms, modeSynonyms) -> "night_mode"
                else -> null
            }

            command?.let {
                callback?.onCommandReceived(it)
            } ?: Log.d(TAG, "Unrecognized command: $text")

        } catch (e: JSONException) {
            Log.e(TAG, "JSON parsing error", e)
        }
    }

    private fun matchesPattern(text: String, group1: Array<String>, group2: Any): Boolean {
        val pattern1 = group1.joinToString("|")
        val pattern2 = when (group2) {
            is Array<*> -> (group2 as Array<String>).joinToString("|")
            is String -> group2
            else -> return false
        }
        return text.matches(".*\\b($pattern1)\\s+($pattern2)\\b.*".toRegex())
    }

    // Command synonym lists
    private val greetSynonyms = arrayOf("hi", "hey", "high", "hello", "hail")
    private val carSynonyms = arrayOf("car", "call", "court", "assistant", "vehicle", "system", "core")
    private val lightSynonyms = arrayOf("light", "lite", "like", "lamp", "bulb", "glow")
    private val modeSynonyms = arrayOf("mode", "mood", "move", "moved", "setting", "style", "moon")
    private val maxSynonyms = arrayOf("maximum", "max", "full")
    private val minSynonyms = arrayOf("minimum", "min", "meme", "low", "small")
    private val daySynonyms = arrayOf("day", "they", "the")
    private val nightSynonyms = arrayOf("night", "nine")
    private val seatSynonyms = arrayOf("seat", "seek", "see", "the")
    private val offSynonyms = arrayOf("off", "of", "oh")

    // RecognitionListener stubs
    override fun onFinalResult(hypothesis: String?) = Unit
    override fun onPartialResult(hypothesis: String?) = Unit
    override fun onError(exception: Exception?) = stopListening()
    override fun onTimeout() = stopListening()

    // Callback interface
    fun interface RecognitionCallback {
        fun onCommandReceived(command: String)
    }
}