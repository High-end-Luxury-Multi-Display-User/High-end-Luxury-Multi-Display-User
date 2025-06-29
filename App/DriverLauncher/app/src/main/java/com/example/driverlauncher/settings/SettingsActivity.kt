package com.example.driverlauncher.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.driverlauncher.R
import com.example.driverlauncher.home.HomeActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        hideSystemNavigationBar()

        // Theme Switch
        val themeContainer = findViewById<LinearLayout>(R.id.theme_container)
        val themeImage = findViewById<ImageButton>(R.id.theme_image)
        themeContainer.setOnClickListener {
            toggleImage(themeImage, R.drawable.day, R.drawable.night)
        }

        // Gesture Control Switch
        val gestureContainer = findViewById<LinearLayout>(R.id.gesture_container)
        val gestureImage = findViewById<ImageButton>(R.id.gesture_image)
        gestureContainer.setOnClickListener {
            toggleImage(gestureImage, R.drawable.gesture, R.drawable.no_gesture)
        }

        // Voice Assist Switch
        val voiceContainer = findViewById<LinearLayout>(R.id.voice_container)
        val voiceImage = findViewById<ImageButton>(R.id.voice_image)
        voiceContainer.setOnClickListener {
            toggleImage(voiceImage, R.drawable.voice, R.drawable.no_voice)
        }

        // Language Switch
        val languageContainer = findViewById<LinearLayout>(R.id.language_container)
        val languageImage = findViewById<ImageButton>(R.id.language_image)
        languageContainer.setOnClickListener {
            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
        }

        // Set click listener for settings icon
        val homeIcon = findViewById<ImageView>(R.id.icon_home)
        homeIcon.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun hideSystemNavigationBar() {
        if (packageManager.hasSystemFeature("android.hardware.type.automotive")) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun toggleImage(imageButton: ImageButton, offResource: Int, onResource: Int) {
        if (imageButton.drawable.constantState == resources.getDrawable(offResource).constantState) {
            imageButton.setImageResource(onResource)
        } else {
            imageButton.setImageResource(offResource)
        }
    }
}