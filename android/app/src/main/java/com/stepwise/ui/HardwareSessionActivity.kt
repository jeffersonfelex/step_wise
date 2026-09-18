package com.stepwise.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stepwise.R
import com.stepwise.ai.LlamaStepEvaluator
import com.stepwise.audio.TtsManager
import com.stepwise.audio.VoiceInputManager
import com.stepwise.engine.ProceduralEngine
import com.stepwise.engine.StepResult
import com.stepwise.glasses.MetaDatSessionManager
import com.stepwise.glasses.QualityGate
import com.stepwise.power.BatteryMonitor
import com.stepwise.procedure.ProcedureLoader
import com.stepwise.resilience.LinkStateManager
import com.stepwise.session.SessionController
import com.stepwise.session.VoiceCommand
import com.stepwise.vision.ColorSegmentationDetector
import com.stepwise.vision.CompositeDetector
import com.stepwise.vision.MediaPipeLiteRtDetector
import kotlinx.coroutines.Job

/**
 * Sessão do modo hardware (montagem física) — constrói e liga CTRL/ENG/
 * PROC/DAT/VOICE/TTS/REC (ver diagrama de arquitetura) e expõe:
 *  - início de sessão com o procedimento de demonstração
 *    ("montagem_engrenagens", único com JSON real no momento);
 *  - fallback de UI para VERIFY/HELP/REPEAT (o principal é por voz, ver
 *    claude/checklist-hackathon.md — a UI é só contingência de pitch);
 *  - Modo Demonstração, para mostrar a transição de fading (feedback da
 *    mentoria, 15/09) e roteirizar KNOWN_ERROR/INCONCLUSIVE sem depender
 *    da integração real do Meta DAT (ainda pendente, ver
 *    MetaDatSessionManager.captureBestFrameFromStream).
 */
class HardwareSessionActivity : AppCompatActivity() {

