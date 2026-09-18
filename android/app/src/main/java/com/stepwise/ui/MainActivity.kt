package com.stepwise.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
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
    private lateinit var startSessionButton: MaterialButton
    private lateinit var hardwareCard: MaterialCardView
    private lateinit var softwareCard: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        selectedMode = TutorMode.values().firstOrNull {
            it.name == savedInstanceState?.getString(SELECTED_MODE)
        } ?: SoftwareMode.mode

        val (scroll, column) = buildScrollableScreen(ctx)

        column.addView(TextView(ctx).apply {
            text = getString(R.string.app_name)
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(ctx, R.color.text_primary))
        })
        column.addView(TextView(ctx).apply {
            text = getString(R.string.app_tagline)
            textSize = 14f
            setTextColor(color(ctx, R.color.text_secondary))
            setPadding(0, dp(ctx, 4), 0, dp(ctx, 24))
        })

        column.addView(buildSectionLabel(ctx, getString(R.string.select_mode)))

        softwareCard = buildModeCard(
            ctx = ctx,
            title = getString(R.string.mode_software),
            description = getString(R.string.mode_software_desc),
            glyph = "📐",
            accentColorRes = R.color.accent_software
        ) { selectMode(SoftwareMode.mode) }
        column.addView(softwareCard)

        hardwareCard = buildModeCard(
            ctx = ctx,
            title = getString(R.string.mode_hardware),
            description = getString(R.string.mode_hardware_desc),
            glyph = "🔧",
            accentColorRes = R.color.accent_hardware
        ) { selectMode(HardwareMode.mode) }
        column.addView(hardwareCard)

        val (statusCard, statusInner) = buildCard(ctx)
        setLinearMargins(ctx, statusCard, top = 8, bottom = 20)
        status = buildBodyText(ctx)
        statusInner.addView(status)
        column.addView(statusCard)

        startSessionButton = buildPrimaryButton(ctx, "") {
            val target = if (selectedMode == TutorMode.HARDWARE) {
                HardwareSessionActivity::class.java
            } else {
                SoftwareSessionActivity::class.java
            }
            startActivity(Intent(ctx, target))
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        column.addView(startSessionButton)

        val privacyButton = buildSecondaryButton(ctx, getString(R.string.privacy_button)) {
            startActivity(Intent(ctx, PrivacyActivity::class.java))
        }
        privacyButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        setLinearMargins(ctx, privacyButton, top = 12)
        column.addView(privacyButton)

        setContentView(scroll)
        selectMode(selectedMode)
    }

    private fun buildModeCard(
        ctx: MainActivity,
        title: String,
        description: String,
        glyph: String,
        accentColorRes: Int,
        onClick: () -> Unit
    ): MaterialCardView {
        val (card, inner) = buildCard(ctx)
        inner.orientation = LinearLayout.HORIZONTAL
        inner.gravity = android.view.Gravity.CENTER_VERTICAL

        inner.addView(TextView(ctx).apply {
            text = glyph
            textSize = 28f
            setPadding(0, 0, dp(ctx, 16), 0)
        })

        val textColumn = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textColumn.addView(TextView(ctx).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(ctx, R.color.text_primary))
        })
        textColumn.addView(TextView(ctx).apply {
            text = description
            textSize = 13f
            setTextColor(color(ctx, R.color.text_secondary))
            setPadding(0, dp(ctx, 4), 0, 0)
        })
        inner.addView(textColumn)

        card.setStrokeColor(color(ctx, accentColorRes))
        card.setOnClickListener { onClick() }
        return card
    }

    private fun selectMode(mode: TutorMode) {
        selectedMode = mode
        status.text = when (mode) {
            TutorMode.HARDWARE -> getString(R.string.hardware_status)
            TutorMode.SOFTWARE -> getString(R.string.software_status)
        }
        startSessionButton.text = getString(
            when (mode) {
                TutorMode.HARDWARE -> R.string.start_hardware_session
                TutorMode.SOFTWARE -> R.string.start_software_session
            }
        )
        startSessionButton.isEnabled = true

        val activeStroke = dp(this, 2)
        val inactiveStroke = dp(this, 1)
        hardwareCard.setStrokeWidth(if (mode == TutorMode.HARDWARE) activeStroke else inactiveStroke)
        softwareCard.setStrokeWidth(if (mode == TutorMode.SOFTWARE) activeStroke else inactiveStroke)
        hardwareCard.setCardBackgroundColor(
            color(this, if (mode == TutorMode.HARDWARE) R.color.bg_surface_alt else R.color.bg_surface)
        )
        softwareCard.setCardBackgroundColor(
            color(this, if (mode == TutorMode.SOFTWARE) R.color.bg_surface_alt else R.color.bg_surface)
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SELECTED_MODE, selectedMode.name)
        super.onSaveInstanceState(outState)
    }

    private companion object { const val SELECTED_MODE = "selected_tutor_mode" }
}
