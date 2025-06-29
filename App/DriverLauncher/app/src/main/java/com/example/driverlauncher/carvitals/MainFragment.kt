package android.vendor.carinfo

import android.car.Car
import android.car.hardware.CarPropertyValue
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

class MainFragment : Fragment() {

    private val VENDOR_EXTENSION_FLeft_Door_PROPERTY = 0x21400107
    private val VENDOR_EXTENSION_RLeft_Door_PROPERTY = 0x21400108
    private val VENDOR_EXTENSION_FRight_Door_PROPERTY = 0x21400109
    private val VENDOR_EXTENSION_RRight_Door_PROPERTY = 0x2140010A
    private val areaID = 0

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    private lateinit var doorFL: ImageView
    private lateinit var doorFR: ImageView
    private lateinit var doorRL: ImageView
    private lateinit var doorRR: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 500L // ms

    private val updateTask = object : Runnable {
        override fun run() {
            try {
                updateDoorVisibility(doorFL, VENDOR_EXTENSION_FLeft_Door_PROPERTY, "FL")
                updateDoorVisibility(doorFR, VENDOR_EXTENSION_FRight_Door_PROPERTY, "FR")
                updateDoorVisibility(doorRL, VENDOR_EXTENSION_RLeft_Door_PROPERTY, "RL")
                updateDoorVisibility(doorRR, VENDOR_EXTENSION_RRight_Door_PROPERTY, "RR")
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
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doorFL = view.findViewById(R.id.doorFL)
        doorFR = view.findViewById(R.id.doorFR)
        doorRL = view.findViewById(R.id.doorRL)
        doorRR = view.findViewById(R.id.doorRR)

        try {
            car = Car.createCar(requireContext().applicationContext)
            carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        } catch (e: Exception) {
            Log.e("CarDoors", "Car init failed", e)
            return
        }

        handler.post(updateTask) // Start periodic polling
    }

    private fun updateDoorVisibility(doorImage: ImageView, propertyId: Int, label: String) {
        try {
            val carPropValue =
                carPropertyManager.getProperty(Integer::class.java, propertyId, areaID)
            if (carPropValue != null) {
                val isOpen = carPropValue.value.toInt() == 1
                doorImage.visibility = if (isOpen) View.VISIBLE else View.GONE
                Log.d("CarDoors", "Door $label state: ${carPropValue.value}")
            }
        } catch (e: Exception) {
            Log.e("CarDoors", "Failed to read property $propertyId", e)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTask) // Stop when fragment is destroyed
    }
}
