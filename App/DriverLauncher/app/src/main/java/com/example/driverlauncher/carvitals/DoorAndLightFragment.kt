package android.vendor.carinfo

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast


class DoorAndLightFragmentFragment : Fragment() {

    private val VENDOR_EXTENSION_LIGHT_CONTROL_PROPERTY = 0x21400106 // LED control
    private val areaId = 0
    private var isLedOn = false
    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager
    private lateinit var progressBar: ProgressBar
    private lateinit var toggleButton: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L // 1 second

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_door_and_light, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressBar = view.findViewById(R.id.progressBar)
        toggleButton = view.findViewById(R.id.btnToggleLed)

        car = Car.createCar(requireContext())
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        toggleButton.setOnClickListener {
            toggleLed()
        }


    }

    private fun toggleLed() {
        try {
            isLedOn = !isLedOn
            carPropertyManager.setProperty(
                Int::class.java,
                VENDOR_EXTENSION_LIGHT_CONTROL_PROPERTY,
                areaId,
                if (isLedOn) 1 else 0
            )
            toggleButton.setImageResource(
                if (isLedOn) R.drawable.ic_led_on else R.drawable.ic_led_off
            )
            Toast.makeText(requireContext(), "LED ${if (isLedOn) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("DoorAndLightFragment", "LED toggle error", e)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}