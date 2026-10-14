package com.stepwise.software.android

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.stepwise.core.*
import com.stepwise.glasses.GlassesFrameSource
import com.stepwise.glasses.MetaDatSessionManager
import com.stepwise.software.*
import com.stepwise.software.providers.GeminiLiveProvider
import kotlinx.coroutines.*

/** Development companion for the CAD tutor. Does not depend on physical assembly code. */
class CadTutorActivity : AppCompatActivity() {
    private var drawing: ImageFrame? = null
    private var progress = CadProgress()
    private var session: SoftwareTutorSession? = null
    private var connecting = false
    private var picking = false
    private var loadingImage = false
    private var connectionJob: Job? = null
    private var microphoneJob: Job? = null
    private var verificationTimeout: Job? = null
    private var firstReplyTimeout: Job? = null
    private var playback: PcmPlayback? = null
    private val glasses = MetaDatSessionManager()
    private lateinit var token: EditText
    private lateinit var model: EditText
    private lateinit var objective: EditText
    private lateinit var question: EditText
    private lateinit var status: TextView
    private lateinit var planText: TextView
    private lateinit var transcript: TextView
    private lateinit var drawingStatus: TextView
    private lateinit var connectButton: MaterialButton
    private lateinit var drawingButton: MaterialButton
    private lateinit var acceptButton: MaterialButton
    private lateinit var verifyButton: MaterialButton
    private lateinit var photoButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var sendButton: MaterialButton
    private lateinit var micButton: MaterialButton
    private lateinit var interruptButton: MaterialButton

