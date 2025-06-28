package com.example.driverlauncher.home

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.widget.LinearLayout
import com.example.driverlauncher.R

class HomeActivity : AppCompatActivity() {

    private lateinit var rootLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        rootLayout = findViewById(R.id.root_layout) ?: throw IllegalStateException("Root layout not found")

        // Load fragments
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.navigation_container, NavigationFragment())
                .replace(R.id.dashboard_container, DashboardFragment())
                .commit()
        }

        // Set initial orientation based on multi-window state
        updateLayoutOrientation(isInMultiWindowMode)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        updateLayoutOrientation(isInMultiWindowMode)
    }

    override fun onBackPressed() {
        if (isInMultiWindowMode) {
            // Send broadcast to finish SecondActivity
            val intent = Intent("com.example.merge_split_screen.FINISH_SECOND_ACTIVITY")
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        } else {
            super.onBackPressed()
        }
    }

    private fun updateLayoutOrientation(isInMultiWindowMode: Boolean) {
        rootLayout.orientation = if (isInMultiWindowMode) {
            LinearLayout.VERTICAL
        } else {
            LinearLayout.HORIZONTAL
        }
        // Update fragment layouts
        val navLayout = findViewById<FrameLayout>(R.id.navigation_container)?.layoutParams as LinearLayout.LayoutParams
        val dashLayout = findViewById<FrameLayout>(R.id.dashboard_container)?.layoutParams as LinearLayout.LayoutParams
        if (isInMultiWindowMode) {
            navLayout.apply {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = 0
                weight = 2f
            }
            dashLayout.apply {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = 0
                weight = 1f
            }
        } else {
            navLayout.apply {
                width = 0
                height = LinearLayout.LayoutParams.MATCH_PARENT
                weight = 2f
            }
            dashLayout.apply {
                width = 0
                height = LinearLayout.LayoutParams.MATCH_PARENT
                weight = 1f
            }
        }
        rootLayout.requestLayout()
    }
}