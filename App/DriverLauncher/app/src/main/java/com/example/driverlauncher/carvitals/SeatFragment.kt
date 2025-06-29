package android.vendor.carinfo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SeatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SeatFragment : Fragment() {

    private lateinit var tiltView: SeatTiltView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_seat, container, false)
        tiltView = view.findViewById(R.id.tiltView)

        // Example: update every 2 seconds with dummy potentiometer angle
        view.postDelayed({
            tiltView.setBackrestAngle(60f) // Replace with actual sensor input
        }, 2000)

        return view
    }
}