    private val drawingPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        picking = false
        uri?.let { loadImage(it, true) }
    }
    private val evidencePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        picking = false
        uri?.let { loadImage(it, false) }
    }
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && session != null) toggleMicrophone()
        else status.text = "Microfone não autorizado. Você pode escrever a pergunta."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        fun label(value: String) = TextView(this).apply {
            text = value; textSize = 17f; content.addView(this)
        }
        fun field(hintText: String, value: String = "") = EditText(this).apply {
            hint = hintText; setText(value); content.addView(this)
        }
        fun button(label: String, action: () -> Unit) = MaterialButton(this).apply {
            text = label; setOnClickListener { action() }; content.addView(this)
        }
        button("Voltar aos modos") { finish() }
        label("Software · Tutor CAD").textSize = 25f
        label("Nesta versão, desenho, imagens de verificação e perguntas são enviados ao Gemini. " +
            "A conversa pode ter resposta por áudio. Use imagens que você deseja compartilhar com o serviço.")
        drawingStatus = label("1. Selecione uma imagem do desenho técnico (JPEG, PNG ou WebP).")
        drawingButton = button("Selecionar desenho") { picking = true; drawingPicker.launch("image/*") }
        objective = field("Objetivo", "Orientar a construção desta peça no Fusion 360")
        model = field("Modelo", "gemini-3.8-live")
        label("2. Cole um token temporário novo. Ele só permite uma conexão e deve ser usado em até 10 minutos após a geração.")
        token = field("Token temporário Gemini (auth_tokens/…)")
        token.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        token.isSaveEnabled = false
        token.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        connectButton = button("Conectar e enviar desenho") { connect() }
        button("Encerrar sessão") { disconnect(); status.text = "Sessão encerrada." }
        status = TextView(this).apply {
            text = "Para começar, selecione o desenho e informe um token temporário."
            textSize = 17f
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        planText = label("O tutor proporá um plano para sua revisão.")
        acceptButton = button("Aceitar plano") {
            progress.acceptPlan(); render()
            sendText("Aceitei o plano ${progress.plan!!.id}. Oriente somente a etapa ${progress.currentGoal!!.id}: ${progress.currentGoal!!.instruction}")
        }
        verifyButton = button("Verificar pelos óculos") { verify(null) }
        photoButton = button("Verificar com foto local (teste)") { picking = true; evidencePicker.launch("image/*") }
        nextButton = button("Confirmar e avançar") {
            progress.advance(); render()
            sendText(progress.currentGoal?.let { "Avancei no aplicativo. Oriente a etapa ${it.id}: ${it.instruction}" }
                ?: "Confirmei a conclusão de todas as etapas do plano.")
        }
        question = field("Pergunte ao tutor")
        sendButton = button("Enviar pergunta") {
            val text = question.text.toString().trim()
            if (text.isNotEmpty()) { question.text.clear(); sendText(text) }
        }
        micButton = button("Falar pelo celular") {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                toggleMicrophone()
            else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        interruptButton = button("Interromper resposta") {
            playback?.interrupt(); progress.cancelVerification(); verificationTimeout?.cancel(); render()
            action { session?.send(SoftwareTutorInput.Interrupt) }
        }
        transcript = label("")
        transcript.isSaveEnabled = false
        // Keep connection feedback visible even while the user scrolls to the conversation.
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(status)
            addView(ScrollView(this@CadTutorActivity).apply { addView(content) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        })
        render()
    }

    private fun connect() {
        val reference = drawing ?: run {
            status.text = "Selecione o desenho técnico antes de conectar."
            return
        }
        val credential = token.text.toString().trim()
        if (credential.isBlank() || objective.text.isBlank() || model.text.isBlank()) {
            status.text = "Preencha objetivo, modelo e token temporário."; return
        }
        if (!credential.startsWith("auth_tokens/") || credential.any { it.isWhitespace() }) {
            status.text = "Cole somente o token que começa com auth_tokens/. A chave da API deve ser usada no script do computador."
            return
        }
        progress = CadProgress()
        transcript.text = ""
        connecting = true; render()
        status.text = "Conectando ao tutor…"
        val task = CadTask(reference, objective.text.toString())
        val provider = GeminiLiveProvider(credential, model.text.toString().trim())
        token.text.clear()
        connectionJob = lifecycleScope.launch {
            try {
                session = SoftwareTutorSession.open(provider, task, GlassesFrameSource(glasses))
                playback = PcmPlayback { runOnUiThread {
                    status.text = "Falha na saída de áudio. Reconecte para tentar novamente."
                } }
                connecting = false; render()
                status.text = "Conectado. Aguarde o plano ou converse com o tutor."
                firstReplyTimeout = lifecycleScope.launch {
                    delay(20_000)
                    status.text = "Conectado, mas o Gemini ainda não respondeu ao desenho. Tente enviar uma pergunta; se continuar sem resposta, reconecte com um token novo."
                }
                session!!.events.collect { event ->
                    if (event is SoftwareTutorEvent.TextDelta || event is SoftwareTutorEvent.Audio ||
                        event is SoftwareTutorEvent.PlanProposed) {
                        if (firstReplyTimeout?.isActive == true) status.text = "Resposta recebida. Leia a conversa ou ouça o tutor."
                        firstReplyTimeout?.cancel()
                    }
                    when (event) {
                        is SoftwareTutorEvent.PlanProposed -> {
                            if (progress.propose(event.plan)) status.text = "Revise as etapas antes de aceitar o plano."
                        }
                        is SoftwareTutorEvent.Assessment -> if (progress.receive(event)) {
                            verificationTimeout?.cancel()
                            status.text = "${event.assessment.result}: ${event.assessment.feedback}"
                        }
                        is SoftwareTutorEvent.TextDelta -> appendTranscript(event.text)
                        is SoftwareTutorEvent.UserTranscript -> appendTranscript("\nVocê: ${event.text}\n")
                        is SoftwareTutorEvent.Audio -> if (!picking) playback?.enqueue(event.chunk)
                        is SoftwareTutorEvent.Failure -> status.text = event.message
                        SoftwareTutorEvent.Interrupted -> {
                            // This can acknowledge interruption of the previous turn by a NEW verification.
                            playback?.interrupt()
                        }
                        SoftwareTutorEvent.TurnCompleted -> appendTranscript("\n")
                    }
                    render()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: TutorConnectionException) {
                status.text = error.message
            } catch (_: Exception) {
                status.text = "Sessão indisponível. Confira conexão, modelo e validade do token temporário."
            } finally {
                withContext(NonCancellable) { microphoneJob?.cancelAndJoin(); session?.close() }
                microphoneJob = null; session = null; connecting = false
                playback?.close(); playback = null
                progress.cancelVerification(); verificationTimeout?.cancel()
                firstReplyTimeout?.cancel()
                render()
            }
        }
    }

    private fun verify(localImage: ImageFrame?) {
        val current = progress.currentGoal ?: return
        val id = progress.beginVerification()
        render(); status.text = "Verificando ${current.instruction}"
        action {
            try {
                val active = checkNotNull(session)
                if (localImage == null) {
                    if (!active.verify(current.id, id)) {
                        progress.cancelVerification()
                        status.text = "Sem imagem dos óculos. Confira se estão conectados e autorizados no Meta AI, " +
                            "ou use uma foto local para testar o tutor."
                        return@action
                    }
                } else active.send(SoftwareTutorInput.Verify(localImage, current.id, id))
                verificationTimeout = lifecycleScope.launch {
                    delay(30_000)
                    if (progress.pendingRequestId == id) {
                        progress.cancelVerification()
                        status.text = "Sem avaliação da etapa. Pergunte ao tutor ou verifique novamente."
                        render()
                    }
                }
            } catch (cancelled: CancellationException) {
                progress.cancelVerification(); throw cancelled
            } catch (_: Exception) {
                progress.cancelVerification(); status.text = "Não foi possível verificar. Tente novamente."
            } finally { render() }
        }
    }

    private fun sendText(text: String) {
        playback?.interrupt(); progress.cancelVerification(); verificationTimeout?.cancel(); render()
        appendTranscript("\nVocê: $text\nTutor: ")
        action { session?.send(SoftwareTutorInput.Text(text)) }
    }

    private fun toggleMicrophone() {
        if (microphoneJob != null) {
            val capture = microphoneJob
            action {
                capture?.cancelAndJoin(); microphoneJob = null
                session?.send(SoftwareTutorInput.AudioEnd); render()
            }
            return
        }
        playback?.interrupt(); progress.cancelVerification(); verificationTimeout?.cancel()
        microphoneJob = lifecycleScope.launch {
            try {
                val active = checkNotNull(session)
                PhoneMicrophone.capture { active.send(SoftwareTutorInput.Audio(it)) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { status.text = "Não foi possível capturar o microfone." }
            finally { microphoneJob = null; render() }
        }
        render()
    }

    private fun loadImage(uri: Uri, reference: Boolean) = action {
        loadingImage = true
        render()
        try {
            status.text = "Abrindo imagem…"
            val image = withContext(Dispatchers.IO) {
                ImageImport.readAsJpeg(contentResolver, uri,
                    if (reference) ImageSource.IMPORTED_DRAWING else ImageSource.TEST_FIXTURE)
            }
            if (reference) {
                if (session != null || connecting) return@action
                drawing = image; drawingStatus.text = "Desenho carregado. Será enviado ao conectar."; render()
                status.text = "Pronto para conectar."
            } else if (session != null && progress.currentGoal != null && progress.pendingRequestId == null) verify(image)
        } finally {
            loadingImage = false
            render()
        }
    }

    private fun action(block: suspend () -> Unit) { lifecycleScope.launch {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { status.text = "Não foi possível concluir. Confira a imagem ou a conexão e tente novamente." }
    } }

    private fun appendTranscript(text: String) { transcript.text = (transcript.text.toString() + text).takeLast(12_000) }

    private fun render() {
        val active = session != null
        connectButton.isEnabled = !active && !connecting && !loadingImage
        connectButton.text = if (connecting) "Conectando…" else "Conectar e enviar desenho"
        drawingButton.isEnabled = !active && !connecting && !loadingImage
        token.isEnabled = !active && !connecting; model.isEnabled = token.isEnabled; objective.isEnabled = token.isEnabled
        acceptButton.isEnabled = active && progress.proposedPlan != null && progress.plan == null
        val canVerify = active && !loadingImage && progress.currentGoal != null && progress.pendingRequestId == null && microphoneJob == null
        verifyButton.isEnabled = canVerify; photoButton.isEnabled = canVerify
        nextButton.isEnabled = active && !loadingImage && progress.canAdvance
        sendButton.isEnabled = active && microphoneJob == null
        micButton.isEnabled = active
        micButton.text = if (microphoneJob == null) "Falar pelo celular" else "Terminar fala"
        interruptButton.isEnabled = active
        val plan = progress.plan ?: progress.proposedPlan
        planText.text = when {
            progress.completed -> "Plano concluído."
            plan == null -> "O tutor proporá um plano para sua revisão."
            else -> plan.goals.mapIndexed { i, goal ->
                val marker = if (progress.plan != null && i == progress.index) "→ " else ""
                "$marker${i + 1}. ${goal.instruction}"
            }.joinToString("\n")
        }
    }

    private fun disconnect() {
        microphoneJob?.cancel(); playback?.interrupt(); verificationTimeout?.cancel()
        firstReplyTimeout?.cancel()
        connectionJob?.cancel(); glasses.stopStream()
        token.text.clear()
    }
    override fun onStop() {
        super.onStop()
        playback?.interrupt()
        if (!picking) disconnect()
        else {
            val capture = microphoneJob
            action {
                capture?.cancelAndJoin()
                if (capture != null) session?.send(SoftwareTutorInput.AudioEnd)
            }
        }
    }
    override fun onDestroy() { disconnect(); playback?.close(); super.onDestroy() }
}
