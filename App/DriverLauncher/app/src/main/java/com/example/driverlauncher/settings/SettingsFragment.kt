package com.example.driverlauncher.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.driverlauncher.MainActivity
import com.example.driverlauncher.R
import com.example.driverlauncher.drawsiness.EyeDetectionService

class SettingsFragment : Fragment() {
    private var isDrowsinessEnabledUI = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        // Theme Switch
        val themeContainer = view.findViewById<LinearLayout>(R.id.theme_container)
        val themeImage = view.findViewById<ImageButton>(R.id.theme_image)
        themeContainer.setOnClickListener {
            toggleImage(themeImage, R.drawable.day, R.drawable.night)
        }
        themeImage.setOnClickListener {
            toggleImage(themeImage, R.drawable.day, R.drawable.night)
        }

        // Gesture Control Switch
        val gestureContainer = view.findViewById<LinearLayout>(R.id.gesture_container)
        val gestureImage = view.findViewById<ImageButton>(R.id.gesture_image)
        gestureContainer.setOnClickListener {
            toggleImage(gestureImage, R.drawable.gesture, R.drawable.no_gesture)
        }
        gestureImage.setOnClickListener {
            toggleImage(gestureImage, R.drawable.gesture, R.drawable.no_gesture)
        }

        // Voice Assist Switch
        val voiceContainer = view.findViewById<LinearLayout>(R.id.voice_container)
        val voiceImage = view.findViewById<ImageButton>(R.id.voice_image)
        voiceContainer.setOnClickListener {
            (activity as? MainActivity)?.toggleService()
        }
        voiceImage.setOnClickListener {
            (activity as? MainActivity)?.toggleService()
        }

        // Set initial state
        updateVoiceImage()

        // Language Switch
//        val languageContainer = view.findViewById<LinearLayout>(R.id.language_container)
//        val languageImage = view.findViewById<ImageButton>(R.id.language_image)
//        languageContainer.setOnClickListener {
//            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
//        }
//        languageImage.setOnClickListener {
//            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
//        }
        val drawsinessContainer = view.findViewById<LinearLayout>(R.id.drawsiness_container)
        val drawsinessImage = view.findViewById<ImageButton>(R.id.drawsiness_image)

        val onClickListener = View.OnClickListener {
            val mainActivity = activity as? MainActivity
            val isBoundEye = mainActivity?.isBoundEye ?: false
            val isEyeDetectionEnabled = mainActivity?.isEyeDetectionEnabled ?: false

            Log.d("SettingsFragment", "Drowsiness clicked: isBound=$isBoundEye, isEnabled=$isEyeDetectionEnabled")

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            if (isBoundEye) {
                mainActivity?.toggleEyeService()
                toggleDrowsinessIcon(drawsinessImage)
            } else {
                Toast.makeText(requireContext(), "Service not bound", Toast.LENGTH_SHORT).show()
            }
        }

        drawsinessContainer.setOnClickListener(onClickListener)
        drawsinessImage.setOnClickListener(onClickListener)
    }


    private fun toggleImage(imageButton: ImageButton, offResource: Int, onResource: Int) {
        if (imageButton.drawable.constantState == resources.getDrawable(offResource, null).constantState) {
            imageButton.setImageResource(onResource)
        } else {
            imageButton.setImageResource(offResource)
        }
    }
    private fun toggleDrowsinessIcon(imageButton: ImageButton) {
        isDrowsinessEnabledUI = !isDrowsinessEnabledUI
        imageButton.setImageResource(
            if (isDrowsinessEnabledUI) R.drawable.eye_enabled else R.drawable.eye_disabled
        )
    }

    // ✅ FIX: Public method to allow MainActivity to update icon when detection state changes
    fun updateDrowsinessIcon(enabled: Boolean) {
        isDrowsinessEnabledUI = enabled
        view?.findViewById<ImageButton>(R.id.drawsiness_image)?.setImageResource(
            if (enabled) R.drawable.eye_enabled else R.drawable.eye_disabled
        )
    }
    fun updateVoiceImage() {
        val isRunning = MainActivity.isServiceRunning
        view?.findViewById<ImageButton>(R.id.voice_image)?.setImageResource(
            if (isRunning) R.drawable.voice else R.drawable.no_voice
        )
    }

    override fun onResume() {
        super.onResume()
        updateVoiceImage()
    }
}