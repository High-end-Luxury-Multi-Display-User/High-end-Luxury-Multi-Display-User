package com.example.driverlauncher.voskva

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.driverlauncher.R

class VoskActivity : AppCompatActivity(), VoskRecognitionService.RecognitionCallback {

    companion object {
        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }

    private lateinit var micIcon: ImageView
    private lateinit var statusImage: ImageView
    private var isServiceRunning = false
    private var voskService: VoskRecognitionService? = null
    private var isBound = false
    private var lastCommand = ""

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as VoskRecognitionService.LocalBinder
            voskService = binder.getService().apply {
                setRecognitionCallback(this@VoskActivity)
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
        setContentView(R.layout.main)

        micIcon = findViewById(R.id.mic_icon)
        statusImage = findViewById(R.id.statusImage)

        micIcon.setOnClickListener { toggleService() }

        checkPermissions()
        startServiceOnlyOnce()
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
                statusImage.setImageResource(R.drawable.ledon)
                playAudio(R.raw.lighton)
            }
            "light_off" -> {
                statusImage.setImageResource(R.drawable.ledoff)
                playAudio(R.raw.lightoff)
            }
            "day_mode" -> {
                statusImage.setImageResource(R.drawable.sun)
                playAudio(R.raw.day)
            }
            "night_mode" -> {
                statusImage.setImageResource(R.drawable.moon)
                playAudio(R.raw.night)
            }
            "seat_45" -> {
                statusImage.setImageResource(R.drawable.seat_45)
                playAudio(R.raw.seat)
            }
            "seat_70" -> {
                statusImage.setImageResource(R.drawable.seat_70)
                playAudio(R.raw.seatdefault)
            }
            "seat_90" -> {
                statusImage.setImageResource(R.drawable.seat_90)
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
        micIcon.setImageResource(
            if (isServiceRunning) R.drawable.ic_mic_on else R.drawable.ic_mic_off
        )
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