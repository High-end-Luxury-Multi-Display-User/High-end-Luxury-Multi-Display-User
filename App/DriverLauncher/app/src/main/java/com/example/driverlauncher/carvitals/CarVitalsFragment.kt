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
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.driverlauncher.R

class CarVitalsFragment : Fragment() {

    private val VENDOR_EXTENSION_FLeft_Door_PROPERTY = 0x21400107
    private val VENDOR_EXTENSION_RLeft_Door_PROPERTY = 0x21400108
    private val VENDOR_EXTENSION_FRight_Door_PROPERTY = 0x21400109
    private val VENDOR_EXTENSION_RRight_Door_PROPERTY = 0x2140010A
    private val areaID = 0

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    private lateinit var openDoorFL: ImageView
    private lateinit var openDoorFR: ImageView
    private lateinit var openDoorRL: ImageView
    private lateinit var openDoorRR: ImageView

    private lateinit var closedDoorFL: ImageView
    private lateinit var closedDoorFR: ImageView
    private lateinit var closedDoorRL: ImageView
    private lateinit var closedDoorRR: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 500L // ms

    private val updateTask = object : Runnable {
        override fun run() {
            try {
                updateDoorVisibility(openDoorFL, closedDoorFL, VENDOR_EXTENSION_FLeft_Door_PROPERTY, "FL")
                updateDoorVisibility(openDoorFR, closedDoorFR, VENDOR_EXTENSION_FRight_Door_PROPERTY, "FR")
                updateDoorVisibility(openDoorRL, closedDoorRL, VENDOR_EXTENSION_RLeft_Door_PROPERTY, "RL")
                updateDoorVisibility(openDoorRR, closedDoorRR, VENDOR_EXTENSION_RRight_Door_PROPERTY, "RR")
            } catch (e: Exception) {
                Log.e("CarDoors", "Error reading door states", e)
            } finally {
                handler.postDelayed(this, updateInterval)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_car_vitals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use explicit type + !! to force unwrap
        closedDoorFL = view.findViewById<ImageView>(R.id.closed_door_fl)!!
        closedDoorFR = view.findViewById<ImageView>(R.id.closed_door_fr)!!
        closedDoorRL = view.findViewById<ImageView>(R.id.closed_door_rl)!!
        closedDoorRR = view.findViewById<ImageView>(R.id.closed_door_rr)!!

        openDoorFL = view.findViewById<ImageView>(R.id.open_door_fl)!!
        openDoorFR = view.findViewById<ImageView>(R.id.open_door_fr)!!
        openDoorRL = view.findViewById<ImageView>(R.id.open_door_rl)!!
        openDoorRR = view.findViewById<ImageView>(R.id.open_door_rr)!!

        try {
            car = Car.createCar(requireContext().applicationContext)!!
            carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        } catch (e: Exception) {
            Log.e("CarDoors", "Car init failed", e)
            return
        }

        handler.post(updateTask) // Start periodic polling
    }

    private fun updateDoorVisibility(
        openDoorImage: ImageView,
        closedDoorImage: ImageView,
        propertyId: Int,
        label: String
    ) {
        try {
            val carPropValue = carPropertyManager.getProperty(Integer::class.java, propertyId, areaID)
            if (carPropValue != null) {
                val isOpen = carPropValue.value.toInt() == 1
                openDoorImage.visibility = if (isOpen) View.VISIBLE else View.GONE
                closedDoorImage.visibility = if (isOpen) View.GONE else View.VISIBLE
                Log.d("CarDoors", "Door $label state: ${carPropValue.value}")
            }
        } catch (e: Exception) {
            Log.e("CarDoors", "Failed to read property $propertyId", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTask)
    }
}

