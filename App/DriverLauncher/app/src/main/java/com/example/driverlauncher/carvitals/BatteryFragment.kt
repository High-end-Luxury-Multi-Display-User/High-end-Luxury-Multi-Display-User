package android.vendor.carinfo

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class BatteryFragment : Fragment(R.layout.fragment_battery) {
    private val VENDOR_EXTENSION_BATTERY_PROPERTY = 0x21100105
    private val areaId = 0

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    private lateinit var circleProgress: ProgressBar
    private lateinit var rangeText: TextView
    private lateinit var batteryText: TextView
    private lateinit var consumptionText: TextView
    private lateinit var chargerText: TextView
    private lateinit var efficiencyText: TextView

    private val batteryCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(event: CarPropertyValue<*>) {
            val battery = event.value as? Int ?: return
            updateUI(battery)
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.e("BatteryFragment", "Error with prop $propId in zone $zone")
        }
    }

    private fun updateUI(battery: Int) {
        // Simulate calculations:
        val range = battery * 3.3    // example: 75% = 247km
        val consumption = (200 - battery) + 63  // inverse
        val nextCharger = (battery * 0.48).toInt()
        val efficiency = (battery * 1.24).coerceAtMost(100.0).toInt()

        circleProgress.progress = battery
        rangeText.text = "$battery%"
        batteryText.text = "${range.toInt()} km"
        consumptionText.text = "$consumption W/km"
        chargerText.text = "$nextCharger km"
        efficiencyText.text = "$efficiency%"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_battery, container, false)
        circleProgress = view.findViewById(R.id.circleProgress)
        rangeText = view.findViewById(R.id.rangeText)
        batteryText = view.findViewById(R.id.text_battery)
        consumptionText = view.findViewById(R.id.text_consumption)
        chargerText = view.findViewById(R.id.text_charger)
        efficiencyText = view.findViewById(R.id.text_efficiency)

        car = Car.createCar(requireContext())
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        carPropertyManager.registerCallback(
            batteryCallback,
            VENDOR_EXTENSION_BATTERY_PROPERTY,
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        )

        // Simulate value on first load
        updateUI(75)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carPropertyManager.unregisterCallback(batteryCallback)
    }
}
