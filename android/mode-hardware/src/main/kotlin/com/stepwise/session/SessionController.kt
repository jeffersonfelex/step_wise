package com.stepwise.session

import android.util.Log
import com.stepwise.ai.LlamaStepEvaluator
import com.stepwise.audio.TtsManager
import com.stepwise.audio.VoiceInputManager
import com.stepwise.engine.AssistanceLevel
import com.stepwise.engine.ProceduralEngine
import com.stepwise.engine.StepResult
import com.stepwise.glasses.MetaDatSessionManager
import com.stepwise.glasses.QualityGate
import com.stepwise.procedure.Procedure
import com.stepwise.procedure.ProcedureLoader
import com.stepwise.resilience.LinkStateManager
import com.stepwise.vision.VisualDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Controlador de sessão (CTRL no diagrama de arquitetura).
 *
 * Orquestra:
 *  - VOICE/UI -> comandos (VERIFY, HELP, REPEAT)
 *  - CTRL -> DAT (VERIFY: melhor frame do stream aberto, ver MetaDatSessionManager)
 *  - CTRL -> ENG (HELP/REPEAT: solicita instrução sem nova captura)
 *  - CTRL -> PROC (carrega procedure_id ativo)
 *  - REC -> CTRL (reconexão / restauração de estado)
 */
class SessionController(
    private val metaDatSessionManager: MetaDatSessionManager,
    private val qualityGate: QualityGate,
    private val visualDetector: VisualDetector,
    private val proceduralEngine: ProceduralEngine,
    private val voiceInputManager: VoiceInputManager,
    private val ttsManager: TtsManager,
    private val procedureLoader: ProcedureLoader,
    private val linkStateManager: LinkStateManager,
    // IA real (critério "Uso de IA"): compara o frame ao vivo com o desenho
    // técnico de referência via Llama 4 (Groq) — ver LlamaStepEvaluator.
    // qualityGate/visualDetector seguem no construtor por compatibilidade
    // (pipeline local antigo, sem IA no caminho crítico), mas VERIFY agora
    // decide o resultado via llamaStepEvaluator.
    private val llamaStepEvaluator: LlamaStepEvaluator
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var verifyJob: Job? = null

    /**
     * Carrega o procedimento ativo (ex.: "montagem_engrenagens"), restaura
     * estado e abre a rota de áudio (HFP) para toda a sessão — ver
     * VoiceInputManager.startSession().
     *
     * TODO: restaurar checkpoint real via Room (SessionStateDao.latestCheckpoint)
     * em vez de sempre começar do zero (resumeStepIndex/resumeLevel default).
     */
    fun startSession(procedureId: String) {
        val procedure = procedureLoader.load(procedureId)
        proceduralEngine.load(procedure)
        voiceInputManager.startSession()
    }

    fun endSession() {
        verifyJob?.cancel()
        voiceInputManager.stopSession()
        metaDatSessionManager.stopStream()
    }

    fun onCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.VERIFY -> handleVerify()
            VoiceCommand.HELP -> handleHelp()
            VoiceCommand.REPEAT -> handleRepeat()
        }
    }

    private fun handleVerify() {
        ttsManager.speak("Verificando") // resposta imediata: evita silêncio num dispositivo sem tela.

        if (verifyJob?.isActive == true) return // uma verificação por vez.
        verifyJob = scope.launch {
            // Uso de IA real: o frame ao vivo é comparado com o desenho técnico
            // de referência por um modelo Llama 4 com visão (ver LlamaStepEvaluator/
            // LlamaVisionClient) — a entrada externa (imagem) muda a saída
            // (CORRECT/KNOWN_ERROR/INCONCLUSIVE + feedback falado).
            val evaluation = try {
                val frame = metaDatSessionManager.captureBestFrameFromStream()
                val step = proceduralEngine.currentStep()
                when {
                    frame == null -> null // quality gate não achou frame bom o bastante
                    step == null -> null // procedimento já concluído / nada carregado
                    else -> llamaStepEvaluator.evaluate(frame, step)
                }
            } catch (e: NotImplementedError) {
                // Integração real com o Meta DAT (stream de câmera) ainda pendente
                // neste checkout — ver MetaDatSessionManager. Tratar como
                // INCONCLUSIVE em vez de deixar a sessão travar/crashar.
                Log.w(TAG, "Captura Meta DAT ainda não integrada; tratando como INCONCLUSIVE.", e)
                null
            }

            if (evaluation == null) {
                applyAndSpeak(StepResult.INCONCLUSIVE)
            } else {
                Log.i(
                    TAG,
                    "IA (${evaluation.modelUsed}): ${evaluation.result} " +
                        "confiança=${evaluation.confidence} latência=${evaluation.latencyMs}ms"
                )
                proceduralEngine.applyResult(evaluation.result)
                ttsManager.speak(evaluation.feedback)
            }
        }
    }

    private fun applyAndSpeak(result: StepResult) {
        proceduralEngine.applyResult(result)
        ttsManager.speak(proceduralEngine.currentInstruction())
    }

    private fun handleHelp() {
        ttsManager.speak(proceduralEngine.currentInstruction(helpRequested = true))
    }

    private fun handleRepeat() {
        ttsManager.speak(proceduralEngine.currentInstruction(repeat = true))
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
    // para a banca ver os três desfechos em poucos minutos.
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
        proceduralEngine.load(
            procedure = procedure,
            resumeStepIndex = demoStepIndex,
            resumeLevel = demoLevel,
            resumeStreak = 1 // falta só mais 1 acerto para cair de nível
        )
    }

    /**
     * Força um resultado sem depender da detecção visual real — usado só
     * para roteirizar KNOWN_ERROR/INCONCLUSIVE no pitch quando o tempo de
     * demonstração é curto. Nunca usar fora do modo demonstração.
     */
    fun forceDemoResult(result: StepResult) {
        applyAndSpeak(result)
    }

    private companion object {
        const val TAG = "SessionController"
    }
}
