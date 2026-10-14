package com.stepwise.ui

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stepwise.R
import com.stepwise.audio.TtsManager
import com.stepwise.audio.VoiceInputManager
import com.stepwise.core.FrameSource
import com.stepwise.core.ImageFrame
import com.stepwise.core.ImageSource
import com.stepwise.core.TutoringPlan
import com.stepwise.engine.StepResult
import com.stepwise.session.VoiceCommand
import com.stepwise.software.CadTask
import com.stepwise.software.SoftwareTutorEvent
import com.stepwise.software.SoftwareTutorInput
import com.stepwise.software.SoftwareTutorSession
import com.stepwise.software.android.ImageImport
import com.stepwise.software.android.providers.LlamaCadTutorProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sessão do modo software (tutor de CAD) — recebe um desenho técnico
 * importado pelo app (não pela câmera dos óculos: o desenho é um
 * documento/imagem que o aluno já tem, ver com.stepwise.software.CadTask),
 * pede para a IA (modelo de visão via Groq — ver LlamaCadTutorProvider)
 * gerar um passo a passo de modelagem no Tinkercad, e narra cada passo
 * por voz — como um tutor explicando ao aluno — usando a mesma
 * infraestrutura de voz/TTS do modo hardware (VoiceInputManager/
 * TtsManager, grammar "verificar"/"ajuda"/"repetir": aqui "verificar"
 * avança para o próximo passo).
 *
 * Fora de escopo nesta versão: verificação automática do progresso do
 * aluno por imagem (o domínio em :mode-software já suporta isso via
 * SoftwareTutorInput.Verify/SoftwareTutorEvent.Assessment — só não está
 * ligado a uma captura de câmera aqui ainda).
 */
class SoftwareSessionActivity : AppCompatActivity() {

    private lateinit var ttsManager: TtsManager
    private lateinit var voiceInputManager: VoiceInputManager
    private lateinit var statusView: android.widget.TextView
    private lateinit var objectiveInput: EditText
    private lateinit var pickDrawingButton: com.google.android.material.button.MaterialButton
    private lateinit var generatePlanButton: com.google.android.material.button.MaterialButton
    private lateinit var drawingPreview: ImageView

    private var drawingJpeg: ByteArray? = null
    private var session: SoftwareTutorSession? = null
    private var currentPlan: TutoringPlan? = null
    private var currentGoalIndex: Int = 0

