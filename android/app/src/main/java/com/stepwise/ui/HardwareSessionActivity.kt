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
import com.stepwise.data.db.AppDatabase
import com.stepwise.engine.ProceduralEngine
import com.stepwise.engine.StepResult
import com.stepwise.glasses.MetaDatSessionManager
import com.stepwise.power.BatteryMonitor
import com.stepwise.procedure.ProcedureLoader
import com.stepwise.session.SessionController
import com.stepwise.session.VoiceCommand
import com.stepwise.vision.TfliteClassifierDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Sessão do modo hardware (montagem física) — constrói e liga CTRL/ENG/
 * PROC/DAT/VOICE/TTS/DB (ver diagrama de arquitetura) e expõe:
 *  - sessão com o procedimento real do kit (torneira com conexões),
 *    retomando o último checkpoint salvo;
 *  - verificação local (classificador TFLite + regras do procedimento) e,
 *    só se houver chave configurada, avaliação por IA na nuvem quando o
 *    motor local não tiver certeza — com aviso explícito na tela;
 *  - fallback de UI para VERIFY/HELP/REPEAT (o principal é por voz);
 *  - Modo Demonstração, para mostrar a transição de fading (feedback da
 *    mentoria, 15/09) e roteirizar KNOWN_ERROR/INCONCLUSIVE sem depender
 *    da câmera dos óculos.
 */
class HardwareSessionActivity : AppCompatActivity() {

    private lateinit var sessionController: SessionController
    private lateinit var proceduralEngine: ProceduralEngine
    private lateinit var procedureLoader: ProcedureLoader
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var ttsManager: TtsManager
    private lateinit var classifier: TfliteClassifierDetector
    private lateinit var statusView: TextView
    private lateinit var batteryView: TextView
    private var cloudEnabled = false
    private var batteryJob: Job? = null

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            runCatching { sessionController.startSession(PROCEDURE_ID) }
                .onFailure { statusView.text = getString(R.string.hardware_procedure_error) }
        } else {
            statusView.text = getString(R.string.hardware_permissions_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        proceduralEngine = ProceduralEngine()
        ttsManager = TtsManager(this)
        procedureLoader = ProcedureLoader(this)
        // Plano A local: classificador treinado com fotos reais do kit (ml/train.py).
        classifier = TfliteClassifierDetector(this)
        // Plano B na nuvem (Groq), opcional: sem chave (sempre no release) fica null.
        val cloudEvaluator = LlamaStepEvaluator(this, procedureId = PROCEDURE_ID).takeIf { it.isConfigured }
        cloudEnabled = cloudEvaluator != null
        batteryMonitor = BatteryMonitor(this)
        val voiceInputManager = VoiceInputManager(
            context = this,
            onCommand = { command -> sessionController.onCommand(command) },
            onStatus = { message ->
                runOnUiThread {
                    statusView.text = message
                    ttsManager.speak(message)
                }
            }
        )

        sessionController = SessionController(
            metaDatSessionManager = MetaDatSessionManager(),
            visualDetector = classifier,
            proceduralEngine = proceduralEngine,
            voiceInputManager = voiceInputManager,
            ttsManager = ttsManager,
            procedureLoader = procedureLoader,
            cloudEvaluator = cloudEvaluator,
            sessionStateDao = AppDatabase.getInstance(this).sessionStateDao(),
            onStateChanged = { spoken -> refreshStatus(spoken) },
        )

        setContentView(buildLayout())
        requestSessionPermissions()
        startBatteryLogging()
        // Sem modelo (asset ausente) a verificação segue sem o plano A; ver SessionController.
        lifecycleScope.launch { runCatching { classifier.warmUp() } }
    }

    private fun buildLayout(): ScrollView {
        val ctx = this
        val (scroll, column) = buildScrollableScreen(ctx)

        column.addView(buildTopBar(ctx, getString(R.string.hardware_session_title), onBack = { finish() }))

        val (statusCard, statusInner) = buildCard(ctx)
        statusView = buildBodyText(ctx).apply { textSize = 17f }
        statusInner.addView(statusView)
        val verification = buildCaptionText(
            ctx,
            getString(if (cloudEnabled) R.string.hardware_verification_cloud else R.string.hardware_verification_local)
        )
        setLinearMargins(ctx, verification, top = 10)
        statusInner.addView(verification)
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
            sessionController.onCommand(VoiceCommand.VERIFY)
        }.apply { layoutParams = rowButtonParams(ctx) })
        row.addView(buildSecondaryButton(ctx, getString(R.string.help_fallback)) {
            sessionController.onCommand(VoiceCommand.HELP)
        }.apply { layoutParams = rowButtonParams(ctx) })
        row.addView(buildSecondaryButton(ctx, getString(R.string.repeat_fallback)) {
            sessionController.onCommand(VoiceCommand.REPEAT)
        }.apply { layoutParams = rowButtonParams(ctx, marginEnd = 0) })
        return row
    }

    private fun demoRow(ctx: HardwareSessionActivity): LinearLayout {
        val column = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val enableButton = buildSecondaryButton(ctx, getString(R.string.hardware_demo_enable)) {
            runCatching { procedureLoader.load(PROCEDURE_ID) }
                .onSuccess { loadedProcedure -> sessionController.enableDemoMode(loadedProcedure) }
                .onFailure { statusView.text = getString(R.string.hardware_procedure_error) }
        }
        enableButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        setLinearMargins(ctx, enableButton, bottom = 10)
        column.addView(enableButton)

        val forceRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        forceRow.addView(buildSuccessButton(ctx, getString(R.string.hardware_demo_force_correct)) {
            sessionController.forceDemoResult(StepResult.CORRECT)
        }.apply { layoutParams = rowButtonParams(ctx) })
        forceRow.addView(buildWarningButton(ctx, getString(R.string.hardware_demo_force_error)) {
            sessionController.forceDemoResult(StepResult.KNOWN_ERROR)
        }.apply { layoutParams = rowButtonParams(ctx) })
        forceRow.addView(buildSecondaryButton(ctx, getString(R.string.hardware_demo_force_inconclusive)) {
            sessionController.forceDemoResult(StepResult.INCONCLUSIVE)
        }.apply { layoutParams = rowButtonParams(ctx, marginEnd = 0) })
        column.addView(forceRow)

        return column
    }

    /** Chamado pelo SessionController na thread principal, com o texto que acabou de ser falado. */
    private fun refreshStatus(spoken: String) {
        statusView.text = if (proceduralEngine.isCompleted()) {
            spoken
        } else {
            getString(
                R.string.hardware_status_format,
                proceduralEngine.currentStepNumber(),
                proceduralEngine.currentLevel().name,
                spoken
            )
        }
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
        ttsManager.shutdown()
        classifier.close()
        super.onDestroy()
    }

    private companion object {
        // Kit real da final (ver docs/final-2026-09/dataset-real-stepwise.md).
        const val PROCEDURE_ID = "montagem_torneira_stepwise_v1"
    }
}
