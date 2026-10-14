package com.stepwise.session

import android.util.Log
import com.stepwise.ai.LlamaStepEvaluator
import com.stepwise.audio.TtsManager
import com.stepwise.audio.VoiceInputManager
import com.stepwise.data.db.SessionStateDao
import com.stepwise.data.db.SessionStateEntity
import com.stepwise.engine.AssistanceLevel
import com.stepwise.engine.ProceduralEngine
import com.stepwise.engine.StepResult
import com.stepwise.glasses.MetaDatSessionManager
import com.stepwise.procedure.Procedure
import com.stepwise.procedure.ProcedureLoader
import com.stepwise.procedure.ProcedureStep
import com.stepwise.vision.DetectionResult
import com.stepwise.vision.VisualDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controlador de sessão (CTRL no diagrama de arquitetura).
 *
 * Orquestra:
 *  - VOICE/UI -> comandos (VERIFY, HELP, REPEAT)
 *  - CTRL -> DAT (VERIFY: uma foto dos óculos, câmera fechada logo depois)
 *  - CTRL -> ENG (decisão local determinística; HELP/REPEAT sem nova captura)
 *  - CTRL -> PROC (carrega procedure_id ativo) e DB (checkpoints para retomar)
 *  - REC -> CTRL (reconexão / restauração de estado)
 *
 * Estado e callbacks vivem na thread principal: comandos de voz chegam de
 * outra thread e são repassados para cá antes de tocar no motor ou na UI.
 */