    private lateinit var sessionController: SessionController
    private lateinit var proceduralEngine: ProceduralEngine
    private lateinit var procedureLoader: ProcedureLoader
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var ttsManager: TtsManager
    private lateinit var statusView: TextView
    private lateinit var batteryView: TextView
    private var batteryJob: Job? = null

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            sessionController.startSession(PROCEDURE_ID)
            refreshStatus()
        } else {
            statusView.text = getString(R.string.hardware_permissions_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        proceduralEngine = ProceduralEngine()
        ttsManager = TtsManager(this)
        val metaDatSessionManager = MetaDatSessionManager()
        val visualDetector = CompositeDetector(MediaPipeLiteRtDetector(), ColorSegmentationDetector())
        procedureLoader = ProcedureLoader(this)
        // Uso de IA real (critério "Uso de IA"): compara o frame ao vivo com
        // o desenho técnico de referência via modelo de visão hospedado na
        // Groq — ver LlamaStepEvaluator/LlamaVisionClient. GROQ_API_KEY vem
        // de local.properties (groq.api.key=...), nunca hardcoded.
        val llamaStepEvaluator = LlamaStepEvaluator(this)
        batteryMonitor = BatteryMonitor(this)
        val voiceInputManager = VoiceInputManager(
            context = this,
            onCommand = { command -> onVoiceCommand(command) },
            onStatus = { message ->
                runOnUiThread {
                    statusView.text = message
                    ttsManager.speak(message)
                }
            }
        )
        val linkStateManager = LinkStateManager(onReconnected = { sessionController.onReconnected() })

        sessionController = SessionController(
            metaDatSessionManager = metaDatSessionManager,
            qualityGate = QualityGate(),
            visualDetector = visualDetector,
            proceduralEngine = proceduralEngine,
            voiceInputManager = voiceInputManager,
            ttsManager = ttsManager,
            procedureLoader = procedureLoader,
            linkStateManager = linkStateManager,
            llamaStepEvaluator = llamaStepEvaluator
        )

        setContentView(buildLayout())
        requestSessionPermissions()
        startBatteryLogging()
    }

    private fun buildLayout(): ScrollView {
        val ctx = this
        val (scroll, column) = buildScrollableScreen(ctx)

        column.addView(buildTopBar(ctx, getString(R.string.hardware_session_title), onBack = { finish() }))

        val (statusCard, statusInner) = buildCard(ctx)
        statusView = buildBodyText(ctx).apply { textSize = 17f }
        statusInner.addView(statusView)
        batteryView = buildCaptionText(ctx)
        setLinearMargins(ctx, batteryView, top = 10)
        statusInner.addView(batteryView)
        column.addView(statusCard)

        column.addView(buildSectionLabel(ctx, getString(R.string.hardware_voice_fallback_label)))
        val (voiceCard, voiceInner) = buildCard(ctx)
        voiceInner.addView(commandRow(ctx))
        column.addView(voiceCard)

        column.addView(buildSectionLabel(ctx, getString(R.string.hardware_demo_label)))
        val (demoCard, demoInner) = buildCard(ctx)
        val demoHint = buildCaptionText(ctx, getString(R.string.hardware_demo_hint))
        setLinearMargins(ctx, demoHint, bottom = 12)
        demoInner.addView(demoHint)
        demoInner.addView(demoRow(ctx))
        column.addView(demoCard)

        return scroll
    }

    private fun commandRow(ctx: HardwareSessionActivity): LinearLayout {
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(buildPrimaryButton(ctx, getString(R.string.verify_fallback)) {
            onVoiceCommand(VoiceCommand.VERIFY)
        }.apply { layoutParams = rowButtonParams(ctx) })
        row.addView(buildSecondaryButton(ctx, getString(R.string.help_fallback)) {
            onVoiceCommand(VoiceCommand.HELP)
        }.apply { layoutParams = rowButtonParams(ctx) })
        row.addView(buildSecondaryButton(ctx, getString(R.string.repeat_fallback)) {
            onVoiceCommand(VoiceCommand.REPEAT)
        }.apply { layoutParams = rowButtonParams(ctx, marginEnd = 0) })
        return row
    }

    private fun demoRow(ctx: HardwareSessionActivity): LinearLayout {
        val column = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val enableButton = buildSecondaryButton(ctx, getString(R.string.hardware_demo_enable)) {
            runCatching { procedureLoader.load(PROCEDURE_ID) }
                .onSuccess { loadedProcedure ->
                    sessionController.enableDemoMode(loadedProcedure)
                    refreshStatus()
                }
        }
        enableButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        setLinearMargins(ctx, enableButton, bottom = 10)
        column.addView(enableButton)

        val forceRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        forceRow.addView(buildSuccessButton(ctx, getString(R.string.hardware_demo_force_correct)) {
            forceDemoResult(StepResult.CORRECT)
        }.apply { layoutParams = rowButtonParams(ctx) })
        forceRow.addView(buildWarningButton(ctx, getString(R.string.hardware_demo_force_error)) {
            forceDemoResult(StepResult.KNOWN_ERROR)
        }.apply { layoutParams = rowButtonParams(ctx) })
        forceRow.addView(buildSecondaryButton(ctx, getString(R.string.hardware_demo_force_inconclusive)) {
            forceDemoResult(StepResult.INCONCLUSIVE)
        }.apply { layoutParams = rowButtonParams(ctx, marginEnd = 0) })
        column.addView(forceRow)

        return column
    }

    private fun forceDemoResult(result: StepResult) {
        sessionController.forceDemoResult(result)
        refreshStatus()
    }

    private fun onVoiceCommand(command: VoiceCommand) {
        sessionController.onCommand(command)
        refreshStatus()
    }

    private fun refreshStatus() {
        statusView.text = getString(
            R.string.hardware_status_format,
            proceduralEngine.currentStepIndex() + 1,
            proceduralEngine.currentLevel().name,
            proceduralEngine.currentInstruction(repeat = true)
        )
    }

    // Amostra a cada 10 min (critério "Eficiência de bateria", CP2 — ver
    // BatteryMonitor). Óculos não têm leitura automática ainda (SDK Meta
    // DAT não expõe isso nesta integração); mostrar só o celular aqui e
    // deixar claro que o valor dos óculos é lido manualmente na demo.
    private fun startBatteryLogging() {
        batteryJob = batteryMonitor.startPeriodicLogging(lifecycleScope) { phonePercent ->
            runOnUiThread {
                batteryView.text = getString(
                    R.string.hardware_battery_format,
                    phonePercent?.toString() ?: getString(R.string.hardware_battery_unknown)
                )
            }
        }
    }

    private fun requestSessionPermissions() {
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required += Manifest.permission.BLUETOOTH_CONNECT
        }
        requestPermissions.launch(required.toTypedArray())
    }

    override fun onDestroy() {
        batteryJob?.cancel()
        sessionController.endSession()
        super.onDestroy()
    }

    private companion object {
        // Único procedimento com JSON real neste checkout (ver
        // assets/procedures/montagem_engrenagens.json).
        const val PROCEDURE_ID = "montagem_engrenagens"
    }
}
