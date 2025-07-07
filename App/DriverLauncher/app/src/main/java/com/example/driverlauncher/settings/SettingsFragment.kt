package com.example.driverlauncher.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.driverlauncher.MainActivity
import com.example.driverlauncher.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themeContainer = view.findViewById<LinearLayout>(R.id.theme_container)
        val themeImage = view.findViewById<ImageButton>(R.id.theme_image)
        themeContainer.setOnClickListener {
            toggleImage(themeImage, R.drawable.day, R.drawable.night)
        }
        themeImage.setOnClickListener {
            toggleImage(themeImage, R.drawable.day, R.drawable.night)
        }

        val gestureContainer = view.findViewById<LinearLayout>(R.id.gesture_container)
        val gestureImage = view.findViewById<ImageButton>(R.id.gesture_image)
        gestureContainer.setOnClickListener {
            toggleImage(gestureImage, R.drawable.gesture, R.drawable.no_gesture)
        }
        gestureImage.setOnClickListener {
            toggleImage(gestureImage, R.drawable.gesture, R.drawable.no_gesture)
        }

        val voiceContainer = view.findViewById<LinearLayout>(R.id.voice_container)
        val voiceImage = view.findViewById<ImageButton>(R.id.voice_image)
        voiceContainer.setOnClickListener {
            (activity as? MainActivity)?.toggleService()
        }
        voiceImage.setOnClickListener {
            (activity as? MainActivity)?.toggleService()
        }

        updateVoiceImage()

        // Drowsiness toggle
        val drowsinessContainer = view.findViewById<LinearLayout>(R.id.drawsiness_container)
        val drowsinessImage = view.findViewById<ImageButton>(R.id.drawsiness_image)

        val onClickListener = View.OnClickListener {
            val mainActivity = activity as? MainActivity
            if (mainActivity == null || mainActivity.isDestroyed) {
                Log.w("SettingsFragment", "MainActivity is null")
                return@OnClickListener
            }

            // Check permission
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            // Ensure service is bound
            if (!mainActivity.isBoundEye) {
                Toast.makeText(requireContext(), "Drowsiness service not yet available", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            // Toggle eye detection and get new state
            val newState = mainActivity.toggleEyeService()
            updateDrowsinessIcon(newState)
        }

        drowsinessContainer.setOnClickListener(onClickListener)
        drowsinessImage.setOnClickListener(onClickListener)
    }

    private fun toggleImage(imageButton: ImageButton, offResource: Int, onResource: Int) {
        val current = imageButton.drawable?.constantState
        val off = resources.getDrawable(offResource, null).constantState
        imageButton.setImageResource(if (current == off) onResource else offResource)
    }

    // Update from inside or outside
    fun updateDrowsinessIcon(enabled: Boolean) {
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

        // Update drowsiness icon from actual state
        val mainActivity = activity as? MainActivity
        updateDrowsinessIcon(mainActivity?.isEyeDetectionEnabled ?: false)
    }
}