class SessionController(
    private val metaDatSessionManager: MetaDatSessionManager,
    private val visualDetector: VisualDetector,
    private val proceduralEngine: ProceduralEngine,
    private val voiceInputManager: VoiceInputManager,
    private val ttsManager: TtsManager,
    private val procedureLoader: ProcedureLoader,
    /**
     * Avaliador remoto opcional (Groq). Só recebe o frame quando o motor local
     * se abstém (INCONCLUSIVE); null mantém a verificação 100% no aparelho.
     */
    private val cloudEvaluator: LlamaStepEvaluator? = null,
    /** Checkpoints (Room) para retomar a sessão e para a tela de privacidade. */
    private val sessionStateDao: SessionStateDao? = null,
    /** Recebe o texto falado sempre que etapa, nível ou resultado mudam (thread principal). */
    private val onStateChanged: (String) -> Unit = {},
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var verifyJob: Job? = null
    private var startJob: Job? = null
    private var helpRequestedOnStep = false

    /**
     * Carrega o procedimento ativo, retoma o último checkpoint salvo e abre a
     * rota de áudio (HFP) para toda a sessão — ver VoiceInputManager.startSession().
     */
    fun startSession(procedureId: String) {
        val procedure = procedureLoader.load(procedureId)
        proceduralEngine.load(procedure)
        helpRequestedOnStep = false
        voiceInputManager.startSession()
        startJob = scope.launch {
            val checkpoint = sessionStateDao?.let { dao ->
                try {
                    dao.latestCheckpoint(procedureId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Não foi possível ler o checkpoint; começando do início.", e)
                    null
                }
            }
            if (checkpoint != null) restore(procedure, checkpoint)
            speak(proceduralEngine.currentInstruction())
        }
    }

    fun endSession() {
        startJob?.cancel()
        voiceInputManager.stopSession()
        val capturing = verifyJob?.takeIf { it.isActive }
        if (capturing != null) {
            // A captura roda em outra thread: o finally dela fecha a câmera depois que
            // ela parar. Fechar daqui competiria com um addCamera ainda em curso.
            capturing.cancel()
        } else {
            metaDatSessionManager.stopStream()
        }
    }

    /** Pode ser chamado de qualquer thread (o KeywordSpotter entrega comandos em background). */
    fun onCommand(command: VoiceCommand) {
        scope.launch {
            when (command) {
                VoiceCommand.VERIFY -> handleVerify()
                VoiceCommand.HELP -> handleHelp()
                VoiceCommand.REPEAT -> handleRepeat()
            }
        }
    }

    private fun handleVerify() {
        if (verifyJob?.isActive == true) return // uma verificação por vez.
        val step = proceduralEngine.currentStep() ?: return speak(proceduralEngine.currentInstruction())
        ttsManager.speak("Verificando") // resposta imediata: evita silêncio num dispositivo sem tela.

        verifyJob = scope.launch {
            // O frame vive só dentro desta coroutine: nunca vai para um campo,
            // para o disco ou para o Room (ver docs/lgpd-privacidade.md).
            val frame = try {
                withContext(Dispatchers.Default) { metaDatSessionManager.captureBestFrameFromStream() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Falha na captura pelos óculos; tratando como INCONCLUSIVE.", e)
                null
            } finally {
                // Câmera fechada já aqui, antes do resultado: bateria e privacidade.
                metaDatSessionManager.stopStream()
            }
            if (frame == null) {
                return@launch finish(step, StepResult.INCONCLUSIVE,
                    "Não consegui uma imagem boa dos óculos. Aproxime a montagem e diga verificar de novo.")
            }

            // Plano A: detector local + regras determinísticas do procedimento.
            val detection = try {
                visualDetector.detect(frame)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Detector local indisponível.", e)
                DetectionResult(emptyList())
            }
            val local = proceduralEngine.evaluateDecision(detection, imageUsable = true)
            Log.i(TAG, "Local: ${local.result} ${local.reason} classe=${local.observedClass} conf=${local.confidence}")
            val evaluator = cloudEvaluator
            if (local.result != StepResult.INCONCLUSIVE || evaluator == null) {
                return@launch finish(step, local.result, local.message)
            }

            // Plano B: o motor se absteve e há avaliador remoto configurado.
            val remote = withContext(Dispatchers.Default) { evaluator.evaluate(frame, step) }
            Log.i(TAG, "IA (${remote.modelUsed}): ${remote.result} confiança=${remote.confidence} latência=${remote.latencyMs}ms")
            finish(step, remote.result, remote.feedback)
        }
    }

    private fun handleHelp() {
        helpRequestedOnStep = true
        speak(proceduralEngine.currentInstruction(helpRequested = true))
    }

    private fun handleRepeat() {
        speak(proceduralEngine.currentInstruction(repeat = true))
    }

    /** Ignora o resultado se a etapa mudou durante a verificação (ex.: modo demonstração). */
    private fun finish(evaluatedStep: ProcedureStep, result: StepResult, feedback: String) {
        if (proceduralEngine.currentStep() != evaluatedStep) return
        apply(result, feedback, persist = true)
    }

    private fun apply(result: StepResult, feedback: String, persist: Boolean) {
        val evaluatedStep = proceduralEngine.currentStep()
        proceduralEngine.applyResult(result)
        if (persist && evaluatedStep != null) record(evaluatedStep, result)
        if (result == StepResult.CORRECT) helpRequestedOnStep = false
        speak(if (result == StepResult.CORRECT) "$feedback ${proceduralEngine.currentInstruction()}" else feedback)
    }

    private fun record(step: ProcedureStep, result: StepResult) {
        val dao = sessionStateDao ?: return
        val entity = SessionStateEntity(
            procedureId = proceduralEngine.procedureId() ?: return,
            stepId = step.stepId,
            assistanceLevel = proceduralEngine.currentLevel().name,
            lastResult = result.name,
            helpRequested = helpRequestedOnStep,
            timestampMillis = System.currentTimeMillis(),
        )
        scope.launch {
            try {
                dao.insert(entity)
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível salvar o checkpoint.", e)
            }
        }
    }

    /** Retoma a partir do último registro: após CORRECT, a etapa seguinte; senão, a mesma. */
    private fun restore(procedure: Procedure, checkpoint: SessionStateEntity) {
        val index = procedure.steps.indexOfFirst { it.stepId == checkpoint.stepId }
        if (index < 0) return
        val next = if (checkpoint.lastResult == StepResult.CORRECT.name) index + 1 else index
        val level = AssistanceLevel.entries.firstOrNull { it.name == checkpoint.assistanceLevel } ?: AssistanceLevel.L4
        // Procedimento já concluído: recomeça, mas mantém o nível de assistência conquistado.
        proceduralEngine.load(procedure, resumeStepIndex = if (next >= procedure.steps.size) 0 else next, resumeLevel = level)
        helpRequestedOnStep = false
    }

    private fun speak(text: String) {
        ttsManager.speak(text)
        onStateChanged(text)
    }

    /** Chamado pelo LinkStateManager após reconexão bem-sucedida. */
    fun onReconnected() {
        // TODO: revalidar estado (REC -> ENG: "restaurar + revalidar") antes de permitir novo avanço.
    }

    // ---------------------------------------------------------------------
    // MODO DEMONSTRAÇÃO (pitch)
    // ---------------------------------------------------------------------
    // Motivo (feedback da mentoria, 15/09): o fading exige duas execuções
    // corretas seguidas para reduzir um nível; num pitch de poucos minutos
    // isso não aparece do zero. O modo demo carrega um histórico
    // pré-configurado (aprendiz que já tem 1 execução correta) para que a
    // *próxima* verificação real (ou forçada) já mostre a transição de
    // nível — e permite forçar KNOWN_ERROR / INCONCLUSIVE roteirizados
    // para a banca ver os três desfechos em poucos minutos. Resultados
    // forçados não são gravados no histórico do aprendiz.
    // ---------------------------------------------------------------------

    /**
     * Prepara a sessão já "quase" trocando de nível: uma execução correta
     * de distância da transição (ex.: L3 -> L2 na próxima etapa correta).
     */
    fun enableDemoMode(
        procedure: Procedure,
        demoStepIndex: Int = 0,
        demoLevel: AssistanceLevel = AssistanceLevel.L3
    ) {
        verifyJob?.cancel()
        proceduralEngine.load(
            procedure = procedure,
            resumeStepIndex = demoStepIndex,
            resumeLevel = demoLevel,
            resumeStreak = 1 // falta só mais 1 acerto para cair de nível
        )
        helpRequestedOnStep = false
        speak(proceduralEngine.currentInstruction())
    }

    /**
     * Força um resultado sem depender da detecção visual real — usado só
     * para roteirizar KNOWN_ERROR/INCONCLUSIVE no pitch quando o tempo de
     * demonstração é curto. Nunca usar fora do modo demonstração.
     */
    fun forceDemoResult(result: StepResult) {
        verifyJob?.cancel()
        val step = proceduralEngine.currentStep() ?: return speak(proceduralEngine.currentInstruction())
        val feedback = when (result) {
            StepResult.CORRECT -> "Etapa conferida. Pode continuar."
            StepResult.KNOWN_ERROR -> step.knownErrors.firstOrNull()?.message
                ?: "Erro conhecido detectado. Corrija a montagem antes de avançar."
            StepResult.INCONCLUSIVE -> "Ainda não tenho evidência suficiente. Reposicione a montagem e verifique novamente."
        }
        apply(result, feedback, persist = false)
    }

    private companion object {
        const val TAG = "SessionController"
    }
}
