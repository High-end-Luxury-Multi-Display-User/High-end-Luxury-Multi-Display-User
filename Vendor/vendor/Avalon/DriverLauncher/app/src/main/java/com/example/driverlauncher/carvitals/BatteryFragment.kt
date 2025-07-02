package com.example.driverlauncher.carvitals

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.driverlauncher.R

class BatteryFragment : Fragment(R.layout.fragment_battery) {

    private val VENDOR_EXTENSION_BATTERY_PROPERTY = 0x21400105
    private val areaId = 0

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    private lateinit var circleProgress: ProgressBar
    private lateinit var rangeText: TextView
    private lateinit var batteryText: TextView
    private lateinit var consumptionText: TextView
    private lateinit var chargerText: TextView
    private lateinit var efficiencyText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val pollInterval = 1000L

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                val batteryProp = carPropertyManager.getProperty(
                    Integer::class.java,
                    VENDOR_EXTENSION_BATTERY_PROPERTY,
                    areaId
                )

                if (batteryProp != null) {
                    val rawPotValue = batteryProp.value.toInt()
                    val batteryPercent = mapPotToPercentage(rawPotValue)
                    updateUI(batteryPercent)
                    Log.d("BatteryFragment", "Raw battery value: $rawPotValue → $batteryPercent%")
                }

            } catch (e: Exception) {
                Log.e("BatteryFragment", "Polling error", e)
            }

            handler.postDelayed(this, pollInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_battery, container, false)

        circleProgress = view.findViewById<ProgressBar>(R.id.circleProgress)!!
	rangeText = view.findViewById<TextView>(R.id.rangeText)!!
	batteryText = view.findViewById<TextView>(R.id.text_battery)!!
	consumptionText = view.findViewById<TextView>(R.id.text_consumption)!!
	chargerText = view.findViewById<TextView>(R.id.text_charger)!!
	efficiencyText = view.findViewById<TextView>(R.id.text_efficiency)!!

        try {
           car = Car.createCar(requireContext().applicationContext)!!
	   carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        } catch (e: Exception) {
            Log.e("BatteryFragment", "Car manager init failed", e)
            return view
        }

        handler.post(pollRunnable)
        return view
    }

    private fun mapPotToPercentage(rawValue: Int): Int {
        val clamped = rawValue.coerceIn(0, 10000)
        return (clamped / 100.0).toInt().coerceIn(0, 100)
    }

    private fun updateUI(battery: Int) {
        val range = battery * 3.3
        val consumption = (200 - battery) + 63
        val nextCharger = (battery * 0.48).toInt()
        val efficiency = (battery * 1.24).coerceAtMost(100.0).toInt()

        circleProgress.progress = battery
        rangeText.text = "$battery%"
        batteryText.text = "${range.toInt()} km"
        consumptionText.text = "$consumption W/km"
        chargerText.text = "$nextCharger km"
        efficiencyText.text = "$efficiency%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(pollRunnable)
        
    }
}
