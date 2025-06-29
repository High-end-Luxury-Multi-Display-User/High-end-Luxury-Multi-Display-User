package android.vendor.carinfo

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    val  VENDOR_EXTENSION_Light_CONTROL_PROPERTY:Int = 0x21400106
    val areaID = 0
    lateinit var car: Car
    lateinit var carPropertyManager: CarPropertyManager
    private var ledState = false // false = off, true = on

    private lateinit var lightIcon: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        lightIcon = findViewById(R.id.light_icon) // the ImageView inside the light_button

        val lightButton = findViewById<LinearLayout>(R.id.light_button)
        lightButton.setOnClickListener {
            ledState = !ledState
            setLedState(ledState)
            updateLightIcon(ledState)
        }
        car = Car.createCar(this.applicationContext)
        if (car == null) {
            Log.e("LED", "Failed to create Car instance")
        } else {
            carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            Log.d("LED", "CarPropertyManager initialized")
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.batteryContainer, BatteryFragment())
            //   .replace(R.id.seatContainer, SeatFragment())
            .replace(R.id.mainFragmentContainer,MainFragment())
           // .replace(R.id.rightFragmentContainer, SeatFragment())
            .commit()
    }
    private fun setLedState(state: Boolean) {
        val value = if (state) 1 else 0
        try {
            synchronized(carPropertyManager) {
                carPropertyManager.setProperty(
                    Integer::class.java,
                    VENDOR_EXTENSION_Light_CONTROL_PROPERTY,
                    areaID,
                    Integer(value)
                )
                Log.d("LED", "LED state set to: $value")
            }
        } catch (e: Exception) {
            Log.e("LED", "Failed to set LED state", e)
        }
    }
    private fun updateLightIcon(state: Boolean) {
        if (state) {
            lightIcon.setImageResource(R.drawable.ic_led_off)
        } else {
            lightIcon.setImageResource(R.drawable.ic_led_on)
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        }
    }

}
