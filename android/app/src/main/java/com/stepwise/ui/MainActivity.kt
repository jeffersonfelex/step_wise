package com.stepwise.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.stepwise.R
import com.stepwise.core.TutorMode
import com.stepwise.hardware.HardwareMode
import com.stepwise.software.SoftwareMode

/**
 * Composition shell. Mode-specific workflows belong in their modules.
 * Selecting a mode does not start capture or connect to an AI provider.
 */
class MainActivity : AppCompatActivity() {
    private var selectedMode = SoftwareMode.mode
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedMode = TutorMode.values().firstOrNull {
            it.name == savedInstanceState?.getString(SELECTED_MODE)
        } ?: SoftwareMode.mode

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        layout.addView(TextView(this).apply {
            text = getString(R.string.select_mode)
            textSize = 24f
        })
        layout.addView(MaterialButton(this).apply {
            text = getString(R.string.mode_software)
            setOnClickListener { selectMode(SoftwareMode.mode) }
        })
        layout.addView(MaterialButton(this).apply {
            text = getString(R.string.mode_hardware)
            setOnClickListener { selectMode(HardwareMode.mode) }
        })
        status = TextView(this).apply { textSize = 18f }
        layout.addView(status)
        setContentView(layout)
        selectMode(selectedMode)
    }

    private fun selectMode(mode: TutorMode) {
        selectedMode = mode
        status.setText(when (mode) {
            TutorMode.HARDWARE -> R.string.hardware_status
            TutorMode.SOFTWARE -> R.string.software_status
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SELECTED_MODE, selectedMode.name)
        super.onSaveInstanceState(outState)
    }

    private companion object { const val SELECTED_MODE = "selected_tutor_mode" }
}
