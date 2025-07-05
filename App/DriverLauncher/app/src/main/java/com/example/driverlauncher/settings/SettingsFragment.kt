package com.example.driverlauncher.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
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
            toggleImage(voiceImage, R.drawable.voice, R.drawable.no_voice)
        }
        voiceImage.setOnClickListener {
            toggleImage(voiceImage, R.drawable.voice, R.drawable.no_voice)
        }

        // Language Switch
        val languageContainer = view.findViewById<LinearLayout>(R.id.language_container)
        val languageImage = view.findViewById<ImageButton>(R.id.language_image)
        languageContainer.setOnClickListener {
            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
        }
        languageImage.setOnClickListener {
            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
        }
    }

    private fun toggleImage(imageButton: ImageButton, offResource: Int, onResource: Int) {
        if (imageButton.drawable.constantState == resources.getDrawable(offResource, null).constantState) {
            imageButton.setImageResource(onResource)
        } else {
            imageButton.setImageResource(offResource)
        }
    }
}