    private val pickDrawing = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadDrawing(it) }
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            voiceInputManager.startSession()
        } else {
            statusView.text = getString(R.string.software_permissions_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ttsManager = TtsManager(this)
        voiceInputManager = VoiceInputManager(
            context = this,
            onCommand = { command -> onVoiceCommand(command) }, // já chega na thread principal
            onStatus = { message ->
                runOnUiThread {
                    statusView.text = message
                    ttsManager.speak(message)
                }
            }
        )

        setContentView(buildLayout())
        requestSessionPermissions()
    }

    private fun buildLayout(): ScrollView {
        val ctx = this
        val (scroll, column) = buildScrollableScreen(ctx)

        column.addView(buildTopBar(ctx, getString(R.string.software_session_title), onBack = { finish() }))

        val (statusCard, statusInner) = buildCard(ctx)
        statusView = buildBodyText(ctx).apply { textSize = 16f }
        statusInner.addView(statusView)
        column.addView(statusCard)

        column.addView(buildSectionLabel(ctx, getString(R.string.software_drawing_section_label)))
        val (drawingCard, drawingInner) = buildCard(ctx)

        drawingPreview = ImageView(ctx).apply {
            visibility = ImageView.GONE
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = true
            contentDescription = getString(R.string.software_drawing_preview_desc)
        }
        val previewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 160)
        )
        previewParams.setMargins(0, 0, 0, dp(ctx, 14))
        drawingPreview.layoutParams = previewParams
        drawingInner.addView(drawingPreview)

        pickDrawingButton = buildSecondaryButton(ctx, getString(R.string.software_pick_drawing)) {
            pickDrawing.launch("image/*")
        }
        pickDrawingButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        drawingInner.addView(pickDrawingButton)

        objectiveInput = buildTextInput(ctx, getString(R.string.software_objective_hint))
        val objectiveParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        objectiveParams.setMargins(0, dp(ctx, 12), 0, dp(ctx, 12))
        objectiveInput.layoutParams = objectiveParams
        drawingInner.addView(objectiveInput)

        generatePlanButton = buildPrimaryButton(ctx, getString(R.string.software_generate_plan)) {
            onGeneratePlanClicked()
        }
        generatePlanButton.isEnabled = false
        generatePlanButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        drawingInner.addView(generatePlanButton)

        column.addView(drawingCard)

        column.addView(buildSectionLabel(ctx, getString(R.string.software_voice_fallback_label)))
        val (voiceCard, voiceInner) = buildCard(ctx)
        voiceInner.addView(commandRow(ctx))
        column.addView(voiceCard)

        return scroll
    }

    private fun commandRow(ctx: SoftwareSessionActivity): LinearLayout {
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(buildPrimaryButton(ctx, getString(R.string.software_verify_fallback)) {
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

    // -------------------------------------------------------------------
    // Desenho técnico (entrada pelo app, não pela câmera dos óculos)
    // -------------------------------------------------------------------

    private fun loadDrawing(uri: Uri) {
        pickDrawingButton.isEnabled = false
        lifecycleScope.launch {
            // Leitura limitada, redução e EXIF fora da thread principal (fotos de câmera podem ter 12+ MP).
            val loaded = try {
                withContext(Dispatchers.IO) {
                    val frame = ImageImport.readAsJpeg(contentResolver, uri, ImageSource.IMPORTED_DRAWING)
                    val preview = BitmapFactory.decodeByteArray(frame.bytes, 0, frame.bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = 2 })
                    frame.bytes to preview
                }
            } catch (e: CancellationException) {
                throw e // tela fechada: não tocar nas views
            } catch (e: Exception) {
                null
            }
            pickDrawingButton.isEnabled = true
            if (loaded == null) {
                statusView.text = getString(R.string.software_drawing_decode_error)
                return@launch
            }
            drawingJpeg = loaded.first
            drawingPreview.setImageBitmap(loaded.second)
            drawingPreview.visibility = ImageView.VISIBLE
            generatePlanButton.isEnabled = true
            statusView.text = getString(R.string.software_drawing_selected)
        }
    }

    private fun onGeneratePlanClicked() {
        val jpeg = drawingJpeg ?: return
        generatePlanButton.isEnabled = false
        pickDrawingButton.isEnabled = false
        statusView.text = getString(R.string.software_generating_plan)
        ttsManager.speak(getString(R.string.software_generating_plan))

        val objective = objectiveInput.text?.toString()?.trim()
            ?.takeUnless { it.isBlank() }
            ?: getString(R.string.software_default_objective)

        lifecycleScope.launch {
            // Um novo plano substitui a sessão anterior: fecha a conexão antiga e seu coletor.
            session?.let { previous ->
                session = null
                runCatching { previous.close() }
            }
            val drawing = ImageFrame(
                bytes = jpeg,
                mimeType = "image/jpeg",
                source = ImageSource.IMPORTED_DRAWING,
                capturedAtMillis = System.currentTimeMillis()
            )
            // Os prompts do LlamaCadTutorProvider ensinam o fluxo do Tinkercad (formas primitivas).
            val task = CadTask(drawing = drawing, objective = objective, applicationName = "Tinkercad")

            val opened = runCatching {
                SoftwareTutorSession.open(
                    provider = LlamaCadTutorProvider(),
                    task = task,
                    frames = FrameSource { null } // verificação por imagem ainda não ligada nesta tela
                )
            }.getOrNull()

            if (opened == null) {
                statusView.text = getString(R.string.software_generic_error)
                ttsManager.speak(getString(R.string.software_generic_error))
                generatePlanButton.isEnabled = true
                pickDrawingButton.isEnabled = true
                return@launch
            }
            session = opened
            launch { collectEvents(opened) }
        }
    }

    private suspend fun collectEvents(activeSession: SoftwareTutorSession) {
        activeSession.events.collect { event ->
            when (event) {
                is SoftwareTutorEvent.PlanProposed -> {
                    currentPlan = event.plan
                    currentGoalIndex = 0
                    pickDrawingButton.isEnabled = true
                    speakCurrentGoal()
                }
                is SoftwareTutorEvent.TextDelta -> {
                    ttsManager.speak(event.text)
                    statusView.text = event.text
                }
                is SoftwareTutorEvent.Assessment -> {
                    ttsManager.speak(event.assessment.feedback)
                    if (event.assessment.result == StepResult.CORRECT) advanceGoal()
                }
                is SoftwareTutorEvent.Failure -> {
                    ttsManager.speak(event.message)
                    statusView.text = event.message
                    generatePlanButton.isEnabled = true
                    pickDrawingButton.isEnabled = true
                }
                is SoftwareTutorEvent.Audio -> {
                    // Sem streaming de áudio nativo do provedor nesta implementação (ver KDoc do provider).
                }
                is SoftwareTutorEvent.UserTranscript -> { /* ASR é local (Vosk); provedor não transcreve */ }
                SoftwareTutorEvent.Interrupted -> { /* sem áudio em andamento para interromper */ }
                SoftwareTutorEvent.TurnCompleted -> { /* no-op */ }
            }
        }
    }

    // -------------------------------------------------------------------
    // Navegação do passo a passo (voz + fallback de UI)
    // -------------------------------------------------------------------

    private fun onVoiceCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.VERIFY -> advanceGoal()
            VoiceCommand.HELP -> askForHelp()
            VoiceCommand.REPEAT -> speakCurrentGoal()
        }
    }

    private fun speakCurrentGoal() {
        val plan = currentPlan
        if (plan == null) {
            ttsManager.speak(getString(R.string.software_no_plan_yet))
            return
        }
        val goal = plan.goals.getOrNull(currentGoalIndex)
        if (goal == null) {
            ttsManager.speak(getString(R.string.software_plan_finished))
            statusView.text = getString(R.string.software_plan_finished)
            return
        }
        ttsManager.speak(goal.instruction)
        statusView.text = getString(
            R.string.software_step_format,
            currentGoalIndex + 1,
            plan.goals.size,
            goal.instruction
        )
    }

    private fun advanceGoal() {
        val plan = currentPlan ?: run {
            ttsManager.speak(getString(R.string.software_no_plan_yet))
            return
        }
        if (currentGoalIndex < plan.goals.size - 1) {
            currentGoalIndex++
            speakCurrentGoal()
        } else {
            ttsManager.speak(getString(R.string.software_plan_finished))
            statusView.text = getString(R.string.software_plan_finished)
        }
    }

    private fun askForHelp() {
        val plan = currentPlan ?: run {
            ttsManager.speak(getString(R.string.software_no_plan_yet))
            return
        }
        val goal = plan.goals.getOrNull(currentGoalIndex) ?: return
        val activeSession = session ?: return
        ttsManager.speak(getString(R.string.software_thinking))
        lifecycleScope.launch {
            runCatching {
                activeSession.send(
                    SoftwareTutorInput.Text("Pode explicar melhor este passo: \"${goal.instruction}\"?")
                )
            }
        }
    }

    private fun requestSessionPermissions() {
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required += Manifest.permission.BLUETOOTH_CONNECT
        }
        requestPermissions.launch(required.toTypedArray())
    }

    override fun onDestroy() {
        voiceInputManager.stopSession()
        ttsManager.shutdown()
        session?.let { activeSession ->
            CoroutineScope(Dispatchers.IO).launch { runCatching { activeSession.close() } }
        }
        super.onDestroy()
    }
}
