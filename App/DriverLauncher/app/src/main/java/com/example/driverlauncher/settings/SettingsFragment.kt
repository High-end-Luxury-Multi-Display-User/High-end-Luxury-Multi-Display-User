package com.example.driverlauncher.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.driverlauncher.MainActivity
import com.example.driverlauncher.R
import java.io.BufferedReader
import java.io.InputStreamReader

class SettingsFragment : Fragment() {

    lateinit var themeImage : ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.i("TAG", "onViewCreated: SettingsFragment")

        // Theme Switch
        val themeContainer = view.findViewById<LinearLayout>(R.id.theme_container)
        themeImage = view.findViewById(R.id.theme_image)
        themeContainer.setOnClickListener @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES) {
            toggleOverlayTheme()
        }
        themeImage.setOnClickListener @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES) {
            toggleOverlayTheme()
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
        updateThemeImage(themeImage)
    }

    @SuppressLint("UseCompatLoadingForDrawables", "UseRequireInsteadOfGet")
    private fun toggleImage(imageButton: ImageButton, offResource: Int, onResource: Int) {
        Log.i("SettingsFragment", "toggleImage: current=${imageButton.drawable.constantState}, off=$offResource, on=$onResource")

//        // Language Switch
//        val languageContainer = view?.findViewById<LinearLayout>(R.id.language_container)
//        val languageImage = view?.findViewById<ImageButton>(R.id.language_image)
//        languageContainer.setOnClickListener {
//            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
//        }
//        languageImage.setOnClickListener {
//            toggleImage(languageImage, R.drawable.english, R.drawable.arabic)
//        }
    }

    fun updateVoiceImage() {
        val isRunning = MainActivity.isServiceRunning
        view?.findViewById<ImageButton>(R.id.voice_image)?.setImageResource(
            if (isRunning) R.drawable.voice else R.drawable.no_voice
        )
    }

    private fun updateThemeImage(themeImage: ImageButton) {
        val isOverlayEnabled = isOverlayEnabled()
        themeImage.setImageResource(if (isOverlayEnabled) R.drawable.night else R.drawable.day)
        Log.i("SettingsFragment", "Initial overlay state: enabled=$isOverlayEnabled")
    }

    override fun onResume() {
        super.onResume()
        updateVoiceImage()
        view?.findViewById<ImageButton>(R.id.theme_image)?.let { updateThemeImage(it) }
    }

    @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES)
    @SuppressLint("UseCompatLoadingForDrawables")
    fun toggleOverlayTheme() {
        setOverlayTheme(!isOverlayEnabled())
    }

    @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES)
    @SuppressLint("UseCompatLoadingForDrawables")
    fun setOverlayTheme(enable: Boolean) {
        val overlayPackage = "com.example.lightmode"
        val targetPackage = "com.example.driverlauncher"
        val userId = 10 // User 10, as specified

        try {
            // Determine current state
            val isOverlayEnabled = isOverlayEnabled()
            Log.i("SettingsFragment", "Overlay $overlayPackage current state: enabled=$isOverlayEnabled, setting to enabled=$enable")

            // Only execute if state needs to change
            if (isOverlayEnabled != enable) {
                // Execute shell command to toggle overlay
                val command = "cmd overlay ${if (enable) "enable" else "disable"} --user $userId $overlayPackage"
                val (success, output) = executeShellCommand(command)
                if (!success) {
                    throw IllegalStateException("Shell command failed: $output")
                }

                Log.i("SettingsFragment", "Overlay $overlayPackage set to enabled=$enable")

                // Update the theme image to reflect the new state
                themeImage.setImageResource(if (enable) R.drawable.night else R.drawable.day)

                // Restart the target app to apply the overlay
                restartTargetApp(requireContext(), targetPackage)
            } else {
                Log.i("SettingsFragment", "Overlay $overlayPackage already in desired state: enabled=$enable")
            }

            // Show user feedback
            Toast.makeText(requireContext(), "Theme ${if (enable) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()

        } catch (e: SecurityException) {
            Log.e("SettingsFragment", "Permission denied: Ensure app is a system app with CHANGE_OVERLAY_PACKAGES permission", e)
            Toast.makeText(requireContext(), "Permission denied: App must be a system app", Toast.LENGTH_LONG).show()
        } catch (e: IllegalStateException) {
            Log.e("SettingsFragment", "Overlay $overlayPackage not found or invalid: ${e.message}", e)
            Toast.makeText(requireContext(), "Overlay not found or invalid", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Failed to set overlay $overlayPackage: ${e.message}", e)
            Toast.makeText(requireContext(), "Failed to set theme", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun isOverlayEnabled(): Boolean {
        val overlayPackage = "com.example.lightmode"
        val userId = 10
        try {
            val (success, output) = executeShellCommand("cmd overlay list --user $userId")
            if (success) {
                val enabled = output.lines().any { it.contains("[x] $overlayPackage") }
                Log.i("SettingsFragment", "Overlay state check: enabled=$enabled, output=$output")
                return enabled
            } else {
                Log.w("SettingsFragment", "Failed to check overlay state: $output")
            }
        } catch (e: Exception) {
            Log.w("SettingsFragment", "Error checking overlay state: ${e.message}")
        }
        // Fallback to themeImage drawable state
        val themeImage = view?.findViewById<ImageButton>(R.id.theme_image)
        val enabled = themeImage?.drawable?.constantState == resources.getDrawable(R.drawable.night, null).constantState
        Log.i("SettingsFragment", "Fallback to themeImage state: enabled=$enabled")
        return enabled
    }

    private fun executeShellCommand(command: String): Pair<Boolean, String> {
        try {
            val process = Runtime.getRuntime().exec(command)
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Log.i("SettingsFragment", "Executed shell command: $command, output: $output")
                return Pair(true, output)
            } else {
                Log.e("SettingsFragment", "Shell command failed: $command, error: $error, exit code: $exitCode")
                return Pair(false, error)
            }
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Failed to execute shell command: ${e.message}", e)
            return Pair(false, e.message ?: "Unknown error")
        }
    }

    @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES)
    private fun restartTargetApp(context: Context, targetPackage: String) {
        try {
            // Force-stop the target app
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.killBackgroundProcesses(targetPackage)
            Log.i("SettingsFragment", "Force-stopped $targetPackage")

            // Relaunch the target app
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ContextCompat.startActivity(context, intent, null)
                Log.i("SettingsFragment", "Relaunched $targetPackage")
            } else {
                Log.w("SettingsFragment", "No launch intent found for $targetPackage; trying System UI restart")
                restartSystemUi(context)
            }
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Failed to restart $targetPackage: ${e.message}", e)
            Toast.makeText(context, "Failed to restart target app", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES)
    private fun restartSystemUi(context: Context) {
        try {
            val (success, output) = executeShellCommand("killall com.android.systemui")
            if (success) {
                Log.i("SettingsFragment", "Restarted System UI")
            } else {
                throw IllegalStateException("Failed to restart System UI: $output")
            }
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Failed to restart System UI: ${e.message}", e)
            Toast.makeText(context, "Failed to restart System UI", Toast.LENGTH_LONG).show()
        }
    }

}