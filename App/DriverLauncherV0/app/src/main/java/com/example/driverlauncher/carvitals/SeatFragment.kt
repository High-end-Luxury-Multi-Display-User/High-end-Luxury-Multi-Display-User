package com.example.driverlauncher.carvitals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import android.widget.TextView
import com.example.driverlauncher.R

class SeatFragment : Fragment() {

    private lateinit var backrestDefault: ImageView
    private lateinit var backrestForward: ImageView
    private lateinit var backrestReverse: ImageView
    private lateinit var angleText: TextView

    private enum class SeatPosition {
        DEFAULT, FORWARD, REVERSE
    }

    private var currentPosition = SeatPosition.DEFAULT

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
        backrestDefault = view.findViewById(R.id.backrestImage)
        backrestForward = view.findViewById(R.id.backrestForward)
        backrestReverse = view.findViewById(R.id.backrestReverse)
        angleText = view.findViewById(R.id.angleText)

        val btnForward = view.findViewById<ImageButton>(R.id.btnForward)
        val btnReverse = view.findViewById<ImageButton>(R.id.btnReverse)

        // Initial state
        switchTo(SeatPosition.DEFAULT)

        btnForward.setOnClickListener {
            when (currentPosition) {
                SeatPosition.DEFAULT -> switchTo(SeatPosition.FORWARD)
                SeatPosition.REVERSE -> switchTo(SeatPosition.DEFAULT)
                SeatPosition.FORWARD -> {} // no change
            }
        }

        btnReverse.setOnClickListener {
            when (currentPosition) {
                SeatPosition.DEFAULT -> switchTo(SeatPosition.REVERSE)
                SeatPosition.FORWARD -> switchTo(SeatPosition.DEFAULT)
                SeatPosition.REVERSE -> {} // no change
            }
        }
    }

    private fun switchTo(position: SeatPosition) {
        // Hide all
        backrestDefault.visibility = View.GONE
        backrestForward.visibility = View.GONE
        backrestReverse.visibility = View.GONE

        // Show selected
        when (position) {
            SeatPosition.DEFAULT -> {
                backrestDefault.visibility = View.VISIBLE
                angleText.text = "70°"
            }
            SeatPosition.FORWARD -> {
                backrestForward.visibility = View.VISIBLE
                angleText.text = "90°"
            }
            SeatPosition.REVERSE -> {
                backrestReverse.visibility = View.VISIBLE
                angleText.text = "45°"
            }
        }

        currentPosition = position
    }
}
