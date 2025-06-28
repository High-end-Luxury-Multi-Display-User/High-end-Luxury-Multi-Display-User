package com.example.driverlauncher.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.driverlauncher.R
import com.example.driverlauncher.settings.SettingsActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Hide system navigation bar (bottom) while keeping status bar (top) visible
        hideSystemNavigationBar()

        // Load fragments
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.navigation_container, NavigationFragment())
                .replace(R.id.dashboard_container, DashboardFragment())
                .commit()
        }

        // Set click listener for settings icon
        val settingsIcon = findViewById<ImageView>(R.id.icon_settings)
        settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && packageManager.hasSystemFeature("android.hardware.type.automotive")) {
            hideSystemNavigationBar()
        }
    }
